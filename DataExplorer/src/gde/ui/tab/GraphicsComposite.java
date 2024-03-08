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

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.CurveUtils;
import gde.utils.GraphicsUtils;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.MathUtils;
import gde.utils.StringHelper;
import gde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsComposite extends Composite {
	final static Logger log = Logger.getLogger(GraphicsComposite.class.getName());

	protected final static int		DEFAULT_TOP_GAP			= 10; // free gap on top of the curves
	protected final static int		DEFAULT_HEADER_GAP	= 5;
	protected final static int		DEFAULT_COMMENT_GAP	= 5;

	public enum GraphicsMode {
		RESET, ZOOM, MEASURE, MEASURE_DELTA, PAN, CUT_LEFT, CUT_RIGHT, SCOPE
	};

	final DataExplorer				application							= DataExplorer.getInstance();
	final Settings						settings								= Settings.getInstance();
	final Channels						channels								= Channels.getInstance();
	final TimeLine						timeLine								= new TimeLine();
	final SashForm						graphicSashForm;
	final GraphicsType				graphicsType;

	Menu											popupmenu;
	TabAreaContextMenu				contextMenu;
	Color											curveAreaBackground;
	Color											surroundingBackground;
	Color											curveAreaBorderColor;

	// drawing canvas
	Text											graphicsHeader;
	Text											recordSetComment;
	boolean										isRecordCommentChanged	= false;

	Canvas										graphicCanvas;
	int												headerHeight						= 0;
	int												headerGap								= 0;
	int												commentHeight						= 0;
	int												commentGap							= 0;
	String										graphicsHeaderText, recordSetCommentText;
	Point											oldSize									= new Point(0, 0);								// composite size - control resized

	// update graphics only area required
	RecordSet									oldActiveRecordSet			= null;
	int												oldChangeCounter				= 0;
	boolean										isFileCommentChanged		= false;

	HashMap<String, Integer>	leftSideScales					= new HashMap<String, Integer>();
	HashMap<String, Integer>	rightSideScales					= new HashMap<String, Integer>();
	int												oldScopeLevel						= 0;
	boolean										oldZoomLevel						= false;

	// mouse actions
	int												xDown										= 0;
	int												xUp											= 0;
	int												xLast										= 0;
	int												yDown										= 0;
	int												yUp											= 0;
	int												yLast										= 0;
	int												leftLast								= 0;
	int												topLast									= 0;
	int												rightLast								= 0;
	int												bottomLast							= 0;
	int												offSetX, offSetY;
	Rectangle									canvasBounds;
	Image											canvasImage;
	GC												canvasImageGC;
	Rectangle									curveAreaBounds					= new Rectangle(0, 0, 1, 1);

	GraphicsMode 							actualModeState;
	
	boolean										isLeftMouseMeasure			= false;
	boolean										isRightMouseMeasure			= false;
	int												xPosMeasure							= 0, yPosMeasure = 0;
	int												xPosDelta								= 0, yPosDelta = 0;

	boolean										isZoomMouse							= false;
	boolean										isResetZoomPosition			= false;
	boolean										isTransientZoom					= false;
	boolean										isTransientGesture			= false;
	boolean										isZoomX									= false;
	boolean										isZoomY									= false;
	int												leftZoom, topZoom, widthZoom, heightZoom;

	boolean										isPanMouse							= false;
	int												xDeltaPan								= 0;
	int												yDeltaPan								= 0;

	boolean										isLeftCutMode						= false;
	boolean										isRightCutMode					= false;
	int												xPosCut									= 0;

	boolean										isScopeMode							= false;

	GraphicsComposite(final SashForm useParent, GraphicsType useGraphicsType) {
		super(useParent, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);
		this.graphicSashForm = useParent;
		this.graphicsType = useGraphicsType;
		//get the background colors
		switch (this.graphicsType) {
		case COMPARE:
			this.curveAreaBackground = this.settings.getCompareCurveAreaBackground();
			this.surroundingBackground = this.settings.getCompareSurroundingBackground();
			this.curveAreaBorderColor = this.settings.getCurveCompareBorderColor();
			break;

		case UTIL:
			this.curveAreaBackground = this.settings.getUtilityCurveAreaBackground();
			this.surroundingBackground = this.settings.getUtilitySurroundingBackground();
			this.curveAreaBorderColor = this.settings.getUtilityCurvesBorderColor();
			break;

		default:
			this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
			this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
			this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();
			break;
		}

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();

		init();
	}

	void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu, this.graphicsType.toTabType());

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "GraphicsComposite.controlResized() = " + evt);
				Rectangle clientRect = GraphicsComposite.this.getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, GraphicsComposite.this.oldSize + " - " + size);
				if (!GraphicsComposite.this.oldSize.equals(size)) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "size changed, update " + GraphicsComposite.this.oldSize + " - " + size);
					GraphicsComposite.this.oldSize = size;

					setComponentBounds();
					doRedrawGraphics();
				}
			}
		});
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "GraphicsComposite.helpRequested " + evt); //$NON-NLS-1$
				switch (GraphicsComposite.this.graphicsType) {
				default:
				case NORMAL:
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case COMPARE:
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_91.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case UTIL:
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
			}
		});
		{
			this.graphicsHeader = new Text(this, SWT.SINGLE | SWT.CENTER);
			this.graphicsHeader.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 3, SWT.BOLD));
			this.graphicsHeader.setBackground(this.surroundingBackground);
			this.graphicsHeader.setMenu(this.popupmenu);
			this.graphicsHeader.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.helpRequested " + evt); //$NON-NLS-1$
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.graphicsHeader.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					//System.out.println("width = " + GraphicsComposite.this.getSize().x);
					if (GraphicsComposite.this.graphicsType == GraphicsType.UTIL) {
						if (GraphicsComposite.this.application.isWithUtilitySet()) {
							RecordSet utilitySet = GraphicsComposite.this.application.getUtilitySet();
							String tmpHeader = utilitySet.getRecordSetDescription();
							if (GraphicsComposite.this.graphicsHeaderText == null || !tmpHeader.equals(GraphicsComposite.this.graphicsHeaderText)) {
								GraphicsComposite.this.graphicsHeader.setText(GraphicsComposite.this.graphicsHeaderText = tmpHeader);
							}
						}
					}
					else {
						Channel activeChannel = GraphicsComposite.this.channels.getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = activeChannel.getActiveRecordSet();
							if (recordSet != null) {
								String tmpDescription = activeChannel.getFileDescription();
								if (tmpDescription.contains(":")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf(":"));
								}
								if (tmpDescription.contains(";")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf(";"));
								}
								if (tmpDescription.contains("\r")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf("\r"));
								}
								if (tmpDescription.contains("\n")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf("\n"));
								}
								String tmpHeader = tmpDescription + GDE.STRING_MESSAGE_CONCAT + recordSet.getName();
								if (GraphicsComposite.this.graphicsHeaderText == null || !tmpHeader.equals(GraphicsComposite.this.graphicsHeaderText)) {
									GraphicsComposite.this.graphicsHeader.setText(GraphicsComposite.this.graphicsHeaderText = tmpHeader);
								}
							}
						}
					}
				}
			});
			this.graphicsHeader.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.keyPressed , event=" + e); //$NON-NLS-1$
					GraphicsComposite.this.isFileCommentChanged = true;
				}
			});
			this.graphicsHeader.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.focusLost() , event=" + evt); //$NON-NLS-1$
					GraphicsComposite.this.isFileCommentChanged = false;
					setFileComment();
				}

				@Override
				public void focusGained(FocusEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.focusGained() , event=" + evt); //$NON-NLS-1$
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
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseExit(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					GraphicsComposite.this.graphicCanvas.setCursor(GraphicsComposite.this.application.getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseUpAction(evt);
					}
					if (GraphicsComposite.this.graphicsType == GraphicsType.NORMAL) {
						RecordSet recordSet =  Channels.getInstance().getActiveChannel().getActiveRecordSet();
						if (recordSet != null && recordSet.isAvgMedianMeasurementMode(recordSet.getRecordKeyMeasurement())) {
							String measureRecordKey = recordSet.getRecordKeyMeasurement();
							recordSet.isAvgMedianMeasurementMode(measureRecordKey);
							Record record = recordSet.get(measureRecordKey);
							GraphicsComposite.this.calculateAvgMedianStatusMessage(recordSet.getDevice(), record, record.getHorizontalPointIndexFromDisplayPoint(GraphicsComposite.this.xPosMeasure), record.getHorizontalPointIndexFromDisplayPoint(GraphicsComposite.this.xPosDelta));
						}
					}
				}
			});
			this.graphicCanvas.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.keyPressed() , event=" + e); //$NON-NLS-1$
					if (GraphicsComposite.this.isTransientZoom && !GraphicsComposite.this.isTransientGesture) {
						GraphicsComposite.this.isResetZoomPosition = false;
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = (GraphicsComposite.this.graphicsType == GraphicsType.NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet()
									: GraphicsComposite.this.application.getCompareSet();
							if (GraphicsComposite.this.canvasImage != null && recordSet != null) {

								if (e.keyCode == 'x') {
									//System.out.println("x-direction");
									GraphicsComposite.this.isZoomX = true;
									GraphicsComposite.this.isZoomY = false;
								}
								else if (e.keyCode == 'y') {
									//System.out.println("y-direction");
									GraphicsComposite.this.isZoomY = true;
									GraphicsComposite.this.isZoomX = false;
								}
								else if (e.keyCode == '+' || e.keyCode == 0x100002b) {
									//System.out.println("enlarge");

									float boundsRelation = 1.0f * GraphicsComposite.this.curveAreaBounds.width / GraphicsComposite.this.curveAreaBounds.height;
									Point point = new Point(GraphicsComposite.this.canvasBounds.width / 2, GraphicsComposite.this.canvasBounds.height / 2);
									float mouseRelationX = 1.0f * point.x / GraphicsComposite.this.curveAreaBounds.width * 2;
									float mouseRelationY = 1.0f * point.y / GraphicsComposite.this.curveAreaBounds.height * 2;
									//System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

									int xStart, xEnd, yMin, yMax;
									if (GraphicsComposite.this.isZoomX) {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
									}
									else if (GraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = GraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}
									else {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}

									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									if (xEnd - xStart > 5 && yMax - yMin > 5) {
										recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
										redrawGraphics();
									}
								}
								else if (e.keyCode == '-' || e.keyCode == 0x100002d) {
									//System.out.println("reduce");
									if (GraphicsComposite.this.isTransientZoom && !GraphicsComposite.this.isTransientGesture) {

										float boundsRelation = 1.0f * GraphicsComposite.this.curveAreaBounds.width / GraphicsComposite.this.curveAreaBounds.height;
										Point point = new Point(GraphicsComposite.this.canvasBounds.width / 2, GraphicsComposite.this.canvasBounds.height / 2);
										float mouseRelationX = 1.0f * point.x / GraphicsComposite.this.curveAreaBounds.width * 2;
										float mouseRelationY = 1.0f * point.y / GraphicsComposite.this.curveAreaBounds.height * 2;
										//System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

										int xStart, xEnd, yMin, yMax;
										if (GraphicsComposite.this.isZoomX) {
											xStart = (int) (-50 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
										}
										else if (GraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = GraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (-50 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
										}
										else {
											xStart = (int) (-50 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (-50 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
										}

										if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
										if (xEnd - xStart > 5 && yMax - yMin > 5) {
											recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
											redrawGraphics();
										}
									}
								}
								else if (e.keyCode == 0x1000001) {
									//System.out.println("move top direction");
									recordSet.shift(0, -5); // 10% each direction
									redrawGraphics(); //this.graphicCanvas.redraw();?
								}
								else if (e.keyCode == 0x1000002) {
									//System.out.println("move bottom direction");
									recordSet.shift(0, +5); // 10% each direction
									redrawGraphics(); //this.graphicCanvas.redraw();?
								}
								else if (e.keyCode == 0x1000003) {
									//System.out.println("move left direction");
									recordSet.shift(+5, 0); // 10% each direction
									redrawGraphics(); //this.graphicCanvas.redraw();?
								}
								else if (e.keyCode == 0x1000004) {
									//System.out.println("move right direction");
									recordSet.shift(-5, 0); // 10% each direction
									redrawGraphics(); //this.graphicCanvas.redraw();?
								}
								else {
									//System.out.println("x,y off");
									GraphicsComposite.this.isZoomX = GraphicsComposite.this.isZoomY = false;
								}
							}
						}
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.keyReleased() , event=" + e); //$NON-NLS-1$
					//System.out.println("x,y off");
					GraphicsComposite.this.isZoomX = GraphicsComposite.this.isZoomY = false;
				}
			});
			this.graphicCanvas.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseScrolled, event=" + evt); //$NON-NLS-1$
					if (GraphicsComposite.this.isTransientZoom && !GraphicsComposite.this.isTransientGesture) {
						GraphicsComposite.this.isResetZoomPosition = false;
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = (GraphicsComposite.this.graphicsType == GraphicsType.NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet()
									: GraphicsComposite.this.application.getCompareSet();
							if (GraphicsComposite.this.canvasImage != null && recordSet != null) {

								float boundsRelation = 1.0f * GraphicsComposite.this.curveAreaBounds.width / GraphicsComposite.this.curveAreaBounds.height;
								Point point = checkCurveBounds(evt.x, evt.y);
								float mouseRelationX = 1.0f * point.x / GraphicsComposite.this.curveAreaBounds.width * 2;
								float mouseRelationY = 1.0f * point.y / GraphicsComposite.this.curveAreaBounds.height * 2;
								//System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

								int xStart, xEnd, yMin, yMax;
								if (evt.count < 0) { //reduce
									if (GraphicsComposite.this.isZoomX) {
										xStart = (int) (-50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
									}
									else if (GraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = GraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (-50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
									}
									else {
										xStart = (int) (-50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (-50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
									}
								}
								else { //enlarge
									if (GraphicsComposite.this.isZoomX) {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
									}
									else if (GraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = GraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}
									else {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}
								}
								if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								if (xEnd - xStart > 5 && yMax - yMin > 5) {
									recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
									redrawGraphics();
								}
							}
						}
					}
				}
			});
			this.graphicCanvas.addGestureListener(new GestureListener() {
				@Override
				public void gesture(GestureEvent evt) {
					if (evt.detail == SWT.GESTURE_BEGIN) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "BEGIN = " + evt); //$NON-NLS-1$
						GraphicsComposite.this.isTransientGesture = true;
					}
					else if (evt.detail == SWT.GESTURE_MAGNIFY) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "MAGIFY = " + evt); //$NON-NLS-1$
						if (GraphicsComposite.this.isTransientGesture) {
							GraphicsComposite.this.isResetZoomPosition = false;
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet recordSet = (GraphicsComposite.this.graphicsType == GraphicsType.NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet()
										: GraphicsComposite.this.application.getCompareSet();
								if (GraphicsComposite.this.canvasImage != null && recordSet != null) {

									float boundsRelation = 1.0f * GraphicsComposite.this.curveAreaBounds.width / GraphicsComposite.this.curveAreaBounds.height;
									Point point = checkCurveBounds(evt.x, evt.y);
									float mouseRelationX = 1.0f * point.x / GraphicsComposite.this.curveAreaBounds.width * 2;
									float mouseRelationY = 1.0f * point.y / GraphicsComposite.this.curveAreaBounds.height * 2;
									//System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

									int xStart, xEnd, yMin, yMax;
									if (evt.magnification < 1) { //reduce
										if (GraphicsComposite.this.isZoomX) {
											xStart = (int) (-25 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 25 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
										}
										else if (GraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = GraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (-25 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 25 * mouseRelationY);
										}
										else {
											xStart = (int) (-25 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width + 25 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (-25 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height + 25 * mouseRelationY);
										}
									}
									else { //enlarge
										if (GraphicsComposite.this.isZoomX) {
											xStart = (int) (25 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 25 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = GraphicsComposite.this.curveAreaBounds.height - GraphicsComposite.this.curveAreaBounds.y;
										}
										else if (GraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = GraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (25 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 25 * mouseRelationY);
										}
										else {
											xStart = (int) (25 * boundsRelation * mouseRelationX);
											xEnd = (int) (GraphicsComposite.this.curveAreaBounds.width - 25 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (25 * (2 - mouseRelationY));
											yMax = (int) (GraphicsComposite.this.curveAreaBounds.height - 25 * mouseRelationY);
										}
									}
									if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									if (xEnd - xStart > 5 && yMax - yMin > 5) {
										recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
										redrawGraphics();
									}
								}
							}
						}
					}
					else if (evt.detail == SWT.GESTURE_PAN) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "PAN = " + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null && GraphicsComposite.this.isTransientGesture) {
							RecordSet recordSet = (GraphicsComposite.this.graphicsType == GraphicsType.NORMAL) ? activeChannel.getActiveRecordSet() : GraphicsComposite.this.application.getCompareSet();
							if (recordSet != null && GraphicsComposite.this.canvasImage != null) {
								recordSet.shift(evt.xDirection, -1 * evt.yDirection); // 10% each direction
								redrawGraphics(); //this.graphicCanvas.redraw();?
							}
						}
					}
					else if (evt.detail == SWT.GESTURE_END) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "END = " + evt); //$NON-NLS-1$
						GraphicsComposite.this.isTransientGesture = false;
					}
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					//System.out.println("width = " + GraphicsComposite.this.getSize().x);
					try {
						drawAreaPaintControl(evt);
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage());
					}
				}
			});
		}
		{
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT);
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			this.recordSetComment.setBackground(this.surroundingBackground);
			this.recordSetComment.setMenu(this.popupmenu);
			this.recordSetComment.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetComment.paintControl, event=" + evt); //$NON-NLS-1$
					if (GraphicsComposite.this.channels.getActiveChannel() != null) {
						RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
						if (recordSet != null) {
							String tmpDescription = recordSet.getRecordSetDescription();
							if (GraphicsComposite.this.recordSetCommentText == null || !tmpDescription.equals(GraphicsComposite.this.recordSetCommentText)) {
								GraphicsComposite.this.recordSetComment.setText(GraphicsComposite.this.recordSetCommentText = tmpDescription);
							}

						}
					}
				}
			});

			this.recordSetComment.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetCommentText.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_11.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.recordSetComment.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "recordSetComment.keyPressed() , event=" + e); //$NON-NLS-1$
					GraphicsComposite.this.isRecordCommentChanged = true;
				}
			});
			this.recordSetComment.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "recordSetComment.focusLost() , event=" + evt); //$NON-NLS-1$
					updateRecordSetComment();
				}

				@Override
				public void focusGained(FocusEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "recordSetComment.focusGained() , event=" + evt); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records
	 * @param evt
	 */
	void drawAreaPaintControl(PaintEvent evt) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$

		//get gc for drawing operations
		evt.gc.drawImage(this.canvasImage, 0, 0);

		RecordSet recordSet = null;
		switch (this.graphicsType) {
		case COMPARE:
			if (this.application.isWithCompareSet()) {
				recordSet = this.application.getCompareSet();
			}
			break;

		case UTIL:
			if (this.application.isWithUtilitySet()) {
				recordSet = this.application.getUtilitySet();
			}
			break;

		default: // TYPE_NORMAL
			if (this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getActiveRecordSet() != null) {
				recordSet = this.channels.getActiveChannel().getActiveRecordSet();
			}
			break;
		}
		if (recordSet != null && recordSet.realSize() > 0) {

			if (recordSet.isMeasurementMode(recordSet.getRecordKeyMeasurement()) || recordSet.isDeltaMeasurementMode(recordSet.getRecordKeyMeasurement()) || recordSet.isAvgMedianMeasurementMode(recordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(evt.gc, recordSet, GraphicsMode.MEASURE, this.xPosMeasure != 0);
			}
			else if (this.isLeftCutMode) {
				drawCutPointer(evt.gc, GraphicsMode.CUT_LEFT, true, false);
			}
			else if (this.isRightCutMode) {
				drawCutPointer(evt.gc, GraphicsMode.CUT_RIGHT, false, true);
			}
			else if (this.isZoomMouse && recordSet.isZoomMode() && this.isResetZoomPosition) {
				drawZoomBounds(evt.gc);
			}
		}

	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * @param recordSet the record set to be drawn
	 * @param bounds the bounds where the curves and scales are drawn
	 * @param gc the graphics context to be used for the graphics operations
	 */
	private void drawCurves(RecordSet recordSet, Rectangle bounds, GC gc) {
		// prime the record set regarding scope mode and/or zoom mode
		if (this.isScopeMode) {
			int offset = recordSet.get(0).realSize() - recordSet.getScopeModeSize();
			if (offset < 1) {
				recordSet.setScopeModeOffset(0);
				recordSet.setScopeMode(false);
			}
			else {
				recordSet.setScopeModeOffset(offset);
				recordSet.setScopeMode(true);
			}
		}

		//prepare time scale
		double totalDisplayDeltaTime_ms = recordSet.get(0).getDrawTimeWidth_ms();
		int[] timeScale = this.timeLine.getScaleMaxTimeNumber(totalDisplayDeltaTime_ms);
		int maxTimeFormated = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];

		//calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (Record tmpRecord : recordSet.getRecordsSortedForDisplay()) {
			if (tmpRecord != null && tmpRecord.isScaleVisible()) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "==>> " + tmpRecord.getName() + " isScaleVisible = " + tmpRecord.isScaleVisible()); //$NON-NLS-1$ //$NON-NLS-2$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		//correct scales and scale position according compare set requirements
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = 1; //numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = 0; //numberCurvesRight > 0 && numberCurvesLeft == 0 ? 1 : 0;
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		//calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width	- time scale
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
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = DEFAULT_TOP_GAP; // free gap on top of the curves
		int gapBot = 3 * pt.y; // space used for time scale text and scales with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		// set offset values used for mouse measurement pointers
		this.offSetX = x0;
		this.offSetY = y0 - height;

		// draw curves for each active record
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		recordSet.setDrawAreaBounds(this.curveAreaBounds);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$

		gc.setBackground(this.curveAreaBackground);
		gc.fillRectangle(this.curveAreaBounds);
		gc.setBackground(this.surroundingBackground);

		//draw the time scale
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "average time step record 0 = " + recordSet.getAverageTimeStep_ms());
		startTimeFormated = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTimeFormated = startTimeFormated + maxTimeFormated;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "startTime = " + startTimeFormated + " detaTime_ms = " + (long) totalDisplayDeltaTime_ms + " endTime = " + endTimeFormated);
		this.timeLine.drawTimeLine(recordSet, gc, x0, y0 + 1, width, startTimeFormated, endTimeFormated, scaleFactor, timeFormat, (long) totalDisplayDeltaTime_ms, this.application.COLOR_BLACK);

		// draw draw area bounding
		gc.setForeground(this.curveAreaBorderColor);

		gc.drawLine(x0 - 1, yMax - 1, xMax + 1, yMax - 1);
		gc.drawLine(x0 - 1, yMax - 1, x0 - 1, y0);
		gc.drawLine(xMax + 1, yMax - 1, xMax + 1, y0);

		// check for activated time grid
		if (recordSet.getTimeGridType() > 0) drawTimeGrid(recordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());

		// check for activated horizontal grid
		boolean isCurveGridEnabled = recordSet.getValueGridType() > 0;

		// draw each record using sorted record set names
		long startTime = new Date().getTime();
		boolean isDrawScaleInRecordColor = this.settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = this.settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = this.settings.isDrawNumbersInRecordColor();
		
		boolean isDraw10TicksPerRecord = this.settings.isDraw10TicksPerRecord();
		//only change to other record if actual grid record is not visible to guarantee visible grid while switching
		if (isDraw10TicksPerRecord && recordSet.getVisibleAndDisplayableRecords().size() > 0 
				&& (recordSet.getValueGridRecordOrdinal() >= recordSet.size() || recordSet.getValueGridRecordOrdinal() == -1 || !recordSet.get(recordSet.getValueGridRecordOrdinal()).isScaleVisible())) 
			recordSet.setValueGridRecordOrdinal(recordSet.getVisibleAndDisplayableRecords().firstElement().getOrdinal());

		recordSet.syncScaleOfSyncableRecords();
		recordSet.updateSyncRecordScale();
		for (Record actualRecord : recordSet.getRecordsSortedForDisplay()) {
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (log.isLoggable(Level.FINE) && isActualRecordEnabled) log.log(Level.FINE, "drawing record = " + actualRecord.getName() + " isVisibel=" + actualRecord.isVisible() + " isDisplayable=" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					+ actualRecord.isDisplayable() + " isScaleSynced=" + actualRecord.isScaleSynced() + " isScaleVisible=" + actualRecord.isScaleVisible());
			
			if (actualRecord.isScaleVisible()) {
				CurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor, isDraw10TicksPerRecord);

				if (isCurveGridEnabled && actualRecord.getOrdinal() == recordSet.getValueGridRecordOrdinal()) // check for activated horizontal grid
					drawCurveGrid(recordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());
			}
			
			if (isActualRecordEnabled) {
				//gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				//gc.drawRectangle(x0, y0-height, width, height);
				gc.setClipping(x0 - 1, y0 - height - 1, width + 2, height + 2);
				CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
				gc.setClipping(this.canvasBounds);
			}
		}

		// draw start time for zoom mode or scope mode
		if (startTimeFormated != 0) {
			String strStartTime = Messages.getString(MessageIds.GDE_MSGT0255) + TimeLine.getFomatedTimeWithUnit(recordSet.getStartTime());
			Point point = gc.textExtent(strStartTime);
			int yPosition = (int) (y0 + pt.y * 2.5);
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_RED));
			gc.drawText(strStartTime, 10, yPosition - point.y / 2);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, strStartTime);
		}
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getValueGridColor());

		Vector<Integer> horizontalGridVector = recordSet.getValueGrid();
		for (int i = 0; i < horizontalGridVector.size(); i += recordSet.getValueGridType()) {
			int y = horizontalGridVector.get(i);
			if (y > bounds.y && y < (bounds.y + bounds.height)) gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x, bounds.y, x, bounds.y + bounds.height);
		}
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doRedrawGraphics();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * updates the graphics canvas, while repeatable redraw calls it optimized to the required area
	 */
	synchronized void doRedrawGraphics() {
		this.graphicsHeader.notifyListeners(SWT.Paint, new Event());

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "this.graphicCanvas.redraw(); // do full update where required");
		
		// Get the canvas and its dimensions
		this.canvasBounds = GraphicsComposite.this.graphicCanvas.getClientArea();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "canvas size = " + GraphicsComposite.this.canvasBounds); //$NON-NLS-1$
		if (this.canvasImage != null) GraphicsComposite.this.canvasImage.dispose();
		try {
			if (this.canvasBounds.width > 0 && this.canvasBounds.height > 0) {
				this.canvasImage = new Image(GDE.display, GraphicsComposite.this.canvasBounds);
				this.canvasImageGC = new GC(this.canvasImage); //SWTResourceManager.getGC(this.canvasImage);
				this.canvasImageGC.setBackground(this.surroundingBackground);
				this.canvasImageGC.fillRectangle(this.canvasBounds);
				this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));

				RecordSet recordSet = null;
				switch (this.graphicsType) {
				case COMPARE:
					if (this.application.isWithCompareSet()) {
						recordSet = this.application.getCompareSet();
					}
					break;
	
				case UTIL:
					if (this.application.isWithUtilitySet()) {
						recordSet = this.application.getUtilitySet();
					}
					break;
	
				default: // TYPE_NORMAL
					if (this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getActiveRecordSet() != null) {
						recordSet = this.channels.getActiveChannel().getActiveRecordSet();
					}
					break;
				}
				if (recordSet != null && recordSet.realSize() > 0) {
					drawCurves(recordSet, this.canvasBounds, this.canvasImageGC);
					//changed curve selection may change the scale end values
					recordSet.syncScaleOfSyncableRecords();
				}
				this.canvasImageGC.dispose();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		this.graphicCanvas.redraw(); // do full update where required
		
		this.recordSetComment.redraw();
	}

	public void notifySelected() {
		this.recordSetComment.notifyListeners(SWT.FocusOut, new Event());
	}

	/**
	 * draw the start pointer for measurement modes
	 * @param recordSet
	 * @param mode
	 * @param isRefresh
	 */
	public void drawMeasurePointer(GC canvasGC, RecordSet recordSet, GraphicsMode mode, boolean isRefresh) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isRefresh = " + isRefresh);
		if (mode != this.actualModeState)
			this.setModeState(mode); // cleans old pointer if required

		String measureRecordKey = recordSet.getRecordKeyMeasurement();
		if (canvasGC != null) {
			Record record = recordSet.get(measureRecordKey);
			// set the gc properties
			canvasGC.setLineWidth(1);
			canvasGC.setLineStyle(SWT.LINE_DASH);
			canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			IDevice actualDevice = record.getDevice();
			if (recordSet.isMeasurementMode(measureRecordKey)) {
				// initialize measure position if not in refresh 
				this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
				this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "initial xPosMeasure = " + this.xPosMeasure + " yPosMeasure = " + this.yPosMeasure); //$NON-NLS-1$ //$NON-NLS-2$

				drawVerticalLine(canvasGC, this.xPosMeasure, 0, this.curveAreaBounds.height);
				drawHorizontalLine(canvasGC, this.yPosMeasure, 0, this.curveAreaBounds.width);

				this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
				int indexPosMeasure = record.getHorizontalPointIndexFromDisplayPoint(this.xPosMeasure);
				this.calculateMeasurementStatusMessage(actualDevice, record, indexPosMeasure);
			}
			else if (recordSet.isDeltaMeasurementMode(measureRecordKey) || recordSet.isAvgMedianMeasurementMode(measureRecordKey)) {
				this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
				this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);

				// measure position
				drawVerticalLine(canvasGC, this.xPosMeasure, 0, this.curveAreaBounds.height);
				drawHorizontalLine(canvasGC, this.yPosMeasure, 0, this.curveAreaBounds.width);

				// delta position
				this.xPosDelta = isRefresh ? this.xPosDelta : this.curveAreaBounds.width / 3 * 2;
				this.yPosDelta = record.getVerticalDisplayPointValue(this.xPosDelta);

				canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
				drawVerticalLine(canvasGC, this.xPosDelta, 0, this.curveAreaBounds.height);
				drawHorizontalLine(canvasGC, this.yPosDelta, 0, this.curveAreaBounds.width);

				drawConnectingLine(canvasGC, this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);

				canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

				int indexPosMeasure = record.getHorizontalPointIndexFromDisplayPoint(this.xPosMeasure);
				int indexPosDelta = record.getHorizontalPointIndexFromDisplayPoint(this.xPosDelta);

				if (this.graphicsType == GraphicsType.NORMAL && !recordSet.isAvgMedianMeasurementMode(measureRecordKey)) {
					if (record.getDevice().getAtlitudeTripSpeedOrdinals().length == 3 //device must make sure required measurements available
							&& record.getOrdinal() == record.getDevice().getAtlitudeTripSpeedOrdinals()[0]) { // returned first ordinal match altitude 								
						this.calculateSinkAndGlideRatioStatusMessage(actualDevice, record, indexPosMeasure, indexPosDelta);
					}
					else {
						this.calculateDeltaValueStatusMessage(actualDevice, record, indexPosMeasure, indexPosDelta);
					} 
				}
			} 
		}
	}

	/**
	 * draw the zoom bounds rectangle
	 * @param canvasGC paint event canvas GC
	 */
	private void drawZoomBounds(GC canvasGC) {
		canvasGC.setLineWidth(1);
		canvasGC.setLineStyle(SWT.LINE_DASH);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "draw  left = " + (leftZoom - this.offSetX) + " top = " + (topZoom - this.offSetY) + " width = " + widthZoom + " height = " + heightZoom); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		canvasGC.drawRectangle(this.leftZoom, this.topZoom, this.widthZoom, this.heightZoom);
	}
	
	/**
	 * draws horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top
	 * for performance reason specify line width, line style and line color outside
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 */
	private void drawVerticalLine(GC canvasGC, int posFromLeft, int posFromTop, int length) {
		canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
	}

	/**
	 * draws vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top
	 * for performance reason specify line width, line style and line color outside
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 */
	private void drawHorizontalLine(GC canvasGC, int posFromTop, int posFromLeft, int length) {
		canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
	}

	/**
	 * draws line as defined relative to curve draw area, where there is an offset from left and an offset from top
	 * for performance reason specify line width, line style and line color outside
	 * @param posFromTop1
	 * @param posFromLeft1
	 * @param posFromTop2
	 * @param posFromLeft2
	 */
	private void drawConnectingLine(GC canvasGC, int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, int swtColor) {
		canvasGC.setForeground(SWTResourceManager.getColor(swtColor));
		canvasGC.setLineStyle(SWT.LINE_SOLID);
		canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
	}

	/**
	 * erase a vertical line by re-drawing the curve area image
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	void eraseVerticalLine(GC canvasGC, int posFromLeft, int posFromTop, int length, int lineWidth) {
		canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	void eraseHorizontalLine(GC canvasGC, int posFromTop, int posFromLeft, int length, int lineWidth) {
		canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth);
	}

	/**
	 * clean connecting line by re-drawing the untouched curve area image of this area
	 */
	void cleanConnectingLineObsoleteRectangle(GC canvasGC) {
		this.leftLast = this.leftLast == 0 ? this.xPosMeasure : this.leftLast;
		int left = this.xPosMeasure <= this.xPosDelta ? this.leftLast < this.xPosMeasure ? this.leftLast : this.xPosMeasure : this.leftLast < this.xPosDelta ? this.leftLast : this.xPosDelta;

		this.topLast = this.topLast == 0 ? this.yPosDelta : this.topLast;
		int top = this.yPosDelta <= this.yPosMeasure ? this.topLast < this.yPosDelta ? this.topLast : this.yPosDelta : this.topLast < this.yPosMeasure ? this.topLast : this.yPosMeasure;

		this.rightLast = this.rightLast == 0 ? this.xPosDelta - left : this.rightLast;
		int width = this.xPosDelta >= this.xPosMeasure ? this.rightLast > this.xPosDelta ? this.rightLast - left : this.xPosDelta - left
				: this.rightLast > this.xPosMeasure ? this.rightLast - left : this.xPosMeasure - left;

		this.bottomLast = this.bottomLast == 0 ? this.yPosMeasure - top : this.bottomLast;
		int height = this.yPosMeasure >= this.yPosDelta ? this.bottomLast > this.yPosMeasure ? this.bottomLast - top : this.yPosMeasure - top
				: this.bottomLast > this.yPosDelta ? this.bottomLast - top : this.yPosDelta - top;

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			canvasGC.drawImage(this.canvasImage, left + this.offSetX, top + this.offSetY, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}

		this.leftLast = this.xPosMeasure <= this.xPosDelta ? this.xPosMeasure : this.xPosDelta;
		this.topLast = this.yPosDelta <= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		this.rightLast = this.xPosDelta >= this.xPosMeasure ? this.xPosDelta : this.xPosMeasure;
		this.bottomLast = this.yPosDelta >= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * erase connecting line by re-drawing the curve area image
	 * @param posFromLeft1
	 * @param posFromTop1
	 * @param posFromLeft2
	 * @param posFromTop2
	 */
	void eraseConnectingLine(GC canvasGC, int left, int top, int width, int height) {
		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			canvasGC.drawImage(this.canvasImage, left, top, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer() {
		try {
			if (this.recordSetCommentText != null) {
				this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
				this.recordSetComment.setText(this.recordSetCommentText);
			}
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			redrawGraphics();
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * draw the cut pointer for cut modes
	 * @param canvasGC canvas paint event GC
	 * @param mode
	 * @param leftEnabled
	 * @param rightEnabled
	 */
	public void drawCutPointer(GC canvasGC, GraphicsMode mode, boolean leftEnabled, boolean rightEnabled) {
		if (mode != actualModeState)
			this.setModeState(mode); // cleans old pointer if required

		// allow only get the record set to work with
		boolean isGraphicsWindow = this.graphicsType == GraphicsType.NORMAL;
		if (isGraphicsWindow && canvasGC != null) {
			// set the gc properties
			canvasGC.setLineWidth(1);
			canvasGC.setLineStyle(SWT.LINE_SOLID);
			canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

			if (leftEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0258));
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 1 / 4;
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "this.xPosCut = " + this.xPosCut);
				canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				canvasGC.fillRectangle(0 + this.offSetX, 0 + this.offSetY, this.xPosCut, this.curveAreaBounds.height);
				canvasGC.setAdvanced(false);
				drawVerticalLine(canvasGC, this.xPosCut, 0, this.curveAreaBounds.height);
			}
			else if (rightEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0259));
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 3 / 4;
				canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				canvasGC.fillRectangle(this.xPosCut + this.offSetX, 0 + this.offSetY, this.curveAreaBounds.width - this.xPosCut, this.curveAreaBounds.height);
				canvasGC.setAdvanced(false);
				drawVerticalLine(canvasGC, this.xPosCut, 0, this.curveAreaBounds.height);
			}
			else {
				cleanCutPointer(canvasGC);
			}
		}
	}

	/**
	 * clean cutting edge pointer
	 */
	public void cleanCutPointer(GC canvasGC) {
		this.application.setStatusMessage(" "); //$NON-NLS-1$
		eraseVerticalLine(canvasGC, this.xPosCut, 0, this.curveAreaBounds.height, 2);
	}

	/**
	 * switch graphics window mouse mode
	 * @param mode MODE_RESET, MODE_ZOOM, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(GraphicsMode mode) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "GraphicsMode = " + mode);
		if (this.isZoomMouse || this.isLeftMouseMeasure || this.isRightMouseMeasure || this.isPanMouse ||	this.isScopeMode)
			this.cleanMeasurementPointer();
		switch (mode) {
		case ZOOM:
			this.isZoomMouse = true;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case MEASURE:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case MEASURE_DELTA:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case PAN:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = true;
			this.isScopeMode = false;
			break;
		case CUT_LEFT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = true;
			this.isRightCutMode = false;
			this.isScopeMode = false;
			break;
		case CUT_RIGHT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = true;
			this.isScopeMode = false;
			break;
		case SCOPE:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = false;
			this.isScopeMode = true;
			break;
		case RESET:
		default:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = false;
			this.isScopeMode = false;
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			this.xPosCut = -1;
			this.xLast = 0;
			this.yLast = 0;
			this.leftLast = 0;
			this.topLast = 0;
			this.rightLast = 0;
			this.bottomLast = 0;
			updatePanMenueButton();
			this.application.getMenuToolBar().resetZoomToolBar();
			break;
		}
		this.actualModeState = mode;
		this.xPosMeasure = this.xPosDelta = 0;
		this.graphicCanvas.redraw();
	}

	/**
	 *
	 */
	private void updatePanMenueButton() {
		this.application.getMenuBar().enablePanButton(this.isZoomMouse || this.isPanMouse);
		this.application.getMenuToolBar().enablePanButton(this.isZoomMouse || this.isPanMouse);
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
			log.log(Level.FINER, "in  offSetX = " + offSetX + " offSetY = " + offSetY);
		}
		int tmpxPos = xPos - this.offSetX;
		int tmpyPos = yPos - this.offSetY;
		int minX = 0;
		int maxX = this.curveAreaBounds.width;
		int minY = 0;
		int maxY = this.curveAreaBounds.height;
		if (tmpxPos < minX || tmpxPos > maxX) {
			tmpxPos = tmpxPos < minX ? minX : maxX;
		}
		if (tmpyPos < minY || tmpyPos > maxY) {
			tmpyPos = tmpyPos < minY ? minY : maxY;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.graphicsType == GraphicsType.NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (recordSet != null && this.canvasImage != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", this.xDown, evt.x, this.xLast, this.yDown, evt.y, this.yLast)); //$NON-NLS-1$

				if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
					try {
						if (this.isZoomMouse && recordSet.isZoomMode() && this.isResetZoomPosition) {
							//define new rectangle
							this.leftZoom = evt.x - this.xDown > 0 ? this.xDown + this.offSetX : evt.x + this.offSetX;
							this.topZoom = evt.y - this.yDown > 0 ? this.yDown + this.offSetY : evt.y + this.offSetY;
							this.widthZoom = evt.x - this.xDown > 0 ? evt.x - this.xDown : this.xDown - evt.x;
							this.heightZoom = evt.y - this.yDown > 0 ? evt.y - this.yDown : this.yDown - evt.y;

							// detect directions to enable zoom or reset
							if (this.xDown < evt.x) { // left -> right
								this.isTransientZoom = true;
							}
							if (this.xDown > evt.x) { // right -> left
								this.isTransientZoom = false;
							}

							this.xLast = evt.x;
							this.yLast = evt.y;
							this.graphicCanvas.redraw(this.offSetX, this.offSetY, this.curveAreaBounds.width, this.curveAreaBounds.height, false);
						}
						else if (this.isLeftMouseMeasure) {

							// all obsolete lines are cleaned up now draw new position marker
							this.xPosMeasure = evt.x; // evt.x is already relative to curve area
							this.graphicCanvas.redraw(this.offSetX, this.offSetY, this.curveAreaBounds.width, this.curveAreaBounds.height, false);
						}
						else if (this.isRightMouseMeasure) {
							// update the new delta position
							this.xPosDelta = evt.x; // evt.x is already relative to curve area
							this.graphicCanvas.redraw(this.offSetX, this.offSetY, this.curveAreaBounds.width, this.curveAreaBounds.height, false);
						}
						else if (this.isPanMouse) {
							this.xDeltaPan = (this.xLast != 0 && this.xLast != evt.x) ? (this.xDeltaPan + (this.xLast < evt.x ? -1 : 1)) : 0;
							this.yDeltaPan = (this.yLast != 0 && this.yLast != evt.y) ? (this.yDeltaPan + (this.yLast < evt.y ? 1 : -1)) : 0;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, " xDeltaPan = " + this.xDeltaPan + " yDeltaPan = " + this.yDeltaPan); //$NON-NLS-1$ //$NON-NLS-2$
							if ((this.xDeltaPan != 0 && this.xDeltaPan % 5 == 0) || (this.yDeltaPan != 0 && this.yDeltaPan % 5 == 0)) {
								recordSet.shift(this.xDeltaPan, this.yDeltaPan); // 10% each direction
								this.doRedrawGraphics();
								this.xDeltaPan = this.yDeltaPan = 0;
							}
							this.xLast = evt.x;
							this.yLast = evt.y;
						}
						else if (this.isLeftCutMode) {
							//define new cut position black/left
							this.xPosCut = evt.x;
							this.graphicCanvas.redraw(this.offSetX, this.offSetY, this.curveAreaBounds.width, this.curveAreaBounds.height, false);
						}
						else if (this.isRightCutMode) {
							//define new cut position blue/right
							this.xPosCut = evt.x;
							this.graphicCanvas.redraw(this.offSetX, this.offSetY, this.curveAreaBounds.width, this.curveAreaBounds.height, false);
						}
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, "mouse pointer out of range", e); //$NON-NLS-1$
					}
				}
				else if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey) || recordSet.isAvgMedianMeasurementMode(measureRecordKey))) {
					if (this.xPosMeasure + 1 >= evt.x && this.xPosMeasure - 1 <= evt.x || this.xPosDelta + 1 >= evt.x && this.xPosDelta - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_SIZEWE)); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				}
				else if (this.isZoomMouse && !this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
				}
				else if (this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/Hand.gif")); //$NON-NLS-1$
				}
				else if (this.isLeftCutMode || this.isRightCutMode) {
					if (this.xPosCut + 1 >= evt.x && this.xPosCut - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_SIZEWE)); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				}
				else {
					this.graphicCanvas.setCursor(this.application.getCursor());
				}
			}
		}
	}

	/**
	 * calculate measurement status message to display the actual record value, formatted as configured
	 * @param actualDevice
	 * @param record the record where the measurement pointer is set
	 * @param indexPosMeasure index of measurement pointer
	 */
	private void calculateMeasurementStatusMessage(IDevice actualDevice, Record record, int indexPosMeasure) {
		this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0256, new Object[] { record.getName(),
				record.getDecimalFormat().format(actualDevice.translateValue(record, record.realGet(indexPosMeasure) / 1000.0)),
				record.getUnit(), record.getHorizontalDisplayPointAsFormattedTimeWithUnit(this.xPosMeasure) }));
	}

	/**
	 * calculate delta values based on pointer position formatted as configured
	 * @param actualDevice
	 * @param record the record where the measurement pointer is set
	 * @param indexPosMeasure index of black measurement
	 * @param indexPosDelta index of blue measurement pointer
	 */
	private void calculateDeltaValueStatusMessage(IDevice actualDevice, Record record, int indexPosMeasure, int indexPosDelta) {
		this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0257,
				new Object[] { record.getName(), Messages.getString(MessageIds.GDE_MSGT0212), 
						record.getDecimalFormat().format(actualDevice.translateValue(record, record.realGet(indexPosDelta) - record.realGet(indexPosMeasure)) / 1000.0),
						record.getUnit(), TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta) - record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
						record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta)), record.getUnit() }));
	}

	/**
	 * calculate delta values based on pointer position formatted as configured
	 * @param actualDevice
	 * @param record the record where the measurement pointer is set
	 * @param indexPosMeasure index of black measurement
	 * @param indexPosDelta index of blue measurement pointer
	 */
	private void calculateAvgMedianStatusMessage(IDevice actualDevice, Record record, int indexPosMeasure, int indexPosDelta) {
		this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0973,
				//{0} - Zeitdifferenz = {1}, Mittelwert = {2}, Median = {3}
				new Object[] { record.getName(), 
						TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta) - record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
						String.format("%3.1f [%s]", MathUtils.calculateAverage(record, indexPosMeasure, indexPosDelta), record.getUnit()),
						String.format("%3.1f [%s]", MathUtils.calculateMedian(record, indexPosMeasure, indexPosDelta), record.getUnit()) }));
				}

	/**
	 * calculates some interesting ratios for glider evaluation, values formatted as configured
	 * @param record the record where the measurement pointer is set
	 * @param indexPosMeasure index of black measurement
	 * @param indexPosDelta index of blue measurement pointer
	 */
	private void calculateSinkAndGlideRatioStatusMessage(IDevice actualDevice, Record record, int indexPosMeasure, int indexPosDelta) {
		Record tripLength =  record.getParent().get(record.getDevice().getAtlitudeTripSpeedOrdinals()[1]);
		Record altitude =  record.getParent().get(record.getDevice().getAtlitudeTripSpeedOrdinals()[0]);
		Record speed = record.getParent().get(record.getDevice().getAtlitudeTripSpeedOrdinals()[2]);
		
		double altitudeDelta = altitude.realGet(indexPosDelta) - altitude.realGet(indexPosMeasure);
		String altDelta_m = altitude.getDecimalFormat().format(actualDevice.translateValue(altitude, altitudeDelta / 1000.0));
		
		double tripDelta = tripLength.realGet(indexPosDelta) - tripLength.realGet(indexPosMeasure); // km * 1000m/km = m
		String tripDelta_m = tripLength.getDecimalFormat().format(actualDevice.translateValue(tripLength, tripDelta));
		
		String ratioTripAltitude = String.format("%4.2f", Math.abs(actualDevice.translateValue(tripLength, tripDelta) / actualDevice.translateValue(altitude, altitudeDelta / 1000.0))); // m/m
		String speedAvg_kmh = speed.getDecimalFormat().format(actualDevice.translateValue(speed, speed.getAvgValue(indexPosMeasure, indexPosDelta) / 1000.0)); //km/h

		this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0187,
				//GDE_MSGT0187=\  \u2206h/\u2206t = {1} {2}/{3} ===> {4} {5}/sec - \u2206s/\u2206h = {6} {7} ===> {8} bei ~{9} {10}
				new Object[] { 
						altDelta_m, record.getUnit(),
						TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta) - record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
						record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta)), record.getUnit(),
						tripDelta_m, altDelta_m, ratioTripAltitude,  speedAvg_kmh, speed.getUnit()}));
	}

	/**
	 * @param evt
	 */
	void mouseDownAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.graphicsType == GraphicsType.NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (this.canvasImage != null && recordSet != null) {
				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey) || recordSet.isAvgMedianMeasurementMode(measureRecordKey)) && this.xPosMeasure + 1 >= this.xDown
						&& this.xPosMeasure - 1 <= this.xDown) { // snap mouse pointer
					this.isLeftMouseMeasure = true;
					this.isRightMouseMeasure = false;
				}
				else if (measureRecordKey != null && (recordSet.isDeltaMeasurementMode(measureRecordKey) || recordSet.isAvgMedianMeasurementMode(measureRecordKey)) && this.xPosDelta + 1 >= this.xDown && this.xPosDelta - 1 <= this.xDown) { // snap mouse pointer
					this.isRightMouseMeasure = true;
					this.isLeftMouseMeasure = false;
				}
				else if (!this.isPanMouse && !this.isLeftCutMode && !this.isRightCutMode) {
					if (!this.isZoomMouse) //setting zoom mode is only required at the beginning of zoom actions, it will reset scale values to initial values
						this.application.setGraphicsMode(GraphicsMode.ZOOM, true);
					this.xLast = this.xDown;
					this.yLast = this.yDown;
					this.isResetZoomPosition = true;
				}
				else {
					this.isLeftMouseMeasure = false;
					this.isRightMouseMeasure = false;
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.graphicsType == GraphicsType.NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
			if (this.canvasImage != null && recordSet != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (this.isZoomMouse) {
					if (this.isTransientZoom) {
						this.isResetZoomPosition = false;
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, this.isZoomMouse + " - " + recordSet.isZoomMode() + " - " + this.isResetZoomPosition); //$NON-NLS-1$

						// sort the zoom values
						int xStart, xEnd, yMin, yMax;
						if (this.isZoomX) {
							xStart = this.xDown < this.xUp ? this.xDown : this.xUp;
							xEnd = this.xDown > this.xUp ? this.xDown + 1 : this.xUp;
							yMin = 0;
							yMax = this.curveAreaBounds.height - this.curveAreaBounds.y;
						}
						else if (this.isZoomY) {
							xStart = 0;
							xEnd = this.curveAreaBounds.width;
							yMin = this.curveAreaBounds.height - (this.yDown > this.yUp ? this.yDown : this.yUp);
							yMax = this.curveAreaBounds.height - (this.yDown < this.yUp ? this.yDown : this.yUp);
						}
						else {
							xStart = this.xDown < this.xUp ? this.xDown : this.xUp;
							xEnd = this.xDown > this.xUp ? this.xDown + 1 : this.xUp;
							yMin = this.curveAreaBounds.height - (this.yDown > this.yUp ? this.yDown : this.yUp);
							yMax = this.curveAreaBounds.height - (this.yDown < this.yUp ? this.yDown : this.yUp);
						}
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						if (xEnd - xStart > 5 && yMax - yMin > 5) {
							recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
							this.redrawGraphics(); //this.graphicCanvas.redraw();
						}
					}
					else {
						this.application.setGraphicsMode(GraphicsMode.RESET, false);
					}
				}
				else if (this.isLeftMouseMeasure) {
					this.isLeftMouseMeasure = false;
					//application.setStatusMessage(GDE.STRING_EMPTY);
				}
				else if (this.isRightMouseMeasure) {
					this.isRightMouseMeasure = false;
					//application.setStatusMessage(GDE.STRING_EMPTY);
				}
				else if (this.isLeftCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.get(0).getHorizontalPointIndexFromDisplayPoint(this.xUp), true);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
					}
					setModeState(GraphicsMode.RESET);
				}
				else if (this.isRightCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.get(0).getHorizontalPointIndexFromDisplayPoint(this.xUp), false);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
					}
					setModeState(GraphicsMode.RESET);
				}
				updatePanMenueButton();
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check if cut mode can be activated
	 * @param recordSet
	 */
	void updateCutModeButtons() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.graphicsType == GraphicsType.NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet()
					: (this.graphicsType == GraphicsType.COMPARE) ? this.application.getCompareSet() : this.application.getUtilitySet();
			if (this.canvasImage != null && recordSet != null) {
				//
				if (recordSet.isCutLeftEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(true, false);
				}
				else if (recordSet.isCutRightEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(false, true);
				}
				else {
					this.application.getMenuToolBar().enableCutButtons(false, false);
				}
			}
		}
	}

	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = DEFAULT_HEADER_GAP;
			GC gc = new GC(this.graphicsHeader);
			int stringHeight = gc.stringExtent(this.graphicsHeader.getText()).y;
			this.headerHeight = stringHeight;
			gc.dispose();
		}
		else {
			this.headerGap = 0;
			this.headerHeight = 0;
		}
		setComponentBounds();
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = DEFAULT_COMMENT_GAP;
			GC gc = new GC(this.recordSetComment);
			int stringHeight = gc.stringExtent(this.recordSetComment.getText()).y;
			this.commentHeight = stringHeight * 2 + 8;
			gc.dispose();
		}
		else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}
		setComponentBounds();
	}

	public void clearHeaderAndComment() {
		if (GraphicsComposite.this.channels.getActiveChannel() != null) {
			RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
			if (recordSet == null) {
				GraphicsComposite.this.recordSetComment.setText(GDE.STRING_EMPTY);
				GraphicsComposite.this.graphicsHeader.setText(GDE.STRING_EMPTY);
				GraphicsComposite.this.graphicsHeaderText = null;
				GraphicsComposite.this.recordSetCommentText = null;
				this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			}
			updateCaptions();
		}
	}

	public synchronized void updateCaptions() {
		GraphicsComposite.this.recordSetComment.redraw();
		GraphicsComposite.this.graphicsHeader.redraw();
	}

	/**
	 * resize the three areas: header, curve, comment
	 */
	void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width;
		int height = this.headerHeight;
		this.graphicsHeader.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.setBounds " + this.graphicsHeader.getBounds());

		y = this.headerGap + this.headerHeight;
		height = graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight);
		this.graphicCanvas.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "graphicCanvas.setBounds " + this.graphicCanvas.getBounds());

		y = this.headerGap + this.headerHeight + height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetComment.setBounds " + this.recordSetComment.getBounds());
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isRecordCommentChanged() {
		return this.isRecordCommentChanged;
	}

	public void updateRecordSetComment() {
		Channel activeChannel = GraphicsComposite.this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			if (recordSet != null) {
				if (this.isRecordCommentChanged) {
					recordSet.setRecordSetDescription(GraphicsComposite.this.recordSetComment.getText());
					recordSet.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
				}
				else {
					this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
					this.recordSetComment.setText(this.recordSetCommentText = recordSet.getRecordSetDescription());
					String graphicsHeaderExtend = this.graphicsHeaderText == null ? GDE.STRING_MESSAGE_CONCAT + recordSet.getName() : this.graphicsHeaderText.substring(11);
					this.graphicsHeader
							.setText(this.graphicsHeaderText = String.format("%s %s", LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd, recordSet.getStartTimeStamp()), graphicsHeaderExtend));
					this.graphicsHeader.redraw();
				}
				this.isRecordCommentChanged = false;
			}
		}
	}

	/**
	 * @return the graphic window content as image - only if compare window is visible return the compare window graphics
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		// decide if normal graphics window or compare window should be copied
		if (this.graphicsType == GraphicsType.COMPARE) {
			RecordSet compareRecordSet = DataExplorer.getInstance().getCompareSet();
			int numberCompareSetRecords = compareRecordSet.size();
			graphicsHeight = 30 + this.canvasBounds.height + 10 + numberCompareSetRecords * 20;
			graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
			GC graphicsGC = new GC(graphicsImage);
			graphicsGC.setBackground(this.surroundingBackground);
			graphicsGC.setForeground(this.graphicsHeader.getForeground());
			graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
			graphicsGC.setFont(this.graphicsHeader.getFont());
			GraphicsUtils.drawTextCentered(Messages.getString(MessageIds.GDE_MSGT0144), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
			graphicsGC.setFont(this.recordSetComment.getFont());
			for (int i = 0, yPos = 30 + this.canvasBounds.height + 5; i < numberCompareSetRecords; ++i, yPos += 20) {
				Record compareRecord = compareRecordSet.get(i);
				if (compareRecord != null) {
					graphicsGC.setForeground(SWTResourceManager.getColor(compareRecord.getRGB()));
					String recordName = "--- " + compareRecord.getName(); //$NON-NLS-1$
					GraphicsUtils.drawText(recordName, 20, yPos, graphicsGC, SWT.HORIZONTAL);
					graphicsGC.setForeground(this.recordSetComment.getForeground());
					String description = compareRecord.getDescription();
					description = description.contains("\n") ? description.substring(0, description.indexOf("\n")) : description; //$NON-NLS-1$ //$NON-NLS-2$
					Point pt = graphicsGC.textExtent(recordName); // string dimensions
					GraphicsUtils.drawText(description, pt.x + 30, yPos, graphicsGC, SWT.HORIZONTAL);
				}
			}
			graphicsGC.drawImage(this.canvasImage, 0, 30);
			graphicsGC.dispose();
		}
		else if (this.graphicsType == GraphicsType.UTIL) {
			graphicsHeight = 30 + this.canvasBounds.height;
			graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
			GC graphicsGC = new GC(graphicsImage);
			graphicsGC.setBackground(this.surroundingBackground);
			graphicsGC.setForeground(this.graphicsHeader.getForeground());
			graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
			graphicsGC.setFont(this.graphicsHeader.getFont());
			GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
			graphicsGC.drawImage(this.canvasImage, 0, 30);
			graphicsGC.dispose();
		}
		else {
			Channel activeChannel = this.channels.getActiveChannel();
			if (activeChannel != null) {
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null) {
					if (this.canvasImage != null) this.canvasImage.dispose();
					this.canvasImage = new Image(GDE.display, this.canvasBounds);
					this.canvasImageGC = new GC(this.canvasImage); //SWTResourceManager.getGC(this.canvasImage);
					this.canvasImageGC.setBackground(this.surroundingBackground);
					this.canvasImageGC.fillRectangle(this.canvasBounds);
					this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					drawCurves(activeRecordSet, this.canvasBounds, this.canvasImageGC);
					graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
					GC graphicsGC = new GC(graphicsImage);
					graphicsGC.setForeground(this.graphicsHeader.getForeground());
					graphicsGC.setBackground(this.surroundingBackground);
					graphicsGC.setFont(this.graphicsHeader.getFont());
					graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
					if (this.graphicsHeader.getText().length() > 1) {
						GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
					}
					graphicsGC.setFont(this.recordSetComment.getFont());
					if (this.recordSetComment.getText().length() > 1) {
						GraphicsUtils.drawText(this.recordSetComment.getText(), 20, graphicsHeight - 40, graphicsGC, SWT.HORIZONTAL);
					}
					graphicsGC.drawImage(this.canvasImage, 0, 30);
					graphicsGC.dispose();
					this.canvasImageGC.dispose();
				}
			}
		}
		return graphicsImage;
	}

	public void setFileComment() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			String fileComment = this.graphicsHeader.getText();
			if (fileComment.indexOf(GDE.STRING_MESSAGE_CONCAT) > 1) {
				fileComment = fileComment.substring(0, fileComment.indexOf(GDE.STRING_MESSAGE_CONCAT));
			}
			else {
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null && fileComment.indexOf(activeRecordSet.getName()) > 1) {
					fileComment = fileComment.substring(0, fileComment.indexOf(activeRecordSet.getName()));
				}
			}
			activeChannel.setFileDescription(fileComment);
			activeChannel.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
		}
	}

	private String getSelectedMeasurementsAsTable() {
		Properties displayProps = this.settings.getMeasurementDisplayProperties();
		RecordSet activeRecordSet = this.application.getActiveRecordSet();
		if (activeRecordSet != null) {
			int indexPosMeasure = activeRecordSet.get(0).getHorizontalPointIndexFromDisplayPoint(this.xPosMeasure);
			IDevice actualDevice = activeRecordSet.getDevice();
			this.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD));
			Vector<Record> records = activeRecordSet.getVisibleAndDisplayableRecords();
			String formattedTimeWithUnit = records.firstElement().getHorizontalDisplayPointAsFormattedTimeWithUnit(this.xPosMeasure);
			StringBuilder sb = new StringBuilder().append(String.format(" %16s ", formattedTimeWithUnit.substring(formattedTimeWithUnit.indexOf(GDE.CHAR_LEFT_BRACKET))));
			for (Record record : records) {
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(String.format("|%-10s", displayProps.getProperty(record.getName())));
				else {
					final String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					final String name = record.getName().substring(0, record.getName().length() >= 10 - unit.length() ? 10 - unit.length() : record.getName().length());
					final String format = "|%-" + (10 - unit.length()) + "s%" + unit.length() + "s";
					sb.append(String.format(format, name, unit));
				}
			}
			sb.append("| ").append(GDE.LINE_SEPARATOR).append(String.format("%16s  ", formattedTimeWithUnit.substring(0, formattedTimeWithUnit.indexOf(GDE.CHAR_LEFT_BRACKET) - 1)));
			for (Record record : records) {
				sb.append(String.format("|%7s   ", record.getDecimalFormat().format(actualDevice.translateValue(record, record.realGet(indexPosMeasure) / 1000.0))));
			}
			return sb.append("|").toString();
		}
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		return this.recordSetCommentText != null ? this.recordSetCommentText : GDE.STRING_EMPTY;
	}
}
