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

package gde.histo.ui.menu;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.SourceDataSet;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.ExclusionActivity;
import gde.histo.ui.HistoExplorer;
import gde.histo.utils.PathUtils;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.FileUtils;

/**
 * Provides a context menu to the histo table tab and enable selection of background color, ...
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractTabAreaContextMenu {
	private static final String		$CLASS_NAME						= AbstractTabAreaContextMenu.class.getName();
	protected static final Logger	log										= Logger.getLogger($CLASS_NAME);

	protected final DataExplorer	application						= DataExplorer.getInstance();
	protected final HistoExplorer	presentHistoExplorer	= DataExplorer.getInstance().getPresentHistoExplorer();
	protected final Settings			settings							= Settings.getInstance();

	protected boolean							isCreated							= false;

	protected MenuItem						curveSelectionItem, displayGraphicsHeaderItem;
	protected MenuItem						displayGraphicsCommentItem, displayGraphicsCurveSurvey, suppressModeItem, partialTableItem;
	protected MenuItem						copyTabItem, copyPrintImageItem;
	protected MenuItem						fileName;
	protected MenuItem						openRecordSetItem, deleteFileItem, openFolderItem;
	protected Menu								hideMenu, trackMenu;
	protected MenuItem						hideItem;
	protected MenuItem						hideMenuRecordSetItem, hideMenuFileItem, hideMenuRevokeItem;
	protected MenuItem						trackItem;

	public enum TabMenuOnDemand {
		IS_CURSOR_IN_CANVAS, DATA_LINK_PATH, DATA_FILE_PATH, RECORDSET_BASE_NAME, EXCLUDED_LIST //
		, SUMMARY_WARNING
	}

	public abstract void createMenu(Menu popupMenu);

	protected void createCheckItems(Menu popupMenu) {
		{
			suppressModeItem = new MenuItem(popupMenu, SWT.CHECK);
			suppressModeItem.setText(Messages.getString(MessageIds.GDE_MSGT0859));
			if (!GDE.IS_OS_ARCH_ARM) suppressModeItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0860));
			suppressModeItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "suppressModeItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					settings.setSuppressMode(suppressModeItem.getSelection());
					application.getMenuBar().getSuppressModeItem().setSelection(suppressModeItem.getSelection());

					presentHistoExplorer.resetHisto();
				}
			});
		}
		{
			curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
			curveSelectionItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
			curveSelectionItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
					boolean selection = curveSelectionItem.getSelection();
					application.enableCurveSelector(selection);
					application.getMenuBar().getCurveSelectionMenuItem().setSelection(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
			displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsHeaderItem.getSelection();
					application.getMenuBar().getGraphicsHeaderMenuItem().setSelection(selection);
					application.enableGraphicsHeader(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsCommentItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
			if (!GDE.IS_OS_ARCH_ARM) displayGraphicsCommentItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0878));
			displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsCommentItem.getSelection();
					application.getMenuBar().getRecordCommentMenuItem().setSelection(selection);
					application.enableRecordSetComment(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			displayGraphicsCurveSurvey = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsCurveSurvey.setText(Messages.getString(MessageIds.GDE_MSGT0876));
			if (!GDE.IS_OS_ARCH_ARM) displayGraphicsCurveSurvey.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0877));
			displayGraphicsCurveSurvey.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "displayGraphicsCurveSurvey action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsCurveSurvey.getSelection();
					application.getMenuBar().getGraphicsCurveSurveyMenuItem().setSelection(selection);
					presentHistoExplorer.enableCurveSurvey(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			partialTableItem = new MenuItem(popupMenu, SWT.CHECK);
			partialTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
			partialTableItem.setSelection(Settings.getInstance().isPartialDataTable());
			partialTableItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "partialTableItem action performed! " + e); //$NON-NLS-1$
					boolean selection = partialTableItem.getSelection();
					application.getMenuBar().getPartialTableMenuItem().setSelection(selection);
					presentHistoExplorer.enablePartialDataTable(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
	}

	protected void createCopyItems(Menu popupMenu) {
		{
			copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			copyTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0026).substring(0, Messages.getString(MessageIds.GDE_MSGT0026).lastIndexOf('\t')));
			copyTabItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					application.copyTabContentAsImage();
				}
			});
		}
		{
			copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
			copyPrintImageItem.setText(Messages.getString(MessageIds.GDE_MSGT0027).substring(0, Messages.getString(MessageIds.GDE_MSGT0027).lastIndexOf('\t')));
			copyPrintImageItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
					application.copyGraphicsPrintImage();
				}
			});
		}
	}

	protected void createFileItems(Menu popupMenu) {
		{
			fileName = new MenuItem(popupMenu, SWT.None);
			fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
			if (!GDE.IS_OS_ARCH_ARM) fileName.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0863));
		}
		new MenuItem(popupMenu, SWT.SEPARATOR);
		{
			openRecordSetItem = new MenuItem(popupMenu, SWT.PUSH);
			openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));
			if (!GDE.IS_OS_ARCH_ARM) openRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0851));
			openRecordSetItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					SourceDataSet sourceDataSet = SourceDataSet.createSourceDataSet(Paths.get((String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString())), Analyzer.getInstance());
					String recordSetName = popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString().split(Pattern.quote(TrailRecordSet.BASE_NAME_SEPARATOR))[0];
					if (sourceDataSet != null && sourceDataSet.load(recordSetName)) {
						application.selectTab(c -> c instanceof GraphicsWindow && ((GraphicsWindow) c).getGraphicsType().equals(GraphicsType.NORMAL));
						application.updateGraphicsWindow();
					}
				}
			});
		}
		{
			deleteFileItem = new MenuItem(popupMenu, SWT.PUSH);
			deleteFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0861));
			if (!GDE.IS_OS_ARCH_ARM) deleteFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0862));
			deleteFileItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deleteFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					// check if the file exists and if the user really wants to delete it
					File file = new File((String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()));
					if (FileUtils.checkFileExist(file.getPath())) {
						String linkFilePath = (String) popupMenu.getData(TabMenuOnDemand.DATA_LINK_PATH.toString());
						if (file.getPath().endsWith(GDE.FILE_ENDING_DOT_OSD) && !linkFilePath.isEmpty()) {
							FileUtils.deleteFile(linkFilePath);
							if (application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0071, new Object[] {
									file.getAbsolutePath() })) == SWT.YES) {
								FileUtils.deleteFile(file.getPath());
								presentHistoExplorer.resetHisto();
							}
						} else {
							if (application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0050, new Object[] {
									file.getAbsolutePath() })) == SWT.YES) {
								FileUtils.deleteFile(file.getPath());
								presentHistoExplorer.resetHisto();
							}
						}
					}
				}
			});
		}
		{
			openFolderItem = new MenuItem(popupMenu, SWT.PUSH);
			openFolderItem.setText(Messages.getString(MessageIds.GDE_MSGT0873));
			if (!GDE.IS_OS_ARCH_ARM) openFolderItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0874));
			openFolderItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openFolderItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().open(Paths.get((String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString())).getParent().toFile());
						} catch (IOException e) {
							application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0065));
						}
					}
				}
			});
		}
		new MenuItem(popupMenu, SWT.SEPARATOR);
		{
			hideItem = new MenuItem(popupMenu, SWT.CASCADE);
			hideItem.setText(Messages.getString(MessageIds.GDE_MSGT0852));
			hideMenu = new Menu(hideItem);
			hideItem.setMenu(hideMenu);
		}
		{
			hideMenuRecordSetItem = new MenuItem(hideMenu, SWT.PUSH);
			hideMenuRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0853));
			if (!GDE.IS_OS_ARCH_ARM) hideMenuRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0854));
			hideMenuRecordSetItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					ExclusionActivity.setExcludeRecordSet(Paths.get((String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString())), popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString());

					settings.setSuppressMode(true);
					application.getMenuBar().getSuppressModeItem().setSelection(true);

					presentHistoExplorer.updateHistoTabs(false, true, true);
				}
			});
		}
		{
			hideMenuFileItem = new MenuItem(hideMenu, SWT.PUSH);
			hideMenuFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0855));
			if (!GDE.IS_OS_ARCH_ARM) hideMenuFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0856));
			hideMenuFileItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					ExclusionActivity.setExcludeRecordSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()), GDE.STRING_EMPTY);

					settings.setSuppressMode(true);
					application.getMenuBar().getSuppressModeItem().setSelection(true);

					presentHistoExplorer.updateHistoTabs(false, true, true);
				}
			});
		}
		new MenuItem(hideMenu, SWT.SEPARATOR);
		{
			hideMenuRevokeItem = new MenuItem(hideMenu, SWT.PUSH);
			hideMenuRevokeItem.setText(Messages.getString(MessageIds.GDE_MSGT0857));
			hideMenuRevokeItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					String dataFilePath = (String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString());
					if (dataFilePath != null) {
						ExclusionActivity.clearExcludeLists();
						presentHistoExplorer.resetHisto();
					}
				}
			});
		}
	}

	protected void setCoreEnabled(boolean enabled) {
		fileName.setEnabled(enabled);
		openRecordSetItem.setEnabled(enabled);
		deleteFileItem.setEnabled(enabled);
		openFolderItem.setEnabled(enabled);
		hideItem.setEnabled(enabled);
		hideMenuRecordSetItem.setEnabled(enabled);
		hideMenuFileItem.setEnabled(enabled);
		hideMenuRevokeItem.setEnabled(enabled);
		suppressModeItem.setEnabled(enabled);
		curveSelectionItem.setEnabled(enabled);
		displayGraphicsHeaderItem.setEnabled(enabled);
		displayGraphicsCommentItem.setEnabled(enabled);
		displayGraphicsCurveSurvey.setEnabled(enabled);
		copyTabItem.setEnabled(enabled);
		copyPrintImageItem.setEnabled(enabled);

		partialTableItem.setEnabled(enabled);
	}

	protected void setCommonItems(Menu popupMenu) {
		suppressModeItem.setSelection(application.getMenuBar().getSuppressModeItem().getSelection());
		curveSelectionItem.setSelection(application.getMenuBar().getCurveSelectionMenuItem().getSelection());
		displayGraphicsHeaderItem.setSelection(application.getMenuBar().getGraphicsHeaderMenuItem().getSelection());
		displayGraphicsCommentItem.setSelection(application.getMenuBar().getRecordCommentMenuItem().getSelection());
		displayGraphicsCurveSurvey.setSelection(application.getMenuBar().getGraphicsCurveSurveyMenuItem().getSelection());
		partialTableItem.setSelection(application.getMenuBar().getPartialTableMenuItem().getSelection());

		String excludedList = (String) popupMenu.getData(TabMenuOnDemand.EXCLUDED_LIST.toString());
		if (excludedList == null) excludedList = GDE.STRING_EMPTY;
		String excludedText = excludedList.isEmpty() ? ""
				: GDE.STRING_BLANK + excludedList.replace(GDE.STRING_CSV_SEPARATOR, GDE.STRING_NEW_LINE + GDE.STRING_BLANK) + GDE.STRING_NEW_LINE;
		if (!GDE.IS_OS_ARCH_ARM) hideItem.setToolTipText(excludedText + Messages.getString(MessageIds.GDE_MSGT0798));
		if (!GDE.IS_OS_ARCH_ARM) hideMenuRevokeItem.setToolTipText(excludedText + Messages.getString(MessageIds.GDE_MSGT0858));

		// clearing does not depend on updated warning info clearExclusiveWarnings.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0918));
		fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
		openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));
	}

	protected void setDataPathItems(Menu popupMenu, Path dataPath) {
		openFolderItem.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

		String tmpFileName = dataPath.getFileName().toString();
		String displayName = tmpFileName.length() > 22 ? GDE.STRING_ELLIPSIS + tmpFileName.substring(tmpFileName.length() - 22) : tmpFileName;
		fileName.setText(">> " + displayName.toString() + GDE.STRING_BLANK_COLON_BLANK + String.format("%1.22s", popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString()) + " <<"); //$NON-NLS-1$ //$NON-NLS-2$
		List<String> dataSetExtentions = DirectoryType.IMPORT.getDataSetExtentions(Analyzer.getInstance().getActiveDevice(), Analyzer.getInstance().getSettings());
		boolean isImport = dataSetExtentions.contains(PathUtils.getFileExtention(dataPath));
		openRecordSetItem.setText(Messages.getString(isImport ? MessageIds.GDE_MSGT0850 : MessageIds.GDE_MSGT0849));
	}

}