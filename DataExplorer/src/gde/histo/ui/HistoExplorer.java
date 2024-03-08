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

import static gde.ui.DataExplorer.TAB_INDEX_HISTO_SUMMARY;
import static gde.ui.DataExplorer.TAB_INDEX_HISTO_TABLE;
import static java.util.logging.Level.SEVERE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.datasources.HistoSet;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.recordings.TrailDataTags;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.datasources.SupplementObjectFolder;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * History module supplement for the main application class of DataExplorer.
 * @author Thomas Eickert (USER)
 */
public class HistoExplorer {
	static final String								$CLASS_NAME							= HistoExplorer.class.getName();
	static final Logger								log											= Logger.getLogger($CLASS_NAME);

	private final static int					VOLATILE_STATUS_CYCLES	= 3;														// number of redraws

	private final DataExplorer				application							= DataExplorer.getInstance();
	private final Settings						settings								= Settings.getInstance();

	private final CTabFolder					displayTab;
	private final HistoSet						histoSet;

	private VolatileStatusMessage			volatileStatusMessage		= null;
	private List<AbstractChartWindow>	chartTabItems						= new ArrayList<>();
	private List<HistoTableWindow>		tableTabItems						= new ArrayList<>();

	public HistoExplorer(CTabFolder displayTab) {
		this.displayTab = displayTab;

		this.settings.setHistoActive(true);
		this.histoSet = new HistoSet();
	}

	/**
	 * Build and fill the tabs.
	 */
	public void initHisto() {
		SupplementObjectFolder.checkAndCreate();
		SupplementObjectFolder.updateLogMirror();

		int tabLength = displayTab.getItems().length;
		int positionS = tabLength < TAB_INDEX_HISTO_SUMMARY ? tabLength : TAB_INDEX_HISTO_SUMMARY;
		chartTabItems.add(HistoSummaryWindow.create(displayTab, SWT.NONE, positionS));

		int positionT = tabLength < TAB_INDEX_HISTO_TABLE ? tabLength : TAB_INDEX_HISTO_TABLE;
		tableTabItems.add(HistoTableWindow.create(displayTab, SWT.NONE, positionT));

		if (Analyzer.getInstance().getActiveChannel() != null) updateHistoTabs(RebuildStep.A_HISTOSET);
	}

	public void disposeHisto() {
		for (AbstractChartWindow chartWindow : chartTabItems) {
			chartWindow.dispose();
		}
		chartTabItems.clear();
		for (HistoTableWindow tableWindow : tableTabItems) {
			tableWindow.dispose();
		}
		tableTabItems.clear();

		cleanup();
	}

	/**
	 * Rebuilds the contents of the histo windows.
	 * Does nothing if the histoActive setting is false.
	 */
	public synchronized void resetHisto() {
		DirectoryScanner.closeWatchDir();
		SupplementObjectFolder.checkAndCreate();
		SupplementObjectFolder.updateLogMirror();

		for (AbstractChartWindow c : chartTabItems) {
			resetWindowHeaderAndMeasuring(c);
		}
		volatileStatusMessage = null;
		histoSet.initialize();
		updateHistoTabs(RebuildStep.A_HISTOSET);
	}

	/**
	 * @return true if the trail recordset data are available
	 */
	public boolean hasRecords() {
		TrailRecordSet activeRecordSet = getHistoSet().getTrailRecordSet();
		return activeRecordSet != null ? activeRecordSet.size() > 0 : false;
	}

	/**
	 * Updates (redraws) the histo table if visible.
	 */
	public synchronized void updateHistoTableWindow(boolean forceClean) {
		if (displayTab != null && !(displayTab.getSelection() instanceof HistoTableWindow)) return;

		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				for (HistoTableWindow c : tableTabItems) {
					if (forceClean || !c.isRowTextAndTrailValid() || !c.isHeaderTextValid()) {
						c.setHeader();
					}
					TrailRecordSet trailRecordSet = histoSet.getTrailRecordSet();
					if (trailRecordSet != null) {
						int tableRowCount = trailRecordSet.getVisibleAndDisplayableRecordsForTable().size();
						if (settings.isDisplayTags()) {
							TrailDataTags dataTags = trailRecordSet.getDataTags();
							int channelNumber = Analyzer.getInstance().getActiveChannel().getNumber();
							dataTags.defineActiveDisplayTags(channelNumber);
							if (dataTags.getActiveDisplayTags(channelNumber) != null) {
								tableRowCount += dataTags.getActiveDisplayTags(channelNumber).size();
							}
						}
						c.setRowCount(tableRowCount);
					}
				}
			}
		});

	}

	/**
	 * Set the next graph into the window or alternatively restore to full vertical size.
	 * Does not support redrawing.
	 */
	public void scrollSummaryComposite() {
		if (isHistoChartWindowVisible()) {
			((AbstractChartWindow) displayTab.getSelection()).scrollComposite();
		}
	}

	/**
	 * update (redraw) any visible histo chart window from the histo recordset.
	 * @param redrawCurveSelector
	 */
	public void updateHistoChartWindow(boolean redrawCurveSelector) {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			CTabItem activeTab = displayTab.getSelection();
			if (!(activeTab instanceof AbstractChartWindow)) return;
			if (!((AbstractChartWindow) activeTab).isActiveCurveSelectorContextMenu()) {
				((AbstractChartWindow) activeTab).redrawGraphics(redrawCurveSelector);
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					CTabItem activeTab = displayTab.getSelection();
					if (!(activeTab instanceof AbstractChartWindow)) return;
					if (!((AbstractChartWindow) activeTab).isActiveCurveSelectorContextMenu()) {
						((AbstractChartWindow) activeTab).redrawGraphics(redrawCurveSelector);
					}
				}
			});
		}
	}

	public boolean isHistoChartWindowVisible() {
		return displayTab.getSelection() instanceof AbstractChartWindow;
	}

	public boolean isHistoTableWindowVisible() {
		return displayTab.getSelection() instanceof HistoTableWindow;
	}

	public boolean isHistoWindowVisible() {
		if (displayTab == null) return false;
		return displayTab.getSelection() instanceof AbstractChartWindow || displayTab.getSelection() instanceof HistoTableWindow;
	}

	/**
	 * Update any visible histo tab.
	 */
	public void updateHistoTabs() {
		updateHistoTabs(RebuildStep.B_HISTOVAULTS);
	}

	/**
	 * Minor update for any visible histo tab.
	 * @param createRecordSet true creates the recordset from the histo vaults; false uses the existing recordset
	 * @param rebuildTrails true refills the recordset and keeps the selector settings; false only rebuilds the UI
	 */
	public void updateHistoTabs(boolean createRecordSet, boolean rebuildTrails, boolean updateSelector) {
		updateHistoTabs(createRecordSet ? RebuildStep.C_TRAILRECORDSET : rebuildTrails ? RebuildStep.D_TRAIL_DATA : updateSelector ? RebuildStep.E_USER_INTERFACE : RebuildStep.G_SELECTOR_COMBO);
	}

	private void updateHistoTabs(RebuildStep rebuildStep) {
		if (Analyzer.getInstance().getActiveChannel() == null) return;

		if (Thread.currentThread().getId() == application.getThreadId()) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "initial size=", getTrailRecordSet() != null
					? getTrailRecordSet().getDisplayRecords().size() + "  " + getTrailRecordSet().getVisibleAndDisplayableRecords().size() : "0   0");
			if (isHistoWindowVisible()) {
				Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep), "rebuild4Screening"); //$NON-NLS-1$
				try {
					rebuilThread.start();
				} catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "rebuild size=", getTrailRecordSet() != null
					? getTrailRecordSet().getDisplayRecords().size() + "  " + getTrailRecordSet().getVisibleAndDisplayableRecords().size() : "0   0");
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (isHistoWindowVisible()) {
						Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep), "rebuild4Screening"); //$NON-NLS-1$
						try {
							rebuilThread.start();
						} catch (RuntimeException e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
			});
		}
	}

	public synchronized void rebuildHisto(RebuildStep rebuildStep) {
		boolean isRebuilt = false;
		try {
			isRebuilt = histoSet.rebuild4Screening(rebuildStep);

			if (isRebuilt || rebuildStep == RebuildStep.E_USER_INTERFACE || rebuildStep == RebuildStep.G_SELECTOR_COMBO) {
				if (histoSet.getTrailRecordSet() != null) {
					histoSet.getTrailRecordSet().setDisplayable();
					histoSet.getTrailRecordSet().updateVisibleAndDisplayableRecordsForTable();
				}
				log.fine(() -> String.format("updateHistoChartWindow %b - updateHistoTableWindow %b", (rebuildStep != RebuildStep.G_SELECTOR_COMBO), (rebuildStep.scopeOfWork >= RebuildStep.E_USER_INTERFACE.scopeOfWork)));
				updateHistoChartWindow(rebuildStep != RebuildStep.G_SELECTOR_COMBO);
				updateHistoTableWindow(rebuildStep.scopeOfWork >= RebuildStep.E_USER_INTERFACE.scopeOfWork);
			}
			histoSet.setRebuildStepInvisibleTabs(rebuildStep, isRebuilt);
		} catch (Exception e) {
			log.log(SEVERE, e.getMessage(), e);
			if (GDE.isWithUi()) application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + e.getMessage());
		}
	}

	/**
	 * Reset the window measurement pointer including table and header.
	 */
	private void resetWindowHeaderAndMeasuring(AbstractChartWindow tabItem) {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			tabItem.clearHeaderAndComment();
			tabItem.cleanMeasuring();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					tabItem.clearHeaderAndComment();
					tabItem.cleanMeasuring();
				}
			});
		}
	}

	/**
	 * Interactive curve measuring display.
	 */
	public void enableCurveSurvey(boolean enabled) {
		settings.setCurveSurvey(enabled);
	}

	/**
	 * Reduce visible measurements based on the curve selector.
	 */
	public void enablePartialDataTable(boolean enabled) {
		settings.setPartialDataTable(enabled);
	}

	public void updateVisibleTab(SelectionEvent evt) {
		if (isHistoChartWindowVisible()) {
			log.log(Level.FINER, "HistoChartWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(histoSet.getRebuildStepInvisibleTab()); // saves some time compared to HistoSet.RebuildStep.E_USER_INTERFACE
		} else if (isHistoTableWindowVisible()) {
			log.log(Level.FINER, "HistoTableWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(HistoSet.RebuildStep.E_USER_INTERFACE); // ensures rebuild after trails change or record selector change
		}
	}

	/**
	 * Redraw if visible window.
	 */
	public void updateGraphicsWindow(boolean refreshCurveSelector) {
		if ((displayTab.getSelection() instanceof AbstractChartWindow)) {
			((AbstractChartWindow) displayTab.getSelection()).redrawGraphics(refreshCurveSelector);
		}
	}

	/**
	 * Set the inner area background color.
	 */
	public void setInnerAreaBackground(Color innerAreaBackground) {
		settings.setObjectDescriptionInnerAreaBackground(innerAreaBackground);
		for (AbstractChartWindow c : chartTabItems) {
			c.setCurveAreaBackground(innerAreaBackground);
		}
	}

	/**
	 * Set the border color (for curve graphics the curve area border color, ...).
	 */
	public void setBorderColor(Color borderColor) {
		settings.setCurveGraphicsBorderColor(borderColor);
		for (AbstractChartWindow c : chartTabItems) {
			c.setCurveAreaBorderColor(borderColor);
		}
	}

	/**
	 * Set the surrounding area background color (for curve graphics, the area surrounding the curve area, ...).
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
		settings.setUtilitySurroundingBackground(surroundingBackground);
		for (AbstractChartWindow c : chartTabItems) {
			c.setSurroundingBackground(surroundingBackground);
		}
	}

	public void enableGraphicsHeader(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableGraphicsHeader(enabled);
		}
	}

	public void enableRecordSetComment(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableRecordSetComment(enabled);
		}
	}

	public void enableCurveSelector(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableCurveSelector(enabled);
		}
	}

	/**
	 * @return the canvasImage alias graphics window for the visible tab.
	 */
	public Optional<Image> getGraphicsPrintImage() {
		return isHistoChartWindowVisible() ? Optional.of(((AbstractChartWindow) displayTab.getSelection()).getGraphicsPrintImage()) : Optional.empty();
	}

	/**
	 * Get tabulator content as image.
	 */
	public Optional<Image> getContentAsImage() {
		Image graphicsImage = null;
		if (isHistoChartWindowVisible()) {
			graphicsImage = ((AbstractChartWindow) displayTab.getSelection()).getContentAsImage();
		} else if (isHistoTableWindowVisible()) {
			graphicsImage = ((HistoTableWindow) displayTab.getSelection()).getContentAsImage();
		}
		return Optional.ofNullable(graphicsImage);
	}

	/**
	 * return the first histo graphics window content as image
	 */
	public Image getHistoGraphicsContentAsImage() {
		return chartTabItems.stream().filter(c -> c instanceof HistoGraphicsWindow).findFirst().map(AbstractChartWindow::getContentAsImage).orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * return the first histo summary window content as image
	 */
	public Image getHistoSummaryContentAsImage() {
		return chartTabItems.stream().filter(c -> c instanceof HistoSummaryWindow).findFirst().map(AbstractChartWindow::getContentAsImage).orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * return the histo table window content as image
	 */
	public Image getHistoTableContentAsImage() {
		return tableTabItems.stream().findFirst().map(HistoTableWindow::getContentAsImage).orElseThrow(UnsupportedOperationException::new);
	}

	public HistoSet getHistoSet() {
		return histoSet;
	}

	@Nullable // i.e. if rebuild thread is not finished
	public TrailRecordSet getTrailRecordSet() {
		return histoSet.getTrailRecordSet();
	}

	public HistoSummaryWindow getHistoSummaryTabItem() {
		return (HistoSummaryWindow) chartTabItems.stream().filter(c -> c instanceof HistoSummaryWindow).findFirst().orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * @return a visible histo chart window>/br>
	 *         throws Exception if the window is not visible
	 */
	public AbstractChartWindow getActiveHistoChartTabItem() {
		return (AbstractChartWindow) this.displayTab.getSelection();
	}

	/**
	 * Set menu items for the history analysis.
	 */
	public void updateHistoMenuItems() {
		if (!hasRecords()) return;

		final boolean isSmartStatistics = getTrailRecordSet().isSmartStatistics();
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.application.getMenuBar().getSmartStatisticsItem().setSelection(isSmartStatistics);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					application.getMenuBar().getSmartStatisticsItem().setSelection(isSmartStatistics);
				}
			});
		}
	}

	public void cleanup() {
		DirectoryScanner.closeWatchDir();
	}

	/**
	 * The message has higher priority than standard status messages.
	 * It is used for the next {@value #VOLATILE_STATUS_CYCLES} redraws.
	 */
	public void setVolatileStatusMessage(String message, int swtColor) {
		if (volatileStatusMessage == null || !volatileStatusMessage.isIdentical(message, swtColor)) {
			volatileStatusMessage = new VolatileStatusMessage(message, swtColor, VOLATILE_STATUS_CYCLES);
			if (isHistoWindowVisible()) DataExplorer.getInstance().setStatusMessage(volatileStatusMessage.getTextLine(), volatileStatusMessage.getColor());
		}
	}

	public VolatileStatusMessage getVolatileStatusMessage() {
		return this.volatileStatusMessage;
	}

	/**
	 * Use this at redraw to put a potential volatile message into the status line.
	 */
	public void paintVolatileStatusMessage() {
		if (volatileStatusMessage != null && !volatileStatusMessage.isExpired()) {
			String consumedTextLine = volatileStatusMessage.getTextLine();
			DataExplorer.getInstance().setStatusMessage(consumedTextLine, volatileStatusMessage.getColor());
			if (volatileStatusMessage.isExpired()) {
				volatileStatusMessage = null; // heap garbage collection
				if (isHistoTableWindowVisible()) {
					// the table window does not paint or reset the status message
					DataExplorer.getInstance().setStatusMessage("");
				}
			} else {
				DataExplorer.getInstance().setStatusMessage(consumedTextLine, volatileStatusMessage.getColor());
			}
		}
	}
	
	/**
	 * update background/foreground color selector table header
	 */
	public void updateColorSchema() {
		for (AbstractChartWindow chartTabItem : this.chartTabItems) {
			if (chartTabItem instanceof HistoSummaryWindow) chartTabItem.updateColorSchema();
		}
	}

}