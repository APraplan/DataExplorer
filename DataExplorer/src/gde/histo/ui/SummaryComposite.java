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

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.IDevice;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.datasources.HistoSet;
import gde.histo.datasources.SourceFolders;
import gde.histo.exclusions.InclusionData;
import gde.histo.guard.Reminder;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordFormatter;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.histo.ui.data.SummarySpots;
import gde.histo.ui.data.SummarySpots.Density;
import gde.histo.ui.menu.AbstractTabAreaContextMenu.TabMenuOnDemand;
import gde.histo.ui.menu.ChartTabAreaContextMenu;
import gde.histo.ui.menu.ChartTabAreaContextMenu.SummaryWarning;
import gde.histo.utils.HistoCurveUtils;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.MathUtils;
import gde.utils.StringHelper;

/**
 * Curves for the histo summary window.
 * @author Thomas Eickert
 */
public final class SummaryComposite extends AbstractChartComposite {
	private static final String	$CLASS_NAME	= SummaryComposite.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	static final int						UNK_GAP			= 2;

	/**
	 * Record data for the life cycle of a summary composite drawing.
	 */
	public static final class SummaryLayout extends AbstractChartLayout {
		private final Logger				log									= Logger.getLogger(SummaryLayout.class.getName());

		private final TrailRecord		trailRecord;
		private final SummarySpots	summarySpots;

		// summary min/max only depend on the vault q0/q4 values; a synced record has synced min/max values in these fields
		Double											syncMax							= null;
		Double											syncMin							= null;

		private Reminder[]					warningMinMaxValues	= null;
		private double[]						scaleMinMaxValues		= null;
		private DecimalFormat				decimalFormat				= null;

		public SummaryLayout(TrailRecord trailRecord) {
			this.trailRecord = trailRecord;
			this.summarySpots = new SummarySpots(this);
		}

		public void clear() {
			warningMinMaxValues = null;
			scaleMinMaxValues = null;
			decimalFormat = null;
			resetSyncMinMax();
		}

		/**
		 * Determine and set the q0/q4 max/minValues for the summary window from this record.
		 * @param recencyLimit defines the number of most recent logs which contribute the min/max values instead of q0/q4
		 */
		public void setSyncMinMax(int recencyLimit) {
			double[] lowerUpper = trailRecord.defineExtrema();
			double[] recentMinMax = trailRecord.defineRecentMinMax(recencyLimit);
			if (lowerUpper.length == 0) {
				resetSyncMinMax();
			} else {
				setSyncMinMax(Math.min(lowerUpper[0], recentMinMax[0]), Math.max(lowerUpper[1], recentMinMax[1]));
			}
		}

		public DecimalFormat getDecimalFormat() {
			if (decimalFormat == null) {
				decimalFormat = defineDecimalFormat();
			}
			return decimalFormat;
		}

		private DecimalFormat defineDecimalFormat() {
			return TrailRecordFormatter.getDecimalFormat(getScaleMinMax());
		}

		public double[] getScaleMinMax() {
			if (scaleMinMaxValues == null) {
				scaleMinMaxValues = defineScaleMinMax();
			}
			return scaleMinMaxValues;
		}

		private double[] defineScaleMinMax() {
			log.finer(() -> "'" + trailRecord.getName() + "'  syncSummaryMin=" + getSyncMin() + " syncSummaryMax=" + getSyncMax());
			double minValue = getSyncMin();
			double maxValue = getSyncMax();
			double deltaValueDisplay = maxValue - minValue;
			if (Math.abs(deltaValueDisplay) < .0001) { // equal value disturbs the scaling algorithm
				maxValue = maxValue + maxValue / 20.;
				minValue = minValue - minValue / 20.;
			}
			double[] tmpMinMax = new double[] { MathUtils.floorStepwise(minValue, maxValue - minValue), //
					MathUtils.ceilStepwise(maxValue, maxValue - minValue) };
			log.finer(() -> "'" + trailRecord.getName() + "'  tmpMin=" + tmpMinMax[0] + " tmpMax=" + tmpMinMax[1]);
			return tmpMinMax;
		}

		public Reminder[] getMinMaxWarning() {
			if (warningMinMaxValues == null) {
				warningMinMaxValues = trailRecord.defineMinMaxWarning(Settings.getInstance().getReminderCount());
			}
			return warningMinMaxValues;
		}

		/**
		 * @return true if the record or the suite contains reasonable min max data
		 */
		public boolean hasReasonableMinMax() {
			return syncMin == null && syncMax == null || !HistoSet.fuzzyEquals(syncMin, syncMax);
		}

		public void setSyncMinMax(double newMin, double newMax) {
			syncMin = newMin;
			syncMax = newMax;
			log.finer(() -> trailRecord.getName() + " syncSummaryMin=" + newMin + " syncSummaryMax=" + newMax);
		}

		public void resetSyncMinMax() {
			syncMin = syncMax = null;
		}

		public boolean isSyncMinMaxDefined() {
			return syncMin != null && syncMax != null;
		}

		public double getSyncMax() {
			return syncMax != null ? syncMax : 0.;
		}

		public double getSyncMin() {
			return syncMin != null ? syncMin : 0.;
		}

		public SummarySpots getSummarySpots() {
			return summarySpots;
		}

		public TrailRecord getTrailRecord() {
			return this.trailRecord;
		}

		@Override
		public String toString() {
			return "SummaryLayout [trailRecordSize=" + this.trailRecord.size() + ", syncMax=" + this.syncMax + ", syncMin=" + this.syncMin + ", warningMinMaxValues=" + Arrays.toString(this.warningMinMaxValues) + "]";
		}

	}

	/**
	 * Comment caching.
	 * The comment text and tooltip remains active for a defined number of drawing actions.
	 */
	private static final class VolatileComment {
		private final IDevice				device				= Analyzer.getInstance().getActiveDevice();
		private final Channel				channel				= Analyzer.getInstance().getActiveChannel();
		private final String				objectKey			= Settings.getInstance().getActiveObjectKey();

		private final String				textLines;
		private final String				toolTip;
		private int									remainingAccessCounter;

		private VolatileComment(String textLines, String toolTip, int accessCounter) {
			this.textLines = textLines;
			this.toolTip = toolTip;
			this.remainingAccessCounter = accessCounter;
		}

		private String getTextLines() {
			if (--remainingAccessCounter < 0) throw new UnsupportedOperationException();
			return remainingAccessCounter >= 0 ? this.textLines : GDE.STRING_EMPTY;
		}

		private String getToolTip() {
			return remainingAccessCounter > 0 ? this.toolTip : GDE.STRING_EMPTY;
		}

		private boolean isExpired() {
			if (remainingAccessCounter > 0) {
				boolean isOtherChart = !device.equals(Analyzer.getInstance().getActiveDevice()) || !channel.equals(Analyzer.getInstance().getActiveChannel()) || !objectKey.equals(Settings.getInstance().getActiveObjectKey());
				return isOtherChart;
			} else
				return true;
		}

		@Override
		public String toString() {
			boolean isSameChart = device.equals(Analyzer.getInstance().getActiveDevice()) && channel.equals(Analyzer.getInstance().getActiveChannel()) && objectKey.equals(Settings.getInstance().getActiveObjectKey());
			return "VolatileComment [textLines=" + this.textLines + ", toolTip=" + this.toolTip + ", remainingAccessCounter=" + this.remainingAccessCounter + ", isSameChart=" + isSameChart + "]";
		}
	}

	private final AbstractChartData	chartData	= new AbstractChartData();

	private VolatileComment					volatileComment;

	SummaryComposite(SashForm useParent, CTabItem parentWindow) {
		super(useParent, parentWindow, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		// get the background colors
		this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
		this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
		this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new ChartTabAreaContextMenu();

		init();

		enableGraphicsScale(false);
	}

	private void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu);

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.finer(() -> "GraphicsComposite.controlResized() = " + evt); //$NON-NLS-1$
				Rectangle clientRect = getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				log.finer(() -> oldSize + " - " + size); //$NON-NLS-1$
				if (!oldSize.equals(size)) {
					log.fine(() -> "size changed, update " + oldSize + " - " + size); //$NON-NLS-1$ //$NON-NLS-2$
					oldSize = size;
					setComponentBounds();
					doRedrawGraphics();
				}
			}
		});
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				log.finer(() -> "GraphicsComposite.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_94.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.graphicsHeader = new Text(this, SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY);
			this.graphicsHeader.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 3, SWT.BOLD));
			this.graphicsHeader.setBackground(this.surroundingBackground);
			this.graphicsHeader.setMenu(this.popupmenu);
			this.graphicsHeader.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.finer(() -> "graphicsHeader.paintControl, event=" + evt); //$NON-NLS-1$
					SourceFolders sourceFolders = DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet().getSourceFolders();
					String headerText = sourceFolders != null //
							? sourceFolders.getTruncatedFileNamesCsv().replace(GDE.STRING_CSV_SEPARATOR, " | ") : "";
					String toolTipText = sourceFolders != null //
							? sourceFolders.getDecoratedPathsCsv().replace(GDE.CHAR_CSV_SEPARATOR, GDE.CHAR_NEW_LINE) : "";
					if (!headerText.equals(graphicsHeaderText)) {
						graphicsHeaderText = headerText;
						graphicsHeader.setText(headerText);
					}
					{ // getFullText
						int levelMax = Settings.getInstance().getSubDirectoryLevelMax();
						String levelsText = levelMax > 0 ? GDE.STRING_NEW_LINE + "+ " + levelMax + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT0870)
								: GDE.STRING_EMPTY;
						if (!toolTipText.isEmpty()) toolTipText = toolTipText + levelsText;
					}
					graphicsHeader.setToolTipText(toolTipText);
				}
			});
		}
		{
			this.graphicCanvas = new Canvas(this, SWT.NONE);
			this.graphicCanvas.setBackground(this.surroundingBackground);
			this.graphicCanvas.setMenu(this.popupmenu);
			this.graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseExit(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					SummaryComposite.this.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1 || evt.button == 3) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1 || evt.button == 3) {
						mouseUpAction(evt);
					}
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.finer(() -> "graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					// System.out.println("width = " + GraphicsComposite.this.getSize().x);
					try {
						drawAreaPaintControl(evt);
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
					}
				}
			});
		}
		{
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT | SWT.READ_ONLY);
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.recordSetComment.setBackground(this.surroundingBackground);
			this.recordSetComment.setMenu(this.popupmenu);
			this.recordSetComment.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.finer(() -> "recordSetComment.paintControl, event=" + evt); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * Draw the containing records.
	 * Called in case of an paint event (redraw).
	 * @param evt
	 */
	private void drawAreaPaintControl(PaintEvent evt) {
		log.finest(() -> "drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		long nanoTime = System.nanoTime();
		abstractDrawAreaPaintControl(evt.gc);
		log.time(() -> "drawTime=" + StringHelper.getFormatedTime("ss:SSS", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime)));
	}

	void mouseMoveAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0 && this.canvasImage != null) {
			Point point = checkCurveBounds(evt.x, evt.y);

			this.graphicCanvas.setCursor(this.application.getCursor());
			List<Integer> snappedIndices = getSnappedIndices(point);
			TrailRecord record = getDisplayRecord(point);
			if (point.x > 0 && point.x < curveAreaBounds.width && !snappedIndices.isEmpty()) {
				this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
				String text = TrailRecordSetFormatter.getFileNameLines(snappedIndices);
				if (snappedIndices.size() == 1) {
					TrailRecordFormatter formatter = new TrailRecordFormatter(record);
					ExtendedVault vault = trailRecordSet.getVault(snappedIndices.get(0));
					String outliers = record.getVaultOutliers(vault).mapToObj(formatter::getScaleValue).collect(Collectors.joining(GDE.STRING_BLANK_COLON_BLANK));
					String scraps = record.getVaultScraps(vault).mapToObj(formatter::getScaleValue).collect(Collectors.joining(GDE.STRING_BLANK_COLON_BLANK));
					if (!outliers.isEmpty()) text += "\n o " + outliers;
					if (!scraps.isEmpty()) text += "\n s " + scraps;
				}
				if (this.graphicCanvas.getToolTipText() == null || !(text.equals(this.graphicCanvas.getToolTipText()))) {
					log.log(Level.FINEST, "", text);
					this.graphicCanvas.setToolTipText(text);
				}
			} else {
				if (point.x == 0 && record != null && getChartData(record).getMinMaxWarning()[0] != null) { // left scale warnings
					String hintForClick = GDE.STRING_NEW_LINE + Messages.getString(MessageIds.GDE_MSGT0914);
					this.graphicCanvas.setToolTipText(new TrailRecordFormatter(record).defineFormattedMinWarning(getChartData(record)) + hintForClick);
				} else if (point.x == curveAreaBounds.width && record != null && getChartData(record).getMinMaxWarning()[1] != null) { // right
					String hintForClick = GDE.STRING_NEW_LINE + Messages.getString(MessageIds.GDE_MSGT0914);
					this.graphicCanvas.setToolTipText(new TrailRecordFormatter(record).defineFormattedMaxWarning(getChartData(record)) + hintForClick);
				} else {
					this.graphicCanvas.setToolTipText(null);
				}
			}

			if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
				// windowActor.processMouseDownMove(this.timeLine.getAdjacentTimestamp(point.x));
			} else
				windowActor.processMouseUpMove(point);
		}
	}

	/**
	 * @param point is the mouse coordinate relative and corrected to the curve area bounds
	 * @return the record identified by the mouse position
	 */
	@Nullable
	private TrailRecord getDisplayRecord(Point point) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return null; // graphics data have not been determined yet
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return null; // concurrent activity

		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		int xPos = point.x;
		int yPos = point.y; // todo - UNK_GAP;
		log.finest(() -> String.format("x=%d y=%d  stripXPos=%d  stripYPos=%d  stripHeight=%d", //
				point.x, point.y, xPos, yPos, stripHeight));
		if (yPos < 0 || yPos >= stripHeight * trailRecordSet.getDisplayRecords().size() - 1) // below or above the drawing area
			return null;

		int displayOrdinal = Math.min(yPos / stripHeight, trailRecordSet.getDisplayRecords().size() - 1);
		if (displayOrdinal < 0) return null; // mouse position does not point to a visible record

		TrailRecord record = trailRecordSet.getDisplayRecords().get(displayOrdinal);
		log.finest(() -> String.format("x=%d y=%d  displayOrdinal=%d  record=%s", //
				point.x, point.y, displayOrdinal, record.getName()));
		return record;
	}

	/**
	 * @param point is the mouse coordinate relative and restricted to the curve area bounds
	 * @return the recordset timestamp indices of the log data identified by the mouse position or an empty list
	 */
	private List<Integer> getSnappedIndices(Point point) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return new ArrayList<>(); // graphics data have not been determined yet
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return new ArrayList<>(); // concurrent activity

		TrailRecord record = getDisplayRecord(point);
		if (record == null || (!record.isVisible() && settings.isPartialDataTable())) return new ArrayList<>(); // mouse not pointing to a visible record

		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		int stripYPos = (point.y - UNK_GAP) % stripHeight;

		List<Integer> snappedIndexes = getChartData(record).getSummarySpots().getSnappedIndexes(point.x, stripYPos);
		log.log(FINER, "", Arrays.toString(snappedIndexes.toArray()));
		return snappedIndexes;
	}

	void mouseDownAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Point point = checkCurveBounds(evt.x, evt.y);

			TrailRecord record = getDisplayRecord(point);
			if (point.x == 0 && record != null && getChartData(record).getMinMaxWarning()[0] != null) { // left scale warnings
				changeChartAsPerWarning(record, 0);
			} else if (point.x == curveAreaBounds.width && record != null && getChartData(record).getMinMaxWarning()[1] != null) { // right
				changeChartAsPerWarning(record, 1);
			}

			if (evt.button == 1) {
				windowActor.processMouseDownAction(point);
			} else if (evt.button == 3) { // right button
				popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
				Path activeFolder = DirectoryScanner.getActiveFolder4Ui();
				popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), Arrays.stream(ExclusionActivity.getExcludedTrusses()).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
				InclusionData inclusionData = new InclusionData(activeFolder, Analyzer.getInstance().getDataAccess());
				String[] includedRecordNames = inclusionData.getIncludedRecordNames();
				SummaryWarning summaryWarning = new SummaryWarning(activeFolder, record != null ? record.getName() : GDE.STRING_EMPTY, includedRecordNames);
				popupmenu.setData(TabMenuOnDemand.SUMMARY_WARNING.name(), summaryWarning);

				List<Integer> snappedIndices = getSnappedIndices(point);
				if (isBeyondLeftBounds(evt.x) && record != null && getChartData(record).getMinMaxWarning()[0] != null) { // left scale warnings
					Reminder outlier = getChartData(record).getMinMaxWarning()[0];
					ExtendedVault vault = record.getParent().getVault(outlier.getIndices().get(0));
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), vault.getLoadLinkPath());
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), vault.getLoadFilePath());
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), vault.getLogRecordsetBaseName());
				} else if (isBeyondRightBounds(evt.x) && record != null && getChartData(record).getMinMaxWarning()[1] != null) { // right
					Reminder outlier = getChartData(record).getMinMaxWarning()[1];
					ExtendedVault vault = record.getParent().getVault(outlier.getIndices().get(0));
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), vault.getLoadLinkPath());
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), vault.getLoadFilePath());
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), vault.getLogRecordsetBaseName());
				} else if (snappedIndices.size() == 1) { // in the curve area over a single log
					Integer index = snappedIndices.get(0);
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), trailRecordSet.getDataTagText(index, DataTag.LINK_PATH));
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), trailRecordSet.getDataTagText(index, DataTag.FILE_PATH));
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), trailRecordSet.getDataTagText(index, DataTag.RECORDSET_BASE_NAME));
				} else {
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), GDE.STRING_EMPTY);
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
				}
			}
		}

	}

	/**
	 * @param actionType is 0 for a change after clicking on the left (min) side of the chart
	 */
	private void changeChartAsPerWarning(TrailRecord record, int actionType) {
		TrailRecordFormatter recordFormatter = new TrailRecordFormatter(record);
		volatileComment = new VolatileComment(recordFormatter.defineMinMaxWarningText(getChartData(record)), Messages.getString(MessageIds.GDE_MSGT0909),
				3);
		windowActor.setTrailVisible(record, getChartData(record).getMinMaxWarning()[actionType].getSelectIndex());
		windowActor.updateHistoTabs(false, false, false);
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Point point = checkCurveBounds(evt.x, evt.y);

			if (evt.button == 1) {
				windowActor.processMouseUpAction(point);
			}
		}
	}

	@Override
	protected void setFixedGraphicCanvas(Rectangle realBounds) {
		if (retrieveTrailRecordSet().isSmartStatistics()) {
			int heightWithScale = realBounds.height + getXScaleHeight() + AbstractChartComposite.DEFAULT_TOP_GAP;
			setFixedGraphicCanvas(realBounds.y - AbstractChartComposite.DEFAULT_TOP_GAP, heightWithScale);
		} else {
			setFixedGraphicCanvas(realBounds.y - AbstractChartComposite.DEFAULT_TOP_GAP, AbstractChartComposite.ZERO_CANVAS_HEIGHT);
		}
	}

	@Override
	protected void drawCurveArea(int dataScaleWidth) {
		canvasImageGC.setBackground(this.curveAreaBackground);
		canvasImageGC.fillRectangle(this.curveAreaBounds);
		canvasImageGC.setBackground(this.surroundingBackground);

		HistoCurveUtils.drawCurveAreaBorders(canvasImageGC, curveAreaBounds, curveAreaBorderColor);

		long startTime = new Date().getTime();
		if (fixedCanvasHeight > AbstractChartComposite.ZERO_CANVAS_HEIGHT) 
			drawTrailRecordSet(dataScaleWidth);
		log.fine(() -> "draw records time = " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startTime)));
	}

	@Override
	protected void defineLayoutParams() {
		// initialize early in order to avoid problems in mouse move events
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		this.chartData.clear();
		for (TrailRecord record : trailRecordSet.getValues()) {
			this.chartData.put(record.getName(), new SummaryLayout(record));
		}
		trailRecordSet.updateSyncSummaryScale(this.chartData);
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return; // concurrent activity

		Density density = Density.toDensity(curveAreaBounds.width, trailRecordSet.getTimeStepSize() == 0 ? 1 : trailRecordSet.getTimeStepSize());
		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		log.fine(() -> "curve area bounds = " + curveAreaBounds.toString());

		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			if (record.isVisible()) log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", //$NON-NLS-1$
					record.getName(), record.isVisible(), record.isDisplayable(), record.isScaleSynced(), record.isScaleVisible()));

			Rectangle drawStripBounds = new Rectangle(curveAreaBounds.x, curveAreaBounds.y + stripHeight * i + UNK_GAP, curveAreaBounds.width, stripHeight);
			log.finer(() -> record.getName() + "  x0=" + curveAreaBounds.x + " y0=" + drawStripBounds.y + " width=" + curveAreaBounds.width + " height=" + stripHeight);

			SummarySpots summarySpots = getChartData(record).getSummarySpots();
			summarySpots.initialize(drawStripBounds, density);
		}
	}

	/**
	 * Draw all the plots for all channel items.
	 * Support multiple plots for one single item.
	 */
	private void drawTrailRecordSet(int dataScaleWidth) {
		log.finer(() -> "trail record set summary");
		boolean isDrawScaleInRecordColor = settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = settings.isDrawNumbersInRecordColor();
		boolean isPartialDataTable = settings.isPartialDataTable();
		boolean isSummaryBoxVisible = settings.isSummaryBoxVisible();
		boolean isSummarySpreadVisible = settings.isSummarySpreadVisible();
		boolean isSummarySpotsVisible = settings.isSummarySpotsVisible();
		boolean isCurveSelector = windowActor.isCurveSelectorEnabled();
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return;

		InclusionData inclusionData = new InclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
		List<String> exclusiveNames = Arrays.asList(inclusionData.getIncludedRecordNames());

		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			SummaryLayout summary = getChartData(record);
			SummarySpots summarySpots = summary.getSummarySpots();

			HistoCurveUtils.drawChannelItemScale(summary, canvasImageGC, dataScaleWidth, isDrawScaleInRecordColor, isDrawNumbersInRecordColor);
			if (exclusiveNames.contains(record.getName()))
				HistoCurveUtils.drawChannelItemWarnMarker(summary, canvasImageGC, dataScaleWidth, isDrawNumbersInRecordColor);
			if (record.size() == 0) continue; // nothing to display

			if (record.isVisible() || !isPartialDataTable) {
				if (isSummarySpreadVisible) HistoCurveUtils.drawChannelItemSpread(summary.getSummarySpots(), canvasImageGC);
				if (!isCurveSelector && record.isVisible()) HistoCurveUtils.drawChannelItemText(summary, canvasImageGC, isDrawNameInRecordColor);
				if (isSummaryBoxVisible)
					HistoCurveUtils.drawChannelItemBoxplot(summary, canvasImageGC, dataScaleWidth, isDrawNumbersInRecordColor, isSpaceBelow(i));
				if (isSummarySpotsVisible) summarySpots.drawMarkers(canvasImageGC);
				summarySpots.drawRecentMarkers(canvasImageGC);
			}
			boolean isDefaultOrExclusiveWarning = exclusiveNames.isEmpty() || exclusiveNames.contains(record.getName());
			if (isDefaultOrExclusiveWarning && !HistoSet.isGpsCoordinates(record)) {
				HistoCurveUtils.drawChannelItemWarnings(summary, canvasImageGC, dataScaleWidth);
			}
			canvasImageGC.setBackground(this.surroundingBackground);
		}
	}

	/**
	 * @param displayOrdinal is the counter based on the record display sequence
	 * @return true if there is no record drawn below the current record
	 */
	private boolean isSpaceBelow(int displayOrdinal) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return true;

		int nextOrdinal = displayOrdinal + 1;
		if (nextOrdinal < trailRecordSet.getDisplayRecords().size()) {
			TrailRecord nextRecord = trailRecordSet.getDisplayRecords().get(nextOrdinal);
			return !nextRecord.isVisible() && settings.isPartialDataTable();
		} else
			return true;
	}

	@Override
	protected int[] defineNumberLeftRightScales() {
		return new int[] { 1, 1 };
	}

	@Override
	protected void setRecordSetCommentStandard() {
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		if (retrieveTrailRecordSet().isSmartStatistics()) {
			if (volatileComment == null || volatileComment.isExpired()) {
				this.recordSetComment.setText(windowActor.getHistoSet().getDirectoryScanStatistics());
				this.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0896));
			} else {
				this.recordSetComment.setText(volatileComment.getTextLines());
				this.recordSetComment.setToolTipText(volatileComment.getToolTip());
				if (volatileComment.isExpired()) volatileComment = null; // heap gc
			}
		} else {
			this.recordSetComment.setText("supported for smart statistics only");
			this.recordSetComment.setToolTipText("supported for smart statistics only");
		}
	}

	@Override
	public void setMeasuringActive(Measure measure) {
		measuring = new SummaryMeasuring(this, measure);
		if (this.canvasBounds == null) return; // fixed window size
		// draw full graph at first because the curve area might have changed (due to new new scales)
		doRedrawGraphics();

		if (this.canvasBounds.height == 0) return; // fixed window size
		//measuring.drawMeasuring(null);
	}

	public SummaryLayout getChartData(TrailRecord trailRecord) {
		return (SummaryLayout) this.chartData.get(trailRecord.getName());
	}
}
