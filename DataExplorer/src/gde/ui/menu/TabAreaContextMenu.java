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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.config.Settings;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class TabAreaContextMenu {
	private final static String	$CLASS_NAME	= TabAreaContextMenu.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings	= Settings.getInstance();

	MenuItem										curveSelectionItem;
	MenuItem										displayGraphicsHeaderItem, displayGridLine10Item;
	MenuItem										displayGraphicsCommentItem, displayGraphicsCurveSurvey;
	MenuItem										separatorView;
	MenuItem										copyTabItem;
	MenuItem										copyPrintImageItem;
	MenuItem										separatorCopy;
	MenuItem										outherAreaColorItem;
	MenuItem										innerAreaColorItem;
	MenuItem										borderColorItem;
	MenuItem										dateTimeItem;
	MenuItem										partialTableItem;
	MenuItem										editTableItem;
	MenuItem										setDigitalFontItem;
	protected boolean						isCreated		= false;

	public enum TabMenuType {
		GRAPHICS, COMPARE, UTILITY, SIMPLE, TABLE, DIGITAL;
	};

	public enum TabMenuOnDemand {
		IS_CURSOR_IN_CANVAS
	};

	public void createMenu(Menu popupMenu, TabMenuType type) {
		popupMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				TabAreaContextMenu.log.log(Level.FINEST, "menuShown action " + e); //$NON-NLS-1$
				if (type == TabMenuType.GRAPHICS) {
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().getCurveSelectionMenuItem().getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().getGraphicsHeaderMenuItem().getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().getRecordCommentMenuItem().getSelection());
					TabAreaContextMenu.this.displayGridLine10Item.setSelection(TabAreaContextMenu.this.settings.isDraw10TicksPerRecord());
				}

				if (type == TabMenuType.TABLE && TabAreaContextMenu.this.editTableItem != null) {
					TabAreaContextMenu.this.editTableItem.setSelection(Settings.getInstance().isDataTableEditable());
				}
				// clear consumed menu type selector
				popupMenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString(), null);
			}

			@Override
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		if (!this.isCreated) {

			if (type == TabMenuType.GRAPHICS) {
				this.curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
				this.curveSelectionItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
				this.curveSelectionItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.curveSelectionItem.getSelection();
						TabAreaContextMenu.this.application.enableCurveSelector(selection);
						TabAreaContextMenu.this.application.getMenuBar().getCurveSelectionMenuItem().setSelection(selection);
					}
				});
				this.displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
				this.displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsHeaderItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().getGraphicsHeaderMenuItem().setSelection(selection);
						TabAreaContextMenu.this.application.enableGraphicsHeader(selection);
					}
				});
				this.displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsCommentItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
				if (!GDE.IS_OS_ARCH_ARM) this.displayGraphicsCommentItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0878));
				this.displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsCommentItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().getRecordCommentMenuItem().setSelection(selection);
						TabAreaContextMenu.this.application.enableRecordSetComment(selection);
						TabAreaContextMenu.this.application.updateGraphicsWindow();
					}
				});

				this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);
				
				this.displayGridLine10Item = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGridLine10Item.setText(Messages.getString(MessageIds.GDE_MSGT0968));
				if (!GDE.IS_OS_ARCH_ARM) this.displayGridLine10Item.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0969));
				this.displayGridLine10Item.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggle displayGridLine10Item action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGridLine10Item.getSelection();
						TabAreaContextMenu.this.settings.setDraw10TicksPerRecord(selection);
						if (TabAreaContextMenu.this.settings.isDraw10TicksPerRecord()) {
							TabAreaContextMenu.this.application.updateAllTabs(false);
						}
						else {
							if (TabAreaContextMenu.this.application.getActiveRecordSet() != null) 
								TabAreaContextMenu.this.application.getActiveChannel().applyTemplateScaleEndpoints(TabAreaContextMenu.this.application.getActiveRecordSet().getName());
						}
					}
				});

				this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);
			}

			this.copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			this.copyTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0026).substring(0, Messages.getString(MessageIds.GDE_MSGT0026).lastIndexOf('\t')));
			this.copyTabItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					TabAreaContextMenu.this.application.copyTabContentAsImage();
				}
			});

			if (type == TabMenuType.GRAPHICS || type == TabMenuType.COMPARE || type == TabMenuType.UTILITY) {
				this.copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
				this.copyPrintImageItem.setText(Messages.getString(MessageIds.GDE_MSGT0027).substring(0, Messages.getString(MessageIds.GDE_MSGT0027).lastIndexOf('\t')));
				this.copyPrintImageItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.copyGraphicsPrintImage();
					}
				});
			}

			if (type != TabMenuType.TABLE) {
				//
				//			{
				this.separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);
				this.outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.outherAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0462));
				this.outherAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setSurroundingBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
				this.innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.innerAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0463));
				this.innerAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setInnerAreaBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TabMenuType.GRAPHICS || type == TabMenuType.COMPARE || type == TabMenuType.UTILITY) {
				this.borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.borderColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0464));
				this.borderColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setBorderColor(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TabMenuType.TABLE) {
				this.dateTimeItem = new MenuItem(popupMenu, SWT.CHECK);
				this.dateTimeItem.setText(Messages.getString(MessageIds.GDE_MSGT0436));
				this.dateTimeItem.setSelection(Settings.getInstance().isTimeFormatAbsolute());
				this.dateTimeItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "dateTimeItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.setAbsoluteDateTime(TabAreaContextMenu.this.dateTimeItem.getSelection());
					}
				});
				this.editTableItem = new MenuItem(popupMenu, SWT.CHECK);
				this.editTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0731));
				this.editTableItem.setSelection(Settings.getInstance().isDataTableEditable());
				this.editTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "editTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setDataTableEditable(TabAreaContextMenu.this.editTableItem.getSelection());
					}
				});
				this.partialTableItem = new MenuItem(popupMenu, SWT.CHECK);
				this.partialTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
				this.partialTableItem.setSelection(Settings.getInstance().isPartialDataTable());
				this.partialTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "partialTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setPartialDataTable(TabAreaContextMenu.this.partialTableItem.getSelection());
						TabAreaContextMenu.this.application.updateAllTabs(true, false);
					}
				});
			}

			if (type == TabMenuType.DIGITAL) {
				this.setDigitalFontItem = new MenuItem(popupMenu, SWT.PUSH);
				this.setDigitalFontItem.setText(Messages.getString(MessageIds.GDE_MSGT0726));
				this.setDigitalFontItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "setDigitalFontItem action performed! " + e); //$NON-NLS-1$
						int selectedFontSize = TabAreaContextMenu.this.application.openFontSizeDialog();
						if (selectedFontSize != 0) TabAreaContextMenu.this.application.setTabFontSize(TabAreaContextMenu.this.application.getTabSelectionIndex(), selectedFontSize);
					}
				});
			}
			this.isCreated = true;
		}

	}

}
