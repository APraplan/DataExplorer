/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
 ****************************************************************************************/
package gde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import gde.Analyzer;
import gde.GDE;
import gde.TestAnalyzer;
import gde.config.ExportService;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CurveUtils;
import gde.utils.TimeLine;
import junit.framework.TestCase;

public class TestSuperClass extends TestCase {
	Logger rootLogger;
	static {
		Settings.getInstance();
		GDE.display = Display.getDefault();
		GDE.shell = new Shell(GDE.display);
	}

	final TestAnalyzer	analyzer		= (TestAnalyzer) Analyzer.getInstance();
	final DataExplorer	application	= DataExplorer.getInstance();
	final Channels			channels		= Channels.getInstance();
	final Settings			settings		= Settings.getInstance();
	final String				tmpDir			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) ? System.getProperty("java.io.tmpdir")
			: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;
	final String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
	final String tmpDir2 = this.tmpDir + "Write_2_OSD" + GDE.FILE_SEPARATOR;

	protected enum DataSource {
		SETTINGS {
			@Override
			Path getDataPath(Path subPath) {
				return Paths.get(Settings.getInstance().getDataFilePath()).resolve(subPath);
			}
		}, TESTDATA {
			@Override
			Path getDataPath(Path subPath) {
				String srcDataPath = getLoaderPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (srcDataPath.endsWith("bin/")) { // running inside eclipse
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				}
				else if (srcDataPath.indexOf("classes") > -1) { // ET running inside eclipse in Debug mode
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				}
				else {
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf("build")) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				}
				//return Paths.get(srcDataPath).resolve(subPath); Error because of leading slash: /C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer // this.dataPath = Paths.get(srcDataPath).resolve(subPath).toFile();
				return (new File(srcDataPath)).toPath().resolve(subPath);
			}
		}, INDIVIDUAL {
			@Override
			Path getDataPath(Path subPath) {
				return subPath;
			}
		};

		abstract Path getDataPath(Path subPath);
	};

	final TimeLine												timeLine					= new TimeLine();
	Rectangle															curveAreaBounds;
	int																		offSetX, offSetY;

	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	File																	dataPath;
	HashMap<String, String>								legacyDeviceNames	= new HashMap<String, String>(2);

	Handler																ch								= new ConsoleHandler();
	LogFormatter													lf								= new LogFormatter();

	Logger																logger1						= Logger.getLogger("gde.data.Record");
	Logger																logger2						= Logger.getLogger("gde.data.RecordSet");

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.rootLogger = Logger.getLogger("");

		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			this.rootLogger.removeHandler(handler);
		}
		this.rootLogger.setLevel(Level.WARNING);
		this.rootLogger.addHandler(this.ch);
		this.ch.setFormatter(this.lf);
		this.ch.setLevel(Level.FINER);

		// this.logger1.setLevel(Level.FINE);
		// this.logger1.setUseParentHandlers(true);
		// this.logger2.setLevel(Level.FINE);
		// this.logger2.setUseParentHandlers(true);

		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());

		this.initialize();

		// add this two renamed device plug-ins to the list of legacy devices
		this.legacyDeviceNames.put("GPSLogger", "GPS-Logger");
		this.legacyDeviceNames.put("QuadroControl", "QC-Copter");
		this.legacyDeviceNames.put("PichlerP60", "PichlerP60 50W");

		if (!new File(this.tmpDir1).exists() && !new File(this.tmpDir1).mkdirs())
			Logger.getLogger("gde.junit.TestSuperClass").log(Level.WARNING, "Failed creation of " + this.tmpDir1 );
		if (!new File(this.tmpDir2).exists() && !new File(this.tmpDir2).mkdirs())
			Logger.getLogger("gde.junit.TestSuperClass").log(Level.WARNING, "Failed creation of " + this.tmpDir2 );
	}

	/**
	 * goes through the existing device properties files and set active flagged
	 * devices into active devices list
	 *
	 * @throws FileNotFoundException
	 */
	public void initialize() throws FileNotFoundException {
		this.analyzer.setChannels(this.channels);

		this.settings.setPartialDataTable(false);
		this.settings.setTimeFormat("relativ");

		this.application.setHisto(true);
		setHistoSettings();

		//required extract all device properties and graphic template files for existing jar and exported services
		//lazy cleanup will occur during next regular DataExplorer startup
		for (String serviceName : this.settings.getDeviceServices().keySet()) {
			ExportService service = this.settings.getDeviceServices().get(serviceName);
			try {
				this.settings.extractDevicePropertiesAndTemplates(service.getJarFile(), serviceName);
				this.analyzer.getDeviceConfigurations().add(this.analyzer, serviceName, serviceName+GDE.FILE_ENDING_DOT_XML, false);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		File file = new File(Settings.getDevicesPath());
		if (!file.exists()) throw new FileNotFoundException(Settings.getDevicesPath());

		// wait until schema is setup
		this.settings.joinXsdThread();

		this.deviceConfigurations = this.analyzer.getDeviceConfigurations().getAllConfigurations();
		System.out.println("number of device configurations = " + this.deviceConfigurations.size());
	}

	/**
	 *
	 */
	private void setHistoSettings() {
		this.settings.setHistoActive(true); // should not have any influence on junit tests
		// the next lines only hold settings which do not control the GUI appearance
		this.settings.setSearchDataPathImports(true);
		this.settings.setChannelMix(false);
		this.settings.setSamplingTimespan_ms("2"); // this index corresponds to 1 sec
		this.settings.setIgnoreLogObjectKey(true);
		this.settings.setRetrospectMonths("120"); // this is the current maximum value
// this.settings.setZippedCache(false); // keep the users setting in order not to delete any user cache entries
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setSuppressMode(false);
		this.settings.setSubDirectoryLevelMax("5");
		this.settings.setCanonicalQuantiles(true);
		this.settings.setSymmetricToleranceInterval(true);
		this.settings.setOutlierToleranceSpread("9");
	}

	/**
	 * calculates the new class name for the device
	 */
	protected IDevice getInstanceOfDevice(DeviceConfiguration selectedActiveDeviceConfig) {
		IDevice newInst = null;
		try {
			newInst = selectedActiveDeviceConfig.defineInstanceOfDevice();
		}
		catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return newInst;
	}

	/**
	 * this will setup empty channels according the device properties file
	 * copied and modified from DeviceSelectionDialog.setupDataChannels();
	 *
	 * @param activeDevice
	 *          (IDevice is the abstract type)
	 */
	protected void setupDataChannels(IDevice activeDevice) {
		if (activeDevice == null) {
			this.channels.cleanup();
		} else {
			// cleanup existing channels and record sets
			this.analyzer.setActiveDevice(activeDevice);
			this.analyzer.getSettings().setActiveObjectKey(this.application.getObjectKey());
			this.channels.setupChannels(this.analyzer);
			this.channels.setActiveChannelNumber(this.analyzer.getActiveChannel().getNumber());
		}
	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * copied and modified from GraphicsComposite.drawCurves()
	 *
	 * @param recordSet
	 * @param maxX
	 * @param maxY
	 */
	protected void drawCurves(RecordSet recordSet, int maxX, int maxY) {
		// get the image and prepare GC
		Image curveArea = new Image(Display.getDefault(), maxX, maxY);
		GC gc = new GC(curveArea);
		Rectangle bounds = new Rectangle(0, 0, maxX, maxY);

		// prepare time scale
		double totalDisplayDeltaTime_ms = recordSet.get(0).getDrawTimeWidth_ms();
		int[] timeScale = this.timeLine.getScaleMaxTimeNumber(totalDisplayDeltaTime_ms);
		int maxTimeFormated = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];

		// calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (Record tmpRecord : recordSet.getRecordsSortedForDisplay()) {
			if (tmpRecord != null && tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
				// System.out.println("==>> " + recordKey + " isVisible = " +
				// tmpRecord.isVisible() + " isDisplayable = " +
				// tmpRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$
				// //$NON-NLS-3$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		// correct scales and scale position according compare set requirements
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = 1; // numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = 0; // numberCurvesRight > 0 && numberCurvesLeft
			// == 0 ? 1 : 0;
		}
		// System.out.println("nCurveLeft=" + numberCurvesLeft + ",
		// nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		// calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales,
		// numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width - time scale
		int height; // y coordinate - make modulo 10 ??
		int startTimeFormated, endTimeFormated;

		// calculate the horizontal space width to be used for the scales
		Point pt = gc.textExtent("-000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x / 5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = recordSet.isCompareSet() ? horizontalNumberExtend + horizontalGap : horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;

		// calculate the horizontal area available for plotting graphs
		int gapSide = 10; // free gap left or right side of the curves
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide / 2 : gapSide);// enable
		// a
		// small
		// gap
		// if no
		// axis
		// is
		// shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = 10; // free gap on top of the curves
		int gapBot = 3 * pt.y + 4; // space used for time scale text and scales
		// with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		// System.out.println("draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" +
		// xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height);
		// //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		// //$NON-NLS-6$
		// set offset values used for mouse measurement pointers
		this.offSetX = x0;
		this.offSetY = y0 - height;

		// draw curves for each active record
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		recordSet.setDrawAreaBounds(this.curveAreaBounds);
		// System.out.println("curve bounds = " + this.curveAreaBounds);
		// //$NON-NLS-1$

		// gc.setBackground(this.curveAreaBackground);
		gc.fillRectangle(this.curveAreaBounds);
		// gc.setBackground(this.surroundingBackground);

		// draw the time scale
		// System.out.println("average time step record 0 = " +
		// recordSet.getAverageTimeStep_ms());
		startTimeFormated = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTimeFormated = startTimeFormated + maxTimeFormated;
		// System.out.println("startTime = " + startTimeFormated + " detaTime_ms
		// = " + (int)totalDisplayDeltaTime_ms + " endTime = " +
		// endTimeFormated);
		this.timeLine.drawTimeLine(recordSet, gc, x0, y0 + 1, width, startTimeFormated, endTimeFormated, scaleFactor, timeFormat, (long) totalDisplayDeltaTime_ms, DataExplorer.getInstance().COLOR_BLACK);

		// draw draw area bounding
		// gc.setForeground(this.curveAreaBorderColor);

		gc.drawLine(x0 - 1, yMax - 1, xMax + 1, yMax - 1);
		gc.drawLine(x0 - 1, yMax - 1, x0 - 1, y0);
		gc.drawLine(xMax + 1, yMax - 1, xMax + 1, y0);

		// check for activated time grid
		if (recordSet.getTimeGridType() > 0) drawTimeGrid(recordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());

		// check for activated horizontal grid
		boolean isCurveGridEnabled = recordSet.getValueGridType() > 0;

		// draw each record using sorted record set names
		recordSet.updateSyncRecordScale();
		for (Record actualRecord : recordSet.getRecordsSortedForDisplay()) {
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (actualRecord.isScaleVisible()) CurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth, true, true, true, false);

			if (isCurveGridEnabled && actualRecord.getOrdinal() == recordSet.getValueGridRecordOrdinal()) // check
				// for
				// activated
				// horizontal
				// grid
				drawCurveGrid(recordSet, gc, this.offSetY, width, this.settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				// gc.drawRectangle(x0, y0-height, width, height);
				gc.setClipping(x0 - 1, y0 - height - 1, width + 2, height + 2);
				CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
				gc.setClipping(bounds);
			}
		}

		// draw start time for zoom mode or scope mode
		if (startTimeFormated != 0) {
			String strStartTime = Messages.getString(MessageIds.GDE_MSGT0255) + TimeLine.getFomatedTimeWithUnit(recordSet.getStartTime());
			Point point = gc.textExtent(strStartTime);
			int yPosition = (int) (y0 + pt.y * 2.5);
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_RED));
			gc.drawText(strStartTime, 10, yPosition - point.y / 2);
		}
		gc.dispose();
		curveArea.dispose();
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during
	 * drawing of time scale
	 *
	 * @param recordSet
	 * @param gc
	 *          the graphics context to be used
	 * @param height
	 * @param dash
	 *          to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x - bounds.x, 0, x - bounds.x, bounds.height - 1);
		}
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during
	 * daring specified curve scale
	 *
	 * @param recordSet
	 * @param gc
	 *          the graphics context to be used
	 * @param useOffsetY
	 *          the offset in vertical direction
	 * @param width
	 * @param dash
	 *          to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, int useOffSetY, int width, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getValueGridColor());
		// curveAreaGC.setLineStyle(recordSet.getHorizontalGridLineStyle());
		Vector<Integer> horizontalGridVector = recordSet.getValueGrid();
		for (int i = 0; i < horizontalGridVector.size() - 1; i += recordSet.getValueGridType()) {
			int y = horizontalGridVector.get(i);
			gc.drawLine(0, y - useOffSetY, width - 1, y - useOffSetY);
		}
	}

	/**
	 * ger the path where the class GDE gets loaded
	 *
	 * @return
	 */
	protected static String getLoaderPath() {
		return GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	protected File setDataPath() {
		boolean settingsPropertiesExist = new File(Settings.getSettingsFilePath()).exists();
		boolean undefinedDataPath = new File(this.settings.getDataFilePath()).getPath().equals(System.getProperty("user.home"));
		boolean isDataPathConfigured = !undefinedDataPath;

		if (settingsPropertiesExist && isDataPathConfigured) {
			this.dataPath = DataSource.SETTINGS.getDataPath(Paths.get("")).toFile();
		}
		else {
			this.dataPath = DataSource.TESTDATA.getDataPath(Paths.get("")).toFile();
		}

		this.settings.setDataFilePath(this.dataPath.getPath());
		System.out.println("this.dataPath = " + this.dataPath.getPath());
		return this.dataPath;
	}
}
