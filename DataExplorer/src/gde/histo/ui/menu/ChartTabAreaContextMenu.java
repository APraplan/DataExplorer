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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.Analyzer;
import gde.GDE;
import gde.device.resource.DeviceXmlResource;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.exclusions.InclusionData;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * Provides a context menu to the graphics chart area and enable selection of background color, ...
 * @author Thomas Eickert (USER)
 */
public final class ChartTabAreaContextMenu extends AbstractTabAreaContextMenu {
	private final static String				$CLASS_NAME	= ChartTabAreaContextMenu.class.getName();
	@SuppressWarnings("hiding")
	private final static Logger				log					= Logger.getLogger($CLASS_NAME);

	private MenuItem									borderColorItem;
	private Menu											warningMenu;
	private MenuItem									boxplotItem, spreadItem, spotsItem;
	private MenuItem									warningCountItem, warningCountItem0, warningCountItem1, warningCountItem2, warningCountItem3;
	private MenuItem									warningLevelItem, warningLevelNone, warningLevelItem0, warningLevelItem1, warningLevelItem2;
	private MenuItem									isExclusiveWarning, clearExclusiveWarnings;
	private MenuItem									outherAreaColorItem, innerAreaColorItem;

	private Optional<SummaryWarning>	warning;

	public static class SummaryWarning {
		private final Path			dataPath;
		private final String		recordName;
		private final String[]	exclusiveRecordNames;

		public SummaryWarning(Path dataPath, String recordName, String[] exclusiveRecordNames) {
			if (dataPath == null) throw new IllegalArgumentException();
			if (recordName == null) throw new IllegalArgumentException();
			if (exclusiveRecordNames == null) throw new IllegalArgumentException();
			this.dataPath = dataPath;
			this.recordName = recordName;
			this.exclusiveRecordNames = exclusiveRecordNames;
		}

		String getDisplayRecordName() {
			String recordHint = !recordName.isEmpty() ? DeviceXmlResource.getInstance().getReplacement(recordName) : GDE.STRING_EMPTY;
			String displayName = recordHint.length() > 15 ? GDE.STRING_ELLIPSIS + recordHint.substring(recordHint.length() - 15) : recordHint;
			return GDE.STRING_BLANK_LEFT_BRACKET + displayName + GDE.STRING_RIGHT_BRACKET;
		}

		String getDisplayNames() {
			String result = Arrays.stream(DeviceXmlResource.getInstance().getReplacements(exclusiveRecordNames)) //
					.collect(Collectors.joining(GDE.STRING_NEW_LINE + GDE.STRING_BLANK));
			return result.isEmpty() ? result : GDE.STRING_BLANK + result;
		}

		boolean isValid() {
			return !dataPath.toString().isEmpty();
		}

		boolean isExclusiveRecord() {
			return Arrays.asList(exclusiveRecordNames).contains(recordName);
		}

		@Override
		public String toString() {
			return "SummaryWarning [recordName=" + this.recordName + ", exclusiveRecordNames=" + Arrays.toString(this.exclusiveRecordNames) + ", dataPath=" + this.dataPath + ", recordName=" + this.recordName + "]";
		}
	}

	@Override
	public void createMenu(Menu popupMenu) {
		popupMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "menuShown action " + e); //$NON-NLS-1$
				setCommonItems(popupMenu);

				warning = Optional.ofNullable((SummaryWarning) popupMenu.getData(TabMenuOnDemand.SUMMARY_WARNING.toString()));
				log.log(Level.FINEST, "", warning);
				setWarningCountIndex(settings.getReminderCountIndex());
				setWarningLevel(settings.getReminderLevel());
				isExclusiveWarning.setText(Messages.getString(MessageIds.GDE_MSGT0915));
				isExclusiveWarning.setSelection(false);

				if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
					setCoreEnabled(false);
					setTrackEnabled(true);
					isExclusiveWarning.setEnabled(false);

					suppressModeItem.setEnabled(true);
					curveSelectionItem.setEnabled(true);
					hideItem.setEnabled(true);
					hideMenuRevokeItem.setEnabled(true);

					displayGraphicsHeaderItem.setEnabled(true);
					displayGraphicsCommentItem.setEnabled(true);
					displayGraphicsCurveSurvey.setEnabled(true);
					copyTabItem.setEnabled(true);
					copyPrintImageItem.setEnabled(true);

					setColorEnabled(true);
				} else {
					setCoreEnabled(true);
					setTrackEnabled(true);
					trackItem.setEnabled(warning.map(SummaryWarning::isValid).orElse(true));
					isExclusiveWarning.setEnabled(warning.map(SummaryWarning::isValid).orElse(false));

					setColorEnabled(true);

					String dataFilePath = (String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString());
					if (dataFilePath.isEmpty()) {
						fileName.setEnabled(false);
						openRecordSetItem.setEnabled(false);
						deleteFileItem.setEnabled(false);
						openFolderItem.setEnabled(false);

						hideMenuRecordSetItem.setEnabled(false);
						hideMenuFileItem.setEnabled(false);
					} else {
						setDataPathItems(popupMenu, Paths.get(dataFilePath));
					}
				}
				isExclusiveWarning.setText(Messages.getString(MessageIds.GDE_MSGT0915) + warning.map(SummaryWarning::getDisplayRecordName).orElse(GDE.STRING_EMPTY));
				isExclusiveWarning.setSelection(warning.map(SummaryWarning::isExclusiveRecord).orElse(false));
				String warningList = warning.map(w -> {
					String includedNames = w.getDisplayNames();
					return includedNames.isEmpty() ? includedNames : includedNames + GDE.STRING_NEW_LINE;
				}).orElse(GDE.STRING_EMPTY);
				if (!GDE.IS_OS_ARCH_ARM) clearExclusiveWarnings.setToolTipText(warningList + Messages.getString(MessageIds.GDE_MSGT0918));
				if (!GDE.IS_OS_ARCH_ARM) trackItem.setToolTipText(warningList + Messages.getString(MessageIds.GDE_MSGT0891));

				// clear consumed menu data
				popupMenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString(), null);
				popupMenu.setData(TabMenuOnDemand.SUMMARY_WARNING.toString(), null);
			}

			@Override
			public void menuHidden(MenuEvent e) {
				// ignore
			}
		});

		if (!isCreated) {
			createFileItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createTrackItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createCheckItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createCopyItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createColorItems(popupMenu);

			isCreated = true;
		}

	}

	protected void createTrackItems(Menu popupMenu) {
		{
			trackItem = new MenuItem(popupMenu, SWT.CASCADE);
			trackItem.setText(Messages.getString(MessageIds.GDE_MSGT0890));
			trackMenu = new Menu(trackItem);
			trackItem.setMenu(trackMenu);
		}
		{
			boxplotItem = new MenuItem(trackMenu, SWT.CHECK);
			boxplotItem.setText(Messages.getString(MessageIds.GDE_MSGT0892));
			if (!GDE.IS_OS_ARCH_ARM) boxplotItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0893));
			boxplotItem.setSelection(settings.isSummaryBoxVisible());
			boxplotItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "boxplotItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					boolean selection = boxplotItem.getSelection();
					settings.setSummaryBoxVisible(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			spreadItem = new MenuItem(trackMenu, SWT.CHECK);
			spreadItem.setText(Messages.getString(MessageIds.GDE_MSGT0902));
			if (!GDE.IS_OS_ARCH_ARM) spreadItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0903));
			spreadItem.setSelection(settings.isSummarySpreadVisible());
			spreadItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "spreadItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					boolean selection = spreadItem.getSelection();
					settings.setSummarySpreadVisible(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			spotsItem = new MenuItem(trackMenu, SWT.CHECK);
			spotsItem.setText(Messages.getString(MessageIds.GDE_MSGT0894));
			if (!GDE.IS_OS_ARCH_ARM) spotsItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0895));
			spotsItem.setSelection(settings.isSummarySpotsVisible());
			spotsItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "spotsItem.widgetSelected, event=" + evt); //$NON-NLS-1$
					boolean selection = spotsItem.getSelection();
					settings.setSummarySpotsVisible(selection);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}

		{
			warningCountItem = new MenuItem(trackMenu, SWT.CASCADE);
			warningCountItem.setText(Messages.getString(MessageIds.GDE_MSGT0888));
			if (!GDE.IS_OS_ARCH_ARM) warningCountItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0889));
			warningMenu = new Menu(hideItem);
			warningCountItem.setMenu(warningMenu);
		}
		{
			warningCountItem0 = new MenuItem(warningMenu, SWT.CHECK);
			warningCountItem0.setText(String.valueOf(settings.getReminderCount(0)) + " logs");
			warningCountItem0.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem0.widgetSelected, event=" + evt); //$NON-NLS-1$
					setWarningCountIndex(0);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningCountItem1 = new MenuItem(warningMenu, SWT.CHECK);
			warningCountItem1.setText(String.valueOf(settings.getReminderCount(1)) + " logs");
			warningCountItem1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem1.widgetSelected, event=" + evt); //$NON-NLS-1$
					setWarningCountIndex(1);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningCountItem2 = new MenuItem(warningMenu, SWT.CHECK);
			warningCountItem2.setText(String.valueOf(settings.getReminderCount(2)) + " logs");
			warningCountItem2.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem2.widgetSelected, event=" + evt); //$NON-NLS-1$
					setWarningCountIndex(2);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningCountItem3 = new MenuItem(warningMenu, SWT.CHECK);
			warningCountItem3.setText(String.valueOf(settings.getReminderCount(3)) + " logs");
			warningCountItem3.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem3.widgetSelected, event=" + evt); //$NON-NLS-1$
					setWarningCountIndex(3);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}

		{
			warningLevelItem = new MenuItem(trackMenu, SWT.CASCADE);
			warningLevelItem.setText(Messages.getString(MessageIds.GDE_MSGT0912));
			if (!GDE.IS_OS_ARCH_ARM) warningLevelItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0913));
			warningMenu = new Menu(hideItem);
			warningLevelItem.setMenu(warningMenu);
		}
		{
			warningLevelNone = new MenuItem(warningMenu, SWT.CHECK);
			warningLevelNone.setText(Messages.getString(MessageIds.GDE_MSGT0919));
			warningLevelNone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningLevelNone.widgetSelected, event=" + evt);
					setWarningLevel(-1);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningLevelItem0 = new MenuItem(warningMenu, SWT.CHECK);
			warningLevelItem0.setText(Messages.getString(MessageIds.GDE_MSGT0904));
			warningLevelItem0.setImage(SWTResourceManager.getImage("gde/resource/caution_portrait.png"));
			warningLevelItem0.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningLevelItem0.widgetSelected, event=" + evt);
					setWarningLevel(0);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningLevelItem1 = new MenuItem(warningMenu, SWT.CHECK);
			warningLevelItem1.setText(Messages.getString(MessageIds.GDE_MSGT0905));
			warningLevelItem1.setImage(SWTResourceManager.getImage("gde/resource/caution_portrait_yellow.png"));
			warningLevelItem1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningLevelItem1.widgetSelected, event=" + evt);
					setWarningLevel(1);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		{
			warningLevelItem2 = new MenuItem(warningMenu, SWT.CHECK);
			warningLevelItem2.setText(Messages.getString(MessageIds.GDE_MSGT0910));
			warningLevelItem2.setImage(SWTResourceManager.getImage("gde/resource/caution_portrait_blue.png"));
			warningLevelItem2.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningLevelItem2.widgetSelected, event=" + evt);
					setWarningLevel(2);
					presentHistoExplorer.updateHistoTabs(false, false, true);
				}
			});
		}
		new MenuItem(trackMenu, SWT.SEPARATOR);
		{
			isExclusiveWarning = new MenuItem(trackMenu, SWT.CHECK);
			isExclusiveWarning.setText(Messages.getString(MessageIds.GDE_MSGT0915));
			if (!GDE.IS_OS_ARCH_ARM) isExclusiveWarning.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0916));
			isExclusiveWarning.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "isEclusiveWarning.widgetSelected, event=" + evt);
					warning.ifPresent(w -> {
						InclusionData inclusionData = new InclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
						if (isExclusiveWarning.getSelection()) {
							inclusionData.setProperty(w.recordName);
						} else {
							inclusionData.deleteProperty(w.recordName);
						}
						inclusionData.store();
						presentHistoExplorer.updateHistoTabs(false, false, true);
					});
				}
			});
		}
		{
			clearExclusiveWarnings = new MenuItem(trackMenu, SWT.PUSH);
			clearExclusiveWarnings.setText(Messages.getString(MessageIds.GDE_MSGT0917));
			if (!GDE.IS_OS_ARCH_ARM) clearExclusiveWarnings.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0918));
			clearExclusiveWarnings.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "isEclusiveWarning.widgetSelected, event=" + evt);
					warning.ifPresent(w -> {
						InclusionData inclusionData = new InclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
						inclusionData.delete();
						presentHistoExplorer.updateHistoTabs(false, false, true);
					});
				}
			});
		}
	}

	protected void createColorItems(Menu popupMenu) {
		{
			outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
			outherAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0462));
			outherAreaColorItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
					RGB rgb = application.openColorDialog();
					if (rgb != null) {
						application.setSurroundingBackground(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
					}
				}
			});
		}
		{
			innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
			innerAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0463));
			innerAreaColorItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
					RGB rgb = application.openColorDialog();
					if (rgb != null) {
						application.setInnerAreaBackground(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
					}
				}
			});
		}
		{
			borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
			borderColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0464));
			borderColorItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
					RGB rgb = application.openColorDialog();
					if (rgb != null) {
						application.setBorderColor(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
					}
				}
			});
		}
	}

	protected void setTrackEnabled(boolean enabled) {
		trackItem.setEnabled(enabled);
		boxplotItem.setEnabled(enabled);
		spreadItem.setEnabled(enabled);
		spotsItem.setEnabled(enabled);
		warningCountItem.setEnabled(enabled);
		warningCountItem0.setEnabled(enabled);
		warningCountItem1.setEnabled(enabled);
		warningCountItem2.setEnabled(enabled);
		warningCountItem3.setEnabled(enabled);
		warningLevelItem.setEnabled(enabled);
		warningLevelNone.setEnabled(enabled);
		warningLevelItem0.setEnabled(enabled);
		warningLevelItem1.setEnabled(enabled);
		warningLevelItem2.setEnabled(enabled);
		isExclusiveWarning.setEnabled(enabled);
		clearExclusiveWarnings.setEnabled(enabled);
	}

	protected void setColorEnabled(boolean enabled) {
		borderColorItem.setEnabled(enabled);
	}

	protected void setWarningCountIndex(int newIndex) {
		settings.setReminderCountIndex(String.valueOf(newIndex));
		warningCountItem0.setSelection(newIndex == 0);
		warningCountItem1.setSelection(newIndex == 1);
		warningCountItem2.setSelection(newIndex == 2);
		warningCountItem3.setSelection(newIndex == 3);
	}

	protected void setWarningLevel(int newLevel) {
		settings.setReminderLevel(String.valueOf(newLevel));
		warningLevelNone.setSelection(newLevel == -1);
		warningLevelItem0.setSelection(newLevel == 0);
		warningLevelItem1.setSelection(newLevel == 1);
		warningLevelItem2.setSelection(newLevel == 2);
	}

}
