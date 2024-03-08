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

import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;

import gde.GDE;
import gde.config.Settings;
import gde.data.AbstractRecord;
import gde.data.AbstractRecordSet;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.histo.recordings.TrailRecord;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.dialog.AxisEndValuesDialog;
import gde.ui.tab.GraphicsComposite.GraphicsMode;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.ColorUtils;
import gde.utils.TimeLine;

/**
 * Context menu class of the curve selection window acts as popup menu
 * @author Winfried Brügmann
 */
public class CurveSelectorContextMenu {
	final static Logger	log										= Logger.getLogger(CurveSelectorContextMenu.class.getName());

	Menu								menu;
	Menu								lineWidthMenu, lineTypeMenu, axisEndValuesMenu, axisNumberFormatMenu, axisPositionMenu, timeGridMenu, valueGridMenu, measurementMenu;
	MenuItem						recordName, lineVisible, lineColor, copyCurveCompare, cleanCurveCompare;
	MenuItem						lineWidth, lineWidthMenuItem1, lineWidthMenuItem2, lineWidthMenuItem3;
	MenuItem						lineType, lineTypeMenuItem1, lineTypeMenuItem2, lineTypeMenuItem3, smoothAtCurrentDropItem, smoothVoltageCurveItem;
	MenuItem						axisEndValues, axisEndAuto, axisEndRound, axisStarts0, axisEndManual;
	MenuItem						axisNumberFormat, axisNumberFormatAuto, axisNumberFormat0, axisNumberFormat1, axisNumberFormat2, axisNumberFormat3;
	MenuItem						axisPosition, axisPositionLeft, axisPositionRight;
	MenuItem						measurement, measurementRecordName, simpleMeasure, deltaMeasure, avgMedianMeasure;
	MenuItem						timeGridColor, timeGrid, timeGridOff, timeGridMain, timeGridMod60;
	MenuItem						valueGridRecordName, valueGridColor, valueGrid, valueGridOff, valueGridEveryTick, valueGridEverySecond;

	AbstractRecordSet		recordSet;
	final DataExplorer	application;
	final Settings			settings							= Settings.getInstance();
	AxisEndValuesDialog	axisEndValuesDialog;

	// states of initiated sub dialogs
	boolean							isActiveColorDialog		= false;
	boolean							isActiveEnValueDialog	= false;

	TableItem						selectedItem;
	AbstractRecord			actualRecord					= null;
	boolean							isRecordVisible				= false;
	boolean							isSmoothAtCurrentDrop	= false;
	boolean							isSmoothVoltageCurve	= false;
	String							recordNameKey					= null;
	String							recordNameMeasurement	= GDE.STRING_BLANK;
	boolean							isTypeCompare					= false;
	boolean							isTypeUtility					= false;
	boolean							isTypeHisto						= false;

	public CurveSelectorContextMenu() {
		super();
		this.application = DataExplorer.getInstance();
		this.axisEndValuesDialog = new AxisEndValuesDialog(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
	}

	public void createMenu(Menu popupmenu) {
		this.menu = popupmenu;
		try {
			popupmenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "popupmenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					CurveSelectorContextMenu.this.selectedItem = (TableItem) popupmenu.getData(DataExplorer.CURVE_SELECTION_ITEM);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.log.finer(() -> CurveSelectorContextMenu.this.selectedItem.toString());
						if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
							CurveSelectorContextMenu.this.recordNameKey = (String) popupmenu.getData(DataExplorer.RECORD_NAME);
							String recordNameUi = (String) popupmenu.getData(DataExplorer.NAME_REPLACEMENT) == null ? CurveSelectorContextMenu.this.recordNameKey : (String) popupmenu.getData(DataExplorer.NAME_REPLACEMENT);
							CurveSelectorContextMenu.log.fine(() -> "===>>" + CurveSelectorContextMenu.this.recordNameKey);
							CurveSelectorContextMenu.this.isTypeCompare = CurveSelectorContextMenu.this.application.isRecordSetVisible(GraphicsType.COMPARE);
							CurveSelectorContextMenu.this.isTypeUtility = CurveSelectorContextMenu.this.application.isRecordSetVisible(GraphicsType.UTIL);
							CurveSelectorContextMenu.this.isTypeHisto = CurveSelectorContextMenu.this.application.getHistoExplorer().map(h -> h.isHistoChartWindowVisible()).orElse(false);
							CurveSelectorContextMenu.this.recordSet = CurveSelectorContextMenu.this.application.getRecordSetOfVisibleTab();

							if (CurveSelectorContextMenu.this.recordSet != null) {
								setAllEnabled(true);

								if (CurveSelectorContextMenu.this.recordNameKey != null && CurveSelectorContextMenu.this.recordNameKey.length() > 1) {
									CurveSelectorContextMenu.this.actualRecord = CurveSelectorContextMenu.this.recordSet.get(CurveSelectorContextMenu.this.recordNameKey);
									if (CurveSelectorContextMenu.this.actualRecord != null) {
										CurveSelectorContextMenu.this.recordName.setText(">>>>  " + recordNameUi + "  <<<<"); //$NON-NLS-1$ //$NON-NLS-2$
										CurveSelectorContextMenu.this.lineVisible.setText(Messages.getString(MessageIds.GDE_MSGT0085));
										CurveSelectorContextMenu.this.isRecordVisible = CurveSelectorContextMenu.this.actualRecord.isVisible();
										CurveSelectorContextMenu.this.lineVisible.setSelection(CurveSelectorContextMenu.this.isRecordVisible);

										CurveSelectorContextMenu.this.isSmoothAtCurrentDrop = CurveSelectorContextMenu.this.actualRecord.getAbstractParent().isSmoothAtCurrentDrop();
										CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setSelection(CurveSelectorContextMenu.this.isSmoothAtCurrentDrop);
										if (CurveSelectorContextMenu.this.isTypeHisto) {
											CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setEnabled(false);
											CurveSelectorContextMenu.this.timeGrid.setEnabled(false);
											CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
										}
										CurveSelectorContextMenu.this.isSmoothVoltageCurve = CurveSelectorContextMenu.this.actualRecord.getAbstractParent().isSmoothVoltageCurve();
										CurveSelectorContextMenu.this.smoothVoltageCurveItem.setSelection(CurveSelectorContextMenu.this.isSmoothVoltageCurve);
										//enable voltage curve smoothing for special charge types only supported by Graupner Ultra... devices
										if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet.getDevice().getName().startsWith("Ultra")) {
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(true);
										}
										else {
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
										}

										// check measurement selections
										// deltaMeasure.setSelection(recordSet.isDeltaMeasurementMode(recordNameKey));
										// disable all menu items which makes only sense if record is visible
										if (!CurveSelectorContextMenu.this.isRecordVisible) {
											CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
										}
									}
									else {
										return; // actual record is null, record related operations not possible
									}
								}
								// check zoom mode
								if (!CurveSelectorContextMenu.this.isTypeHisto && ((RecordSet) CurveSelectorContextMenu.this.recordSet).isZoomMode()) {
									CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
									CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0083));
								}
								else {
									CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0084));
								}

								// compare window has fixed defined scale end values
								if (CurveSelectorContextMenu.this.isTypeCompare) {
									CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setEnabled(false);
									if (CurveSelectorContextMenu.this.smoothVoltageCurveItem != null) CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
									CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
									CurveSelectorContextMenu.this.axisPosition.setEnabled(false);
									CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
									CurveSelectorContextMenu.this.deltaMeasure.setEnabled(false);
									CurveSelectorContextMenu.this.avgMedianMeasure.setEnabled(false);
								}

								// utility window
								if (CurveSelectorContextMenu.this.isTypeUtility) {
									CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setEnabled(false);
									if (CurveSelectorContextMenu.this.smoothVoltageCurveItem != null) CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
									CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
									CurveSelectorContextMenu.this.measurement.setEnabled(false);
									CurveSelectorContextMenu.this.deltaMeasure.setEnabled(false);
									CurveSelectorContextMenu.this.avgMedianMeasure.setEnabled(false);
								}
								
								// history
								if (CurveSelectorContextMenu.this.isTypeHisto) {
									CurveSelectorContextMenu.this.avgMedianMeasure.setEnabled(false);
								}

								// disable clear, if nothing to clear
								if (!CurveSelectorContextMenu.this.application.isWithCompareSet()) {
									CurveSelectorContextMenu.this.cleanCurveCompare.setEnabled(false);
								}
							}
							else
								setAllEnabled(false);
						}
					}
					else {
						CurveSelectorContextMenu.this.recordName.setText(">>>>  " + Messages.getString(MessageIds.GDE_MSGT0408) + "  <<<<"); //$NON-NLS-1$ //$NON-NLS-2$
						setAllEnabled(false);
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "popupmenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
				}
			});
			popupmenu.addListener(SWT.FocusOut, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "widgetDisposed Action performed! " + e); //$NON-NLS-1$
					CurveSelectorContextMenu.this.menu.setData(DataExplorer.RECORD_NAME, null);
					CurveSelectorContextMenu.this.menu.setData(DataExplorer.CURVE_SELECTION_ITEM, null);
				}
			});
			this.recordName = new MenuItem(popupmenu, SWT.None);

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			this.lineVisible.setText(Messages.getString(MessageIds.GDE_MSGT0085));
			this.lineVisible.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "lineVisible selected evt=" + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.lineVisible.getSelection();
						CurveSelectorContextMenu.this.actualRecord.setVisible(checked);
						// CurveSelectorContextMenu.this.selectedItem.setChecked(checked);
						CurveSelectorContextMenu.this.recordSet.syncScaleOfSyncableRecords();
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.lineColor = new MenuItem(popupmenu, SWT.PUSH);
			this.lineColor.setText(Messages.getString(MessageIds.GDE_MSGT0086));
			this.lineColor.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event evt) {
					CurveSelectorContextMenu.log.finer(() -> "lineColor performed! " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.isActiveColorDialog = true;
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							Color color = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
							CurveSelectorContextMenu.this.selectedItem.setForeground(color);
							CurveSelectorContextMenu.this.actualRecord.setRGB(ColorUtils.toRGB(rgb.red, rgb.green, rgb.blue));
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						}
						CurveSelectorContextMenu.this.isActiveColorDialog = false;
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineWidth.setText(Messages.getString(MessageIds.GDE_MSGT0087));
			this.lineWidthMenu = new Menu(this.lineWidth);
			this.lineWidth.setMenu(this.lineWidthMenu);
			this.lineWidthMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "lineWidthMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int width = CurveSelectorContextMenu.this.actualRecord.getLineWidth();
						switch (width) {
						case 1:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(true);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						case 2:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(true);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						case 3:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(true);
							break;
						default:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "lineWidthMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.lineWidthMenuItem1 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem1.setImage(SWTResourceManager.getImage("gde/resource/LineWidth1.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0088));
			this.lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> Messages.getString(MessageIds.GDE_MSGT0089) + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(1);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidthMenuItem2 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem2.setImage(SWTResourceManager.getImage("gde/resource/LineWidth2.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0090));
			this.lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> Messages.getString(MessageIds.GDE_MSGT0091) + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(2);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidthMenuItem3 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem3.setImage(SWTResourceManager.getImage("gde/resource/LineWidth3.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem3.setText(Messages.getString(MessageIds.GDE_MSGT0092));
			this.lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> Messages.getString(MessageIds.GDE_MSGT0093) + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(3);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.lineType = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineType.setText(Messages.getString(MessageIds.GDE_MSGT0094));
			this.lineTypeMenu = new Menu(this.lineType);
			this.lineType.setMenu(this.lineTypeMenu);
			this.lineTypeMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "lineTypeMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int type = CurveSelectorContextMenu.this.actualRecord.getLineStyle();
						switch (type) {
						case SWT.LINE_SOLID:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(true);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DASH:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(true);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DOT:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(true);
							break;
						default:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "lineTypeMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.lineTypeMenuItem1 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem1.setImage(SWTResourceManager.getImage("gde/resource/LineType1.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0095));
			this.lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "lineTypeMenuItem1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_SOLID);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineTypeMenuItem2 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem2.setImage(SWTResourceManager.getImage("gde/resource/LineType2.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0096));
			this.lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "lineTypeMenuItem2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_DASH);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineTypeMenuItem3 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem3.setImage(SWTResourceManager.getImage("gde/resource/LineType3.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem3.setText(Messages.getString(MessageIds.GDE_MSGT0097));
			this.lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "lineTypeMenuItem3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_DOT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.smoothAtCurrentDropItem = new MenuItem(popupmenu, SWT.CHECK);
			this.smoothAtCurrentDropItem.setText(Messages.getString(MessageIds.GDE_MSGT0335));
			this.smoothAtCurrentDropItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "smoothAtCurrentDropItem selected evt=" + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.smoothAtCurrentDropItem.getSelection();
						((RecordSet) CurveSelectorContextMenu.this.recordSet).setSmoothAtCurrentDrop(checked);
						// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.smoothVoltageCurveItem = new MenuItem(popupmenu, SWT.CHECK);
			this.smoothVoltageCurveItem.setText(Messages.getString(MessageIds.GDE_MSGT0685));
			this.smoothVoltageCurveItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "smoothVoltageCurveItem selected evt=" + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.smoothVoltageCurveItem.getSelection();
						((RecordSet) CurveSelectorContextMenu.this.recordSet).setSmoothVoltageCurve(checked);
						// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0098));
			this.axisEndValuesMenu = new Menu(this.axisEndValues);
			this.axisEndValues.setMenu(this.axisEndValuesMenu);
			this.axisEndValuesMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isRounded = CurveSelectorContextMenu.this.actualRecord.isRoundOut();
						boolean isStart0 = CurveSelectorContextMenu.this.actualRecord.isStartpointZero();
						boolean isManual = CurveSelectorContextMenu.this.actualRecord.isStartEndDefined();
						boolean isAuto = !isRounded && !isManual;
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(isAuto);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(isRounded);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(isStart0);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(isManual);
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.axisEndAuto = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndAuto.setText(Messages.getString(MessageIds.GDE_MSGT0099));
			this.axisEndAuto.setSelection(true);
			this.axisEndAuto.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisEndAuto.SelectionListener = " + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setRoundOut(false);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndRound = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndRound.setText(Messages.getString(MessageIds.GDE_MSGT0101));
			this.axisEndRound.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisEndRound.SelectionListener = " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						if (CurveSelectorContextMenu.this.axisEndRound.getSelection()) { // true
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setRoundOut(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(true);
							CurveSelectorContextMenu.this.actualRecord.setRoundOut(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisStarts0 = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisStarts0.setText(Messages.getString(MessageIds.GDE_MSGT0103));
			this.axisStarts0.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisStarts0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						if (CurveSelectorContextMenu.this.axisStarts0.getSelection()) { // true
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndManual = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndManual.setText(Messages.getString(MessageIds.GDE_MSGT0104));
			this.axisEndManual.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisEndManual Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.isActiveEnValueDialog = true;
						CurveSelectorContextMenu.this.axisEndManual.setSelection(true);
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						AbstractRecord record = CurveSelectorContextMenu.this.actualRecord;
						record.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						record.setRoundOut(false);
						// ET 26.09.2017 do not call getMin... because it is not supported for TrailRecords (parent!)
						double[] oldMinMax = new double[] { record.getMinScaleValue(), record.getMaxScaleValue() };
						double[] newMinMax = CurveSelectorContextMenu.this.axisEndValuesDialog.open(oldMinMax);
						record.setStartEndDefined(true, newMinMax[0], newMinMax[1]);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.isActiveEnValueDialog = false;
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisNumberFormat.setText(Messages.getString(MessageIds.GDE_MSGT0105));
			this.axisNumberFormatMenu = new Menu(this.axisNumberFormat);
			this.axisNumberFormat.setMenu(this.axisNumberFormatMenu);
			this.axisNumberFormatMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormatMenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int format = CurveSelectorContextMenu.this.actualRecord.getNumberFormat();
						switch (format) {
						case -1:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 0:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 1:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						default:
						case 2:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 3:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(true);
							break;
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormatMenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
				}
			});

			this.axisNumberFormatAuto = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormatAuto.setText(Messages.getString(MessageIds.GDE_MSGT0099));
			this.axisNumberFormatAuto.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormatAuto " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(-1);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat0 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat0.setText(Messages.getString(MessageIds.GDE_MSGT0106));
			this.axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormat0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(0);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat1 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat1.setText(Messages.getString(MessageIds.GDE_MSGT0107));
			this.axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormat1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(1);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat2 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat2.setText(Messages.getString(MessageIds.GDE_MSGT0108));
			this.axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormat2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(2);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat3 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat3.setText(Messages.getString(MessageIds.GDE_MSGT0109));
			this.axisNumberFormat3.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisNumberFormat3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(3);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisPosition.setText(Messages.getString(MessageIds.GDE_MSGT0110));
			this.axisPositionMenu = new Menu(this.axisPosition);
			this.axisPosition.setMenu(this.axisPositionMenu);
			this.axisPositionMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisPositionMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isLeft = CurveSelectorContextMenu.this.actualRecord.isPositionLeft();
						if (isLeft) {
							CurveSelectorContextMenu.this.axisPositionLeft.setSelection(true);
							CurveSelectorContextMenu.this.axisPositionRight.setSelection(false);
						}
						else {
							CurveSelectorContextMenu.this.axisPositionLeft.setSelection(false);
							CurveSelectorContextMenu.this.axisPositionRight.setSelection(true);
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "axisPositionMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.axisPositionLeft = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionLeft.setText(Messages.getString(MessageIds.GDE_MSGT0111));
			this.axisPositionLeft.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisPositionLeft Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(true);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_SCALE_POSITION);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisPositionRight = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionRight.setText(Messages.getString(MessageIds.GDE_MSGT0112));
			this.axisPositionRight.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "axisPositionRight Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(false);
						CurveSelectorContextMenu.this.actualRecord.getAbstractParent().syncMasterSlaveRecords(CurveSelectorContextMenu.this.actualRecord, Record.TYPE_AXIS_SCALE_POSITION);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.timeGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.timeGrid.setText(Messages.getString(MessageIds.GDE_MSGT0113));
			this.timeGridMenu = new Menu(this.timeGrid);
			this.timeGrid.setMenu(this.timeGridMenu);
			this.timeGridMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int gridType = CurveSelectorContextMenu.this.isTypeHisto ? RecordSet.TIME_GRID_NONE : ((RecordSet) CurveSelectorContextMenu.this.recordSet).getTimeGridType();
						switch (gridType) {
						case RecordSet.TIME_GRID_MAIN:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(true);
							CurveSelectorContextMenu.this.timeGridMod60.setSelection(false);
							break;
						case RecordSet.TIME_GRID_MOD60:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(false);
							break;
						case RecordSet.TIME_GRID_NONE:
						default:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(true);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMod60.setSelection(false);
							break;
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.timeGridOff = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridOff.setText(Messages.getString(MessageIds.GDE_MSGT0114));
			this.timeGridOff.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridOff Action performed! " + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null) {
						((RecordSet) CurveSelectorContextMenu.this.recordSet).setTimeGridType(RecordSet.TIME_GRID_NONE);
						// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_NONE);
						}
					}
				}
			});
			this.timeGridMain = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMain.setText(Messages.getString(MessageIds.GDE_MSGT0115));
			this.timeGridMain.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridMain Action performed! " + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						((RecordSet) CurveSelectorContextMenu.this.recordSet).setTimeGridType(RecordSet.TIME_GRID_MAIN);
						// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MAIN);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridMod60 = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMod60.setText(Messages.getString(MessageIds.GDE_MSGT0116));
			this.timeGridMod60.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						((RecordSet) CurveSelectorContextMenu.this.recordSet).setTimeGridType(RecordSet.TIME_GRID_MOD60);
						// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MOD60);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridColor = new MenuItem(this.timeGridMenu, SWT.PUSH);
			this.timeGridColor.setText(Messages.getString(MessageIds.GDE_MSGT0117));
			this.timeGridColor.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "timeGridColor Action performed! " + e); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isTypeHisto && CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							((RecordSet) CurveSelectorContextMenu.this.recordSet).setTimeGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							// ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();

							if (CurveSelectorContextMenu.this.isTypeCompare) {
								CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
								if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
								CurveSelectorContextMenu.this.application.updateCompareWindow();
							}
						}
					}
				}
			});
			// timeGridLineStyle = new MenuItem(timeGridMenu, SWT.PUSH);
			// timeGridLineStyle.setText("Linientype");
			// timeGridLineStyle.setEnabled(false);
			// timeGridLineStyle.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(Level.FINEST, "timeGridLineStyle Action performed!");
			// if (recordNameKey != null) {
			// recordSet.setTimeGridLineStyle(SWT.LINE_DOT);
			// application.updateGraphicsWindow();
			// }
			// }
			// });

			this.valueGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.valueGrid.setText(Messages.getString(MessageIds.GDE_MSGT0100));
			this.valueGridMenu = new Menu(this.valueGrid);
			this.valueGrid.setMenu(this.valueGridMenu);
			this.valueGridMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int selectedValueGridOrdinal = CurveSelectorContextMenu.this.recordSet.getValueGridRecordOrdinal();
						if (selectedValueGridOrdinal >= 0) {
							AbstractRecord abstractRecord = CurveSelectorContextMenu.this.recordSet.isCompareSet() 
									? CurveSelectorContextMenu.this.recordSet.get(0)
											: CurveSelectorContextMenu.this.recordSet.get(selectedValueGridOrdinal);
							String recordNameUi = abstractRecord instanceof TrailRecord ? ((TrailRecord) abstractRecord).getNameReplacement() : abstractRecord.getName();
							CurveSelectorContextMenu.this.valueGridRecordName.setText(Messages.getString(MessageIds.GDE_MSGT0118) + recordNameUi);
							int gridType = CurveSelectorContextMenu.this.recordSet.getValueGridType();
							switch (gridType) {
							case RecordSet.VALUE_GRID_EVERY:
								CurveSelectorContextMenu.this.valueGridOff.setSelection(false);
								CurveSelectorContextMenu.this.valueGridEveryTick.setSelection(true);
								CurveSelectorContextMenu.this.valueGridEverySecond.setSelection(false);
								break;
							case RecordSet.VALUE_GRID_SECOND:
								CurveSelectorContextMenu.this.valueGridOff.setSelection(false);
								CurveSelectorContextMenu.this.valueGridEveryTick.setSelection(false);
								CurveSelectorContextMenu.this.valueGridEverySecond.setSelection(true);
								break;
							case RecordSet.VALUE_GRID_NONE:
							default:
								CurveSelectorContextMenu.this.valueGridOff.setSelection(true);
								CurveSelectorContextMenu.this.valueGridEveryTick.setSelection(false);
								CurveSelectorContextMenu.this.valueGridEverySecond.setSelection(false);
								break;
							}
						}
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.valueGridRecordName = new MenuItem(this.valueGridMenu, SWT.NONE);

			new MenuItem(this.valueGridMenu, SWT.SEPARATOR);

			this.valueGridOff = new MenuItem(this.valueGridMenu, SWT.CHECK);
			this.valueGridOff.setText(Messages.getString(MessageIds.GDE_MSGT0119));
			this.valueGridOff.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridOff Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setValueGridType(RecordSet.VALUE_GRID_NONE);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.VALUE_GRID_NONE);
						}
					}
				}
			});
			this.valueGridEveryTick = new MenuItem(this.valueGridMenu, SWT.CHECK);
			this.valueGridEveryTick.setText(Messages.getString(MessageIds.GDE_MSGT0120));
			this.valueGridEveryTick.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridMain Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setValueGridType(RecordSet.VALUE_GRID_EVERY);
						CurveSelectorContextMenu.this.recordSet.setValueGridRecordOrdinal(
								!CurveSelectorContextMenu.this.isTypeHisto && ((RecordSet) CurveSelectorContextMenu.this.recordSet).isCompareSet() ? 0 : CurveSelectorContextMenu.this.actualRecord.getOrdinal());
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.VALUE_GRID_EVERY);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.valueGridEverySecond = new MenuItem(this.valueGridMenu, SWT.CHECK);
			this.valueGridEverySecond.setText(Messages.getString(MessageIds.GDE_MSGT0121));
			this.valueGridEverySecond.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setValueGridType(RecordSet.VALUE_GRID_SECOND);
						CurveSelectorContextMenu.this.recordSet.setValueGridRecordOrdinal(CurveSelectorContextMenu.this.actualRecord.getOrdinal());
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));

						if (CurveSelectorContextMenu.this.isTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.VALUE_GRID_SECOND);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.valueGridColor = new MenuItem(this.valueGridMenu, SWT.PUSH);
			this.valueGridColor.setText(Messages.getString(MessageIds.GDE_MSGT0122));
			this.valueGridColor.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "horizontalGridColor Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setValueGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							//if (!CurveSelectorContextMenu.this.isTypeHisto) ((RecordSet) CurveSelectorContextMenu.this.recordSet).setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
							CurveSelectorContextMenu.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));

							if (CurveSelectorContextMenu.this.isTypeCompare) {
								CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
								if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
								CurveSelectorContextMenu.this.application.updateCompareWindow();
							}
						}
					}
				}
			});
			// horizontalGridLineStyle = new MenuItem(horizontalGridMenu, SWT.PUSH);
			// horizontalGridLineStyle.setText("Linientype");
			// horizontalGridLineStyle.setEnabled(false);
			// horizontalGridLineStyle.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(Level.FINEST, "horizontalGridLineStyle Action performed!");
			// if (recordNameKey != null) {
			// recordSet.setHorizontalGridLineStyle(SWT.LINE_DASH);
			// application.updateGraphicsWindow();
			// }
			// }
			// });

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.measurement = new MenuItem(popupmenu, SWT.CASCADE);
			this.measurement.setText(Messages.getString(MessageIds.GDE_MSGT0123));
			this.measurementMenu = new Menu(this.valueGrid);
			this.measurement.setMenu(this.measurementMenu);
			this.measurementMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "measurementMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						AbstractRecord abstractRecord = CurveSelectorContextMenu.this.recordSet.get(CurveSelectorContextMenu.this.recordNameMeasurement);
						if (abstractRecord != null) {
							String recordNameUi = abstractRecord instanceof TrailRecord ? ((TrailRecord) abstractRecord).getNameReplacement() : CurveSelectorContextMenu.this.recordNameMeasurement;
							CurveSelectorContextMenu.this.measurementRecordName.setText(Messages.getString(MessageIds.GDE_MSGT0124) + recordNameUi);
						}
						else
							CurveSelectorContextMenu.this.measurementRecordName.setText(Messages.getString(MessageIds.GDE_MSGT0124) + CurveSelectorContextMenu.this.recordNameMeasurement);
					}
				}

				@Override
				public void menuHidden(MenuEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "measurementMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.measurementRecordName = new MenuItem(this.measurementMenu, SWT.NONE);

			new MenuItem(this.measurementMenu, SWT.SEPARATOR);

			this.simpleMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.simpleMeasure.setText(Messages.getString(MessageIds.GDE_MSGT0125));
			this.simpleMeasure.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "measure.widgetSelected, event=" + evt); //$NON-NLS-1$
					setMeasurement(CurveSelectorContextMenu.this.recordNameKey, CurveSelectorContextMenu.this.simpleMeasure.getSelection());
				}

			});
			this.deltaMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.deltaMeasure.setText(Messages.getString(MessageIds.GDE_MSGT0126));
			this.deltaMeasure.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "deltaMeasure.widgetSelected, event=" + evt); //$NON-NLS-1$
					setDeltaMeasurement(CurveSelectorContextMenu.this.recordNameKey, CurveSelectorContextMenu.this.deltaMeasure.getSelection());
				}
			});
			this.avgMedianMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.avgMedianMeasure.setText(Messages.getString(MessageIds.GDE_MSGT0972));
			this.avgMedianMeasure.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					CurveSelectorContextMenu.log.finest(() -> "avgMedianMeasure.widgetSelected, event=" + evt); //$NON-NLS-1$
					setAvgMedianMeasurement(CurveSelectorContextMenu.this.recordNameKey, CurveSelectorContextMenu.this.avgMedianMeasure.getSelection());
				}
			});
				
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.copyCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.copyCurveCompare.setText(Messages.getString(MessageIds.GDE_MSGT0127));
			this.copyCurveCompare.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "copyCurveCompare Action performed! " + e); //$NON-NLS-1$
					CurveSelectorContextMenu.this.application.createCompareWindowTabItem(); // if not already exist
					String copyFromRecordKey = (String) popupmenu.getData(DataExplorer.RECORD_NAME);
					RecordSet copyFromRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
					if (copyFromRecordSet != null && copyFromRecordKey != null) {
						Record copyFromRecord = copyFromRecordSet.get(copyFromRecordKey);
						if (copyFromRecord != null && copyFromRecord.isVisible()) {
							if (CurveSelectorContextMenu.this.application.isWithCompareSet()) {
								RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
								if (!compareSet.get(compareSet.getFirstRecordName()).getUnit().equalsIgnoreCase(copyFromRecord.getUnit())) {
									CurveSelectorContextMenu.this.application
											.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0004, new Object[] { copyFromRecordKey + GDE.STRING_MESSAGE_CONCAT + compareSet.getFirstRecordName() }));
									//return; //enable curve compare using different units, but warn about this fact 
								}
								// while adding a new curve to compare set - reset the zoom mode
								CurveSelectorContextMenu.this.application.setCompareWindowMode(GraphicsMode.RESET, false);
							}

							RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
							String newRecordkey = CurveSelectorContextMenu.this.settings.isCurveCompareChannelConfigName()
									? copyFromRecord.getChannelConfigKey() + GDE.STRING_UNDER_BAR + copyFromRecordKey + GDE.STRING_UNDER_BAR + compareSet.size()
									: copyFromRecordKey + GDE.STRING_UNDER_BAR + compareSet.size();
							Record newRecord = compareSet.put(newRecordkey, copyFromRecord.clone()); // will delete channelConfigKey
							newRecord.setOrdinal(copyFromRecord.getOrdinal());
							newRecord.setDescription(copyFromRecordSet.getRecordSetDescription());
							newRecord.setVisible(true); // if a non visible record added

							if (compareSet.size() == 1) { // set grid line mode and color from settings (previous compare behavior)
								compareSet.setValueGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalType());
								compareSet.setValueGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalColor());
								compareSet.setTimeGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalType());
								compareSet.setTimeGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalColor());
								compareSet.setValueGridRecordOrdinal(0);
							}
							// check if the new added record exceeds the existing one in time or set draw limit and pad with dummy points
							double maxRecordTime_ms = compareSet.getCompareSetMaxScaleTime_ms();
							double newRecordMaxTime_ms = newRecord.getMaxTime_ms();
							newRecord.setCompareSetDrawLimit_ms(newRecordMaxTime_ms);

							if (newRecordMaxTime_ms > maxRecordTime_ms) {
								compareSet.setCompareSetMaxScaleTime_ms(newRecordMaxTime_ms);
								maxRecordTime_ms = newRecordMaxTime_ms;
								CurveSelectorContextMenu.log.fine(() -> "adapt compareSet maxRecordTime_sec = " + TimeLine.getFomatedTimeWithUnit(newRecordMaxTime_ms)); //$NON-NLS-1$

								// new added record exceed size of existing, existing needs draw limit to be updated and to be padded
								for (Entry<String, AbstractRecord> tmpRecordEntry : compareSet.entrySet()) {
									if (!newRecordkey.equals(tmpRecordEntry.getKey())) {
										Record tmpRecord = (Record) tmpRecordEntry.getValue();
										if (tmpRecord.getCompareSetDrawLimit_ms() == Integer.MAX_VALUE // draw linit untouched
												|| tmpRecord.getCompareSetDrawLimit_ms() < newRecordMaxTime_ms) {
											double avgTimeStep_ms = tmpRecord.getAverageTimeStep_ms();
											int steps = (int) ((maxRecordTime_ms - tmpRecord.getCompareSetDrawLimit_ms()) / avgTimeStep_ms);
											int value = tmpRecord.lastElement();
											double timeStep = tmpRecord.getLastTime_ms();
											for (int i = 0; i < steps; i++) {
												tmpRecord.add(value, timeStep += avgTimeStep_ms);
											}
											tmpRecord.setDrawTimeWidth(newRecordMaxTime_ms);
										}
									}
								}
							}
							else { // new record is shorter and needs to be padded and the draw limit to set
								double avgTimeStep_ms = newRecord.getAverageTimeStep_ms();
								int steps = (int) ((maxRecordTime_ms - newRecordMaxTime_ms) / avgTimeStep_ms);
								int value = newRecord.lastElement();
								double timeStep = newRecord.getLastTime_ms();
								for (int i = 0; i < steps; i++) {
									newRecord.add(value, timeStep += avgTimeStep_ms);
								}
								newRecord.setDrawTimeWidth(maxRecordTime_ms);
							}

							double oldMinValue = compareSet.getMinValue();
							double oldMaxValue = compareSet.getMaxValue();
							CurveSelectorContextMenu.log.fine(() -> String.format("scale values from compare set min=%.3f max=%.3f", compareSet.getMinValue(), compareSet.getMaxValue())); //$NON-NLS-1$
							for (int i = 0; i < compareSet.size(); i++) {
								Record record = compareSet.get(i);
								double newMinValue = record.getMinScaleValue();
								double newMaxValue = record.getMaxScaleValue();
								CurveSelectorContextMenu.log.fine(() ->	String.format("scale values from record (" + record.getName() + ") to be checked min=%.3f max=%.3f", newMinValue, newMaxValue)); //$NON-NLS-1$ //$NON-NLS-2$

								if (newMinValue < oldMinValue) {
									compareSet.setMinValue(newMinValue); // store new min value into record set
								}
								oldMinValue = compareSet.getMinValue();
								if (newMaxValue > oldMaxValue) {
									compareSet.setMaxValue(newMaxValue); // store new max value into record set
								}
							}
							for (AbstractRecord tmpRecord : compareSet.values()) {
								Record record = (Record) tmpRecord;
								record.setStartEndDefined(true, compareSet.getMinValue(), compareSet.getMaxValue());
							}

							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
						else
							CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0005));

						// this is required before zoom operation
						CurveSelectorContextMenu.this.application.setCompareWindowMode(GraphicsMode.RESET, false);
					}
				}
			});
			this.cleanCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.cleanCurveCompare.setText(Messages.getString(MessageIds.GDE_MSGT0128));
			this.cleanCurveCompare.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					CurveSelectorContextMenu.log.finest(() -> "cleanCurveCompare Action performed! " + e); //$NON-NLS-1$
					RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
					compareSet.clear();
					CurveSelectorContextMenu.this.application.updateCompareWindow();
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void setAllEnabled(boolean enabled) {
		this.lineVisible.setEnabled(enabled);
		this.lineColor.setEnabled(enabled);
		this.lineWidth.setEnabled(enabled);
		this.lineType.setEnabled(enabled);
		this.smoothAtCurrentDropItem.setEnabled(enabled);
		if (this.smoothVoltageCurveItem != null) this.smoothVoltageCurveItem.setEnabled(enabled);
		this.axisEndValues.setEnabled(enabled);
		this.axisNumberFormat.setEnabled(enabled);
		this.axisPosition.setEnabled(enabled);
		this.timeGrid.setEnabled(enabled);
		this.valueGrid.setEnabled(enabled);
		this.measurement.setEnabled(enabled);
		this.copyCurveCompare.setEnabled(enabled);
		this.cleanCurveCompare.setEnabled(enabled);
	}

	/**
	 * check measurement record name
	 */
	boolean isMeasurementWhileNameChanged(String tmpRecordNameMeasurement) {
		boolean isChanged = false;
		if (!this.recordNameMeasurement.equals(tmpRecordNameMeasurement) && this.recordNameMeasurement.length() > 1) {
			this.application.setMeasurementActive(this.recordNameMeasurement, false);
			this.application.setDeltaMeasurementActive(this.recordNameMeasurement, false);
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			isChanged = true;
		}
		return isChanged;
	}

	/**
	 * query the state of context menu and sub dialogs
	 * @return inactive state true/false
	 */
	public boolean isActive() {
		boolean isActive = (this.menu != null && this.menu.isVisible()) || this.isActiveEnValueDialog || this.isActiveColorDialog;
		CurveSelectorContextMenu.log.fine(() -> "isActive = " + isActive);
		return isActive;
	}

	/**
	 * Uncheck the measuring menu entries.
	 */
	public void resetMeasuring() {
			this.recordNameMeasurement = GDE.STRING_BLANK;
			CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
			CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
			CurveSelectorContextMenu.this.avgMedianMeasure.setSelection(false);
			CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
		}

	/**
	 * perform all activities as if the menu item was toggled.
	 * @param isActive
	 * @param tmpRecordNameMeasurement
	 */
	public void setMeasurement(String tmpRecordNameMeasurement, boolean isActive) {
		if (isMeasurementWhileNameChanged(tmpRecordNameMeasurement) || isActive) {
			this.recordNameMeasurement = tmpRecordNameMeasurement;
			setMeasurementActive(tmpRecordNameMeasurement, true);
			CurveSelectorContextMenu.this.simpleMeasure.setSelection(true);
			CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
			CurveSelectorContextMenu.this.avgMedianMeasure.setSelection(false);
		}
		else {
			this.recordNameMeasurement = GDE.STRING_BLANK;
			setMeasurementActive(tmpRecordNameMeasurement, false);
			CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
			CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
		}
	}

	private void setMeasurementActive(String tmpRecordNameMeasurement, boolean enabled) {
		if (this.isTypeHisto) {
			this.application.getPresentHistoExplorer().getActiveHistoChartTabItem().setMeasurementActive(tmpRecordNameMeasurement, enabled, false);
		} else {
			this.application.setMeasurementActive(tmpRecordNameMeasurement, enabled);
		}
	}

	/**
	 * perform all activities as if the menu item was toggled.
	 * @param isActive
	 * @param tmpRecordNameKey
	 */
	public void setDeltaMeasurement(String tmpRecordNameKey, boolean isActive) {
		if (isMeasurementWhileNameChanged(tmpRecordNameKey) || isActive) {
			this.recordNameMeasurement = tmpRecordNameKey;
			setDeltaMeasurementActive(tmpRecordNameKey, true);
			CurveSelectorContextMenu.this.deltaMeasure.setSelection(true);
			CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
			CurveSelectorContextMenu.this.avgMedianMeasure.setSelection(false);
		}
		else {
			this.recordNameMeasurement = GDE.STRING_BLANK;
			setDeltaMeasurementActive(tmpRecordNameKey, false);
			CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
			CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
		}
	}

	private void setDeltaMeasurementActive(String tmpRecordNameMeasurement, boolean enabled) {
		if (this.isTypeHisto) {
			this.application.getPresentHistoExplorer().getActiveHistoChartTabItem().setMeasurementActive(tmpRecordNameMeasurement, enabled, true);
		} else {
			this.application.setDeltaMeasurementActive(tmpRecordNameMeasurement, enabled);
		}
	}

	/**
	 * perform all activities as if the menu item was toggled.
	 * @param isActive
	 * @param tmpRecordNameKey
	 */
	public void setAvgMedianMeasurement(String tmpRecordNameKey, boolean isActive) {
		if (isMeasurementWhileNameChanged(tmpRecordNameKey) || isActive) {
			this.recordNameMeasurement = tmpRecordNameKey;
			setAvgMedianMeasurementActive(tmpRecordNameKey, true);
			CurveSelectorContextMenu.this.avgMedianMeasure.setSelection(true);
			CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
			CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
		}
		else {
			this.recordNameMeasurement = GDE.STRING_BLANK;
			setAvgMedianMeasurementActive(tmpRecordNameKey, false);
			CurveSelectorContextMenu.this.avgMedianMeasure.setSelection(false);
			CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
		}
	}

	private void setAvgMedianMeasurementActive(String tmpRecordNameMeasurement, boolean enabled) {
		if (this.isTypeHisto) {
			this.application.getPresentHistoExplorer().getActiveHistoChartTabItem().setMeasurementActive(tmpRecordNameMeasurement, enabled, true);
		} else {
			this.application.setAvgMedianMeasurementActive(tmpRecordNameMeasurement, enabled);
		}
	}

}
