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
    
    Copyright (c) 2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.messages.Messages;
import gde.ui.ParameterConfigControl;
import gde.ui.SWTResourceManager;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * Composite class to enable step charge configuration for Ultra Duo Plus 50/60
 */
public class StepChargeComposite extends ScrolledComposite {
	final static Logger	log	= Logger.getLogger(StepChargeComposite.class.getName());

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}

	private Group				stepChargeGroup;
	private Canvas			stepChargeCanvas;
	private Composite		stepAdjustmentComposite;
	private int					valueC1, valueC2, valueC3, valueC4;
	private int					valueA1, valueA2, valueA3, valueA4;
	private boolean			impulseValue1, impulseValue2, impulseValue3, impulseValue4;
	private boolean			reflexValue1, reflexValue2, reflexValue3, reflexValue4;
	private Button			reflexStep1, reflexStep2, reflexStep3, reflexStep4;
	private Button			impulsStep1, impulsStep2, impulsStep3, impulsStep4;
	private Text				stepTextC1, stepTextA1, stepTextC2, stepTextA2, stepTextC3, stepTextA3, stepTextC4, stepTextA4;
	private CLabel			stepLabel;
	private Group				stepGroup1, stepGroup2, stepGroup3, stepGroup4;
	private Slider			stepSliderC1, stepSliderA1, stepSliderC2, stepSliderA2, stepSliderC3, stepSliderA3, stepSliderC4, stepSliderA4;
	private ParameterConfigControl	peakConfigControl, cutOffTempControl, trickleCurrentControl;
	private Button									dischargeCheck;
	private Composite								configComposite;
	private final Composite					parent;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}

	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		StepChargeComposite inst = new StepChargeComposite(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	public StepChargeComposite(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
		initGUI();
		//initCapacity(3600, 0, 0, 0, 0);					//step charge unused
		//initCapacity(3600, 500, 2500, 3200, 4000);	//step charge previous used
		//initCapacity(3600, 1000, 2000, 3000, 4000);	//step charge previous used
		//initCurrent(3600, 0, 0, 0, 0);
		//initCurrent(3600, 2400, 4200, 4500, 3200);
		this.configComposite.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (evt.index == 0) {
					StepChargeComposite.this.parent.notifyListeners(SWT.Selection, new Event());
				}
			}
		});

	}

	public void setStepChargeValues(final int capacity, final int current, final int[] stepValues) {
		if (!this.isDisposed()) {
			//0=capacityStep1, 1=capacityStep2, 2=capacityStep3, 3=capacityStep4, 
			//4=currentStep1, 5=currentStep2, 6=currentStep3, 7=currentStep4
			//8=impulseCharge1, 9=impulseCharge2, 10=impulseCharge3, 11=impulseCharge4, 
			//12=reflexCharge1, 13=reflexCharge2, 14=reflexChareg3, 15=reflexCharge4
			//16=discharge, 17=trickleCurrent, 18=peakSensitive, 19=cutOffTemperature, 20=checksum
			if (StepChargeComposite.log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder("setStepChargeValues - Step values = ");
				for (int i : stepValues) {
					sb.append(i).append(GDE.STRING_COMMA).append(GDE.STRING_BLANK);
				}
				StepChargeComposite.log.log(Level.FINE, sb.toString());
				StepChargeComposite.log.log(Level.FINE, "setStepChargeValues - capacity = " + capacity + " current = " + current);
			}

			//capacity steps
			initCapacity(capacity, stepValues[0], stepValues[1], stepValues[2], stepValues[3]);
			//current steps
			initCurrent(current, stepValues[4], stepValues[5], stepValues[6], stepValues[7]);
			//impulse charge steps
			this.impulseValue1 = stepValues[8] == 1;
			this.impulsStep1.setSelection(this.impulseValue1);
			this.impulseValue2 = stepValues[9] == 1;
			this.impulsStep2.setSelection(this.impulseValue2);
			this.impulseValue3 = stepValues[10] == 1;
			this.impulsStep3.setSelection(this.impulseValue3);
			this.impulseValue4 = stepValues[11] == 1;
			this.impulsStep4.setSelection(this.impulseValue4);
			//reflex charge steps
			this.reflexValue1 = stepValues[12] == 1;
			this.reflexStep1.setSelection(this.reflexValue1);
			this.reflexValue2 = stepValues[13] == 1;
			this.reflexStep2.setSelection(this.reflexValue2);
			this.reflexValue3 = stepValues[14] == 1;
			this.reflexStep3.setSelection(this.reflexValue3);
			this.reflexValue4 = stepValues[15] == 1;
			this.reflexStep4.setSelection(this.reflexValue4);
			//discahrge first
			this.dischargeCheck.setSelection(stepValues[16] == 1);
			//trickel current
			this.trickleCurrentControl.getSlider().setSelection(stepValues[17]);
			Event evt = new Event();
			evt.data = new Object();
			this.trickleCurrentControl.getSlider().notifyListeners(SWT.Selection, evt);
			//peak sentity
			this.peakConfigControl.getSlider().setSelection(stepValues[18]);
			this.peakConfigControl.getSlider().notifyListeners(SWT.Selection, evt);
			//cut off temperature
			this.cutOffTempControl.getSlider().setSelection(stepValues[19]);
			this.cutOffTempControl.getSlider().notifyListeners(SWT.Selection, evt);
		}
	}

	public int[] getStepChargeValues(final int[] stepValues) {
		//0=capacityStep1, 1=capacityStep2, 2=capacityStep3, 3=capacityStep4, 4=currentStep1, 5=currentStep2, 6=currentStep3, 7=currentStep4
		//8=impulseCharge1, 9=impulseCharge2, 10=impulseCharge3, 11=impulseCharge4, 12=reflexCharge1, 13=reflexCharge2, 14=reflexChareg3, 15=reflexCharge4
		//16=discharge, 17=trickleCurrent, 18=peakSensitive, 19=cutOffTemperature (20=checksum)
		//capacity steps
		stepValues[0] = this.valueC1;
		stepValues[1] = this.valueC2;
		stepValues[2] = this.valueC3;
		stepValues[3] = this.valueC4;
		//current steps
		stepValues[4] = this.valueA1 * 10;
		stepValues[5] = this.valueA2 * 10;
		stepValues[6] = this.valueA3 * 10;
		stepValues[7] = this.valueA4 * 10;
		//impulse charge steps
		stepValues[8] = this.impulseValue1 && !this.reflexValue1 ? 1 : 0;
		stepValues[9] = this.impulseValue2 && !this.reflexValue2 ? 1 : 0;
		stepValues[10] = this.impulseValue3 && !this.reflexValue3 ? 1 : 0;
		stepValues[11] = this.impulseValue4 && !this.reflexValue4 ? 1 : 0;
		//reflex charge steps
		stepValues[12] = this.reflexValue1 && !this.impulseValue1 ? 1 : 0;
		stepValues[13] = this.reflexValue2 && !this.impulseValue2 ? 1 : 0;
		stepValues[14] = this.reflexValue3 && !this.impulseValue3 ? 1 : 0;
		stepValues[15] = this.reflexValue4 && !this.impulseValue4 ? 1 : 0;
		//discharge first
		stepValues[16] = this.dischargeCheck.getSelection() ? 1 : 0;
		//trickel current
		stepValues[17] = this.trickleCurrentControl.getSlider().getSelection();
		//peak sentity
		stepValues[18] = this.peakConfigControl.getSlider().getSelection();
		//cut off temperature
		stepValues[19] = this.cutOffTempControl.getSlider().getSelection();

		if (StepChargeComposite.log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder("getStepChargeValues - Step values = ");
			for (int i : stepValues) {
				sb.append(i).append(GDE.STRING_COMMA).append(GDE.STRING_BLANK);
			}
			StepChargeComposite.log.log(Level.FINE, sb.toString());
		}

		return stepValues;
	}

	private void initCurrent(final int initialCurrent, int currentStep1, int currentStep2, int currentStep3, int currentStep4) {

		if (currentStep4 == 0) {
			currentStep4 = initialCurrent / 100 * 120; //1.2 C round modulo 100
			currentStep1 = initialCurrent / 100 * 100; //1.0 C
			currentStep2 = initialCurrent / 100 * 200; //2.0 C
			currentStep3 = initialCurrent / 100 * 150; //1.5 C
		}

		this.valueA1 = currentStep1 / 10;
		//this.stepSliderA1.setMaximum(2000 + 10);
		this.stepSliderA1.setSelection(this.valueA1);
		this.stepSliderA1.redraw();
		this.stepTextA1.setText(String.format("%.2f", (StepChargeComposite.this.valueA1 / 100.0)));

		this.valueA2 = currentStep2 / 10;
		//this.stepSliderA2.setMaximum(2000 + 10);
		this.stepSliderA2.setSelection(this.valueA2);
		this.stepSliderA2.redraw();
		this.stepTextA2.setText(String.format("%.2f", (StepChargeComposite.this.valueA2 / 100.0)));

		this.valueA3 = currentStep3 / 10;
		//this.stepSliderA3.setMaximum(2000 + 10);
		this.stepSliderA3.setSelection(this.valueA3);
		this.stepSliderA3.redraw();
		this.stepTextA3.setText(String.format("%.2f", (StepChargeComposite.this.valueA3 / 100.0)));

		this.valueA4 = currentStep4 / 10;
		//this.stepSliderA4.setMaximum(2000 + 10);
		this.stepSliderA4.setSelection(this.valueA4);
		this.stepSliderA4.redraw();
		this.stepTextA4.setText(String.format("%.2f", (StepChargeComposite.this.valueA4 / 100.0)));

		this.stepChargeCanvas.redraw();
	}

	public void initCapacity(final int initialCapacity, int capacityStep1, int capacityStep2, int capacityStep3, int capacityStep4) {
		//int maxCapacityValue = 9900; //capacityStep4 != 0 ? capacityStep4 : initialCapacity * 120 / 100 * 100;
		//maxCapacityValue = maxCapacityValue != initialCapacity ? initialCapacity / 100 * 120 / 100 * 100 : maxCapacityValue;

		if (capacityStep4 == 0) {
			capacityStep1 = initialCapacity / 100 * 10;
			capacityStep2 = initialCapacity / 100 * 60;
			capacityStep3 = initialCapacity / 100 * 80;
			capacityStep4 = (initialCapacity + 70) * 111 / 10000 * 100;//round modulo 100
		}

		this.valueC1 = capacityStep1;
		//this.stepSliderC1.setMaximum(maxCapacityValue / 10 + 10);
		this.stepSliderC1.setSelection(this.valueC1);
		this.stepSliderC1.redraw();
		this.stepTextC1.setText(String.format("%d", StepChargeComposite.this.valueC1));

		this.valueC2 = capacityStep2;
		//this.stepSliderC2.setMaximum(maxCapacityValue / 10 + 10);
		this.stepSliderC2.setSelection(this.valueC2);
		this.stepSliderC2.redraw();
		this.stepTextC2.setText(String.format("%d", StepChargeComposite.this.valueC2));

		this.valueC3 = capacityStep3;
		//this.stepSliderC3.setMaximum(maxCapacityValue / 10 + 10);
		this.stepSliderC3.setSelection(this.valueC3);
		this.stepSliderC3.redraw();
		this.stepTextC3.setText(String.format("%d", StepChargeComposite.this.valueC3));

		this.valueC4 = capacityStep4;
		//this.stepSliderC4.setMaximum(maxCapacityValue / 10 + 10);
		this.stepSliderC4.setSelection(this.valueC4);
		this.stepSliderC4.redraw();
		this.stepTextC4.setText(String.format("%d", StepChargeComposite.this.valueC4));

		this.stepChargeCanvas.redraw();
	}

	private void initGUI() {
		try {
			FillLayout thisLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.setLayout(thisLayout);
			this.setSize(620, 380);
			{
				this.stepChargeGroup = new Group(this, SWT.NONE);
				FormLayout stepChargeGroupLayout = new FormLayout();
				this.stepChargeGroup.setLayout(stepChargeGroupLayout);
				{
					FormData stepChargeCanvasLData = new FormData();
					stepChargeCanvasLData.width = 594;
					stepChargeCanvasLData.height = 180;
					stepChargeCanvasLData.top = new FormAttachment(0, 1000, 5);
					stepChargeCanvasLData.left = new FormAttachment(0, 1000, 5);
					stepChargeCanvasLData.right = new FormAttachment(1000, 1000, -5);
					stepChargeCanvasLData.bottom = new FormAttachment(0, 1000, 130);
					this.stepChargeCanvas = new Canvas(this.stepChargeGroup, SWT.NO_FOCUS | SWT.BORDER);
					this.stepChargeCanvas.setLayoutData(stepChargeCanvasLData);
					this.stepChargeCanvas.setBackground(SWTResourceManager.getColor(250, 249, 230));
					this.stepChargeCanvas.addPaintListener(new PaintListener() {
						@Override
						public void paintControl(PaintEvent evt) {
							StepChargeComposite.log.log(Level.FINEST, "stepChargeCanvas.paintControl, event=" + evt);
							Rectangle canvasBounds = StepChargeComposite.this.stepChargeCanvas.getClientArea();
							if (canvasBounds.width > 0 && canvasBounds.height > 0) {
								Image canvasImage = new Image(Display.getDefault(), canvasBounds.width, canvasBounds.height);
								GC canvasImageGC = new GC(canvasImage);
								canvasImageGC.setBackground(SWTResourceManager.getColor(250, 249, 230));
								canvasImageGC.fillRectangle(canvasBounds);
								canvasImageGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
								canvasImageGC.setFont(SWTResourceManager.getFont("Sans Serife", GDE.WIDGET_FONT_SIZE + 6, SWT.BOLD));
								//cap = 3200; width = 550; -> valueC1 1600 -> 1600*100/3200 = 50
								canvasImageGC.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
								int maxCurrent = StepChargeComposite.this.stepSliderA1.getSelection() > 500 
										|| StepChargeComposite.this.stepSliderA2.getSelection() > 500
										|| StepChargeComposite.this.stepSliderA3.getSelection() > 500 
										|| StepChargeComposite.this.stepSliderA4.getSelection() > 500 
											? StepChargeComposite.this.stepSliderA1.getSelection() > 1000
											|| StepChargeComposite.this.stepSliderA2.getSelection() > 1000
											|| StepChargeComposite.this.stepSliderA3.getSelection() > 1000
											|| StepChargeComposite.this.stepSliderA4.getSelection() > 1000 
												? 2000 : 1000	: 500;

								int rightEdge1 = canvasBounds.width * (100 + StepChargeComposite.this.stepSliderC1.getSelection())
										/ ((100 + StepChargeComposite.this.stepSliderC4.getSelection()) == 0 ? 1 : (100 + StepChargeComposite.this.stepSliderC4.getSelection()));
								int centerPos1x = rightEdge1 / 2;
								int centerPos1Y = canvasBounds.height - (StepChargeComposite.this.valueA1 * canvasBounds.height / maxCurrent / 2);
								canvasImageGC.fillRectangle(new Rectangle(0, canvasBounds.height - StepChargeComposite.this.valueA1 * canvasBounds.height / maxCurrent, rightEdge1, canvasBounds.height));
								canvasImageGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
								if (centerPos1x < rightEdge1 && centerPos1Y < canvasBounds.height - 4) canvasImageGC.drawString("1", centerPos1x - 4, centerPos1Y - 4);

								int rightEdge2 = canvasBounds.width * (100 + StepChargeComposite.this.stepSliderC2.getSelection())
										/ ((100 + StepChargeComposite.this.stepSliderC4.getSelection()) == 0 ? 1 : (100 + StepChargeComposite.this.stepSliderC4.getSelection())) - rightEdge1;
								int centerPos2x = rightEdge1 + rightEdge2 / 2;
								int centerPos2y = canvasBounds.height - (StepChargeComposite.this.valueA2 * canvasBounds.height / maxCurrent / 2);
								canvasImageGC.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
								StepChargeComposite.log.log(Level.FINEST, "valueA2 = " + StepChargeComposite.this.valueA2);
								canvasImageGC.fillRectangle(new Rectangle(rightEdge1, canvasBounds.height - StepChargeComposite.this.valueA2 * canvasBounds.height / maxCurrent, rightEdge2, canvasBounds.height));
								if (centerPos2x < rightEdge1 + rightEdge2 && centerPos2y < canvasBounds.height - 4) canvasImageGC.drawString("2", centerPos2x - 4, centerPos2y - 4);

								int rightEdge3 = canvasBounds.width * (100 + StepChargeComposite.this.stepSliderC3.getSelection())
										/ ((100 + StepChargeComposite.this.stepSliderC4.getSelection()) == 0 ? 1 : (100 + StepChargeComposite.this.stepSliderC4.getSelection())) - rightEdge2 - rightEdge1;
								int centerPos3x = rightEdge1 + rightEdge2 + rightEdge3 / 2;
								int centerPos3Y = canvasBounds.height - (StepChargeComposite.this.valueA3 * canvasBounds.height / maxCurrent / 2);
								canvasImageGC.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
								canvasImageGC.fillRectangle(new Rectangle(rightEdge1 + rightEdge2, canvasBounds.height - StepChargeComposite.this.valueA3 * canvasBounds.height / maxCurrent, rightEdge3,
										canvasBounds.height));
								if (centerPos3x < rightEdge1 + rightEdge2 + rightEdge3 && centerPos3Y < canvasBounds.height - 4) canvasImageGC.drawString("3", centerPos3x - 4, centerPos3Y - 4);

								int rightEdge4 = canvasBounds.width * (100 + StepChargeComposite.this.stepSliderC4.getSelection())
										/ ((100 + StepChargeComposite.this.stepSliderC4.getSelection()) == 0 ? 1 : (100 + StepChargeComposite.this.stepSliderC4.getSelection()));
								int centerPos4x = rightEdge1 + rightEdge2 + rightEdge3 + (rightEdge4 - rightEdge3 - rightEdge2 - rightEdge1) / 2;
								int centerPos4Y = canvasBounds.height - (StepChargeComposite.this.valueA4 * canvasBounds.height / maxCurrent / 2);
								canvasImageGC.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
								canvasImageGC.fillRectangle(new Rectangle(rightEdge1 + rightEdge2 + rightEdge3, canvasBounds.height - StepChargeComposite.this.valueA4 * canvasBounds.height / maxCurrent, rightEdge4,
										canvasBounds.height));
								if (centerPos4x < rightEdge4 && centerPos4Y < canvasBounds.height - 4) canvasImageGC.drawString("4", centerPos4x - 4, centerPos4Y - 4);
								canvasImageGC.dispose();
								evt.gc.drawImage(canvasImage, 0, 0);
								canvasImage.dispose();
							}
						}
					});
				}
				{
					this.stepAdjustmentComposite = new Composite(this.stepChargeGroup, SWT.NONE);
					FillLayout stepAdjustmentCompositeLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
					FormData stepAdjustmentCompositeLData = new FormData();
					stepAdjustmentCompositeLData.left = new FormAttachment(0, 1000, 5);
					stepAdjustmentCompositeLData.top = new FormAttachment(0, 1000, 130);
					stepAdjustmentCompositeLData.bottom = new FormAttachment(1000, 1000, -140);
					stepAdjustmentCompositeLData.right = new FormAttachment(1000, 1000, -5);
					this.stepAdjustmentComposite.setLayoutData(stepAdjustmentCompositeLData);
					this.stepAdjustmentComposite.setLayout(stepAdjustmentCompositeLayout);
					{
						this.stepGroup1 = new Group(this.stepAdjustmentComposite, SWT.NONE);
						this.stepGroup1.setLayout(new FillLayout(SWT.VERTICAL));
						this.stepGroup1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stepGroup1.setText(Messages.getString(MessageIds.GDE_MSGT2343, new String[] { "1" }));
						{
							this.stepLabel = new CLabel(this.stepGroup1, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2344));
						}
						{
							this.stepTextC1 = new Text(this.stepGroup1, SWT.CENTER | SWT.BORDER);
							this.stepTextC1.setEditable(false);
							this.stepTextC1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderC1 = new Slider(this.stepGroup1, SWT.BORDER);
							this.stepSliderC1.setMinimum(0);
							this.stepSliderC1.setMaximum(9810);
							this.stepSliderC1.setIncrement(10);
							this.stepSliderC1.setSelection(250);
							this.valueC1 = 100 + this.stepSliderC1.getSelection();
							this.stepSliderC1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderC1.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueC1 = 100 + StepChargeComposite.this.stepSliderC1.getSelection();
									StepChargeComposite.this.stepTextC1.setText(String.format("%d", StepChargeComposite.this.valueC1)); //$NON-NLS-1$
									if (StepChargeComposite.this.valueC1 > StepChargeComposite.this.valueC2) {
										StepChargeComposite.this.valueC2 = StepChargeComposite.this.valueC1;
										StepChargeComposite.this.stepSliderC2.setSelection(StepChargeComposite.this.valueC2);
										StepChargeComposite.this.stepSliderC2.redraw();
									}
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.stepLabel = new CLabel(this.stepGroup1, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2345));
						}
						{
							this.stepTextA1 = new Text(this.stepGroup1, SWT.CENTER | SWT.BORDER);
							this.stepTextA1.setEditable(false);
							this.stepTextA1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderA1 = new Slider(this.stepGroup1, SWT.BORDER);
							this.stepSliderA1.setMinimum(0);
							this.stepSliderA1.setMaximum(2000);
							this.stepSliderA1.setIncrement(1);
							this.stepSliderA1.setSelection(520);
							this.valueA1 = 10 + this.stepSliderA1.getSelection();
							this.stepSliderA1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderA1.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueA1 = 10 + StepChargeComposite.this.stepSliderA1.getSelection();
									StepChargeComposite.this.stepTextA1.setText(String.format("%.2f", (StepChargeComposite.this.valueA1 / 100.0))); //$NON-NLS-1$
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.impulsStep1 = new Button(this.stepGroup1, SWT.CHECK | SWT.LEFT);
							this.impulsStep1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.impulsStep1.setText(Messages.getString(MessageIds.GDE_MSGT2346));
							this.impulsStep1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "impulsStep1.widgetSelected, event=" + evt);
									StepChargeComposite.this.impulseValue1 = StepChargeComposite.this.impulsStep1.getSelection();
									if (StepChargeComposite.this.impulseValue1) StepChargeComposite.this.reflexStep1.setSelection(false);
								}
							});
						}
						{
							this.reflexStep1 = new Button(this.stepGroup1, SWT.CHECK | SWT.LEFT);
							this.reflexStep1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.reflexStep1.setText(Messages.getString(MessageIds.GDE_MSGT2347));
							this.reflexStep1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "reflexStep1.widgetSelected, event=" + evt);
									StepChargeComposite.this.reflexValue1 = StepChargeComposite.this.reflexStep1.getSelection();
									if (StepChargeComposite.this.reflexValue1) StepChargeComposite.this.impulsStep1.setSelection(false);
								}
							});
						}
						this.stepGroup1.layout();
					}
					{
						this.stepGroup2 = new Group(this.stepAdjustmentComposite, SWT.NONE);
						this.stepGroup2.setLayout(new FillLayout(SWT.VERTICAL));
						this.stepGroup2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stepGroup2.setText(Messages.getString(MessageIds.GDE_MSGT2343, new String[] { "2" }));
						{
							this.stepLabel = new CLabel(this.stepGroup2, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2344));
						}
						{
							this.stepTextC2 = new Text(this.stepGroup2, SWT.CENTER | SWT.BORDER);
							this.stepTextC2.setEditable(false);
							this.stepTextC2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderC2 = new Slider(this.stepGroup2, SWT.BORDER);
							this.stepSliderC2.setMinimum(0);
							this.stepSliderC2.setMaximum(9810);
							this.stepSliderC2.setIncrement(10);
							this.stepSliderC2.setSelection(150);
							this.valueC2 = 100 + this.stepSliderC2.getSelection();
							this.stepSliderC2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderC2.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueC2 = 100 + StepChargeComposite.this.stepSliderC2.getSelection();
									StepChargeComposite.this.stepTextC2.setText(String.format("%d", StepChargeComposite.this.valueC2)); //$NON-NLS-1$
									if (StepChargeComposite.this.valueC2 < StepChargeComposite.this.valueC1) {
										StepChargeComposite.this.valueC1 = StepChargeComposite.this.valueC2;
										StepChargeComposite.this.stepSliderC1.setSelection(StepChargeComposite.this.valueC1);
										StepChargeComposite.this.stepSliderC1.redraw();
									}
									if (StepChargeComposite.this.valueC2 > StepChargeComposite.this.valueC3) {
										StepChargeComposite.this.valueC3 = StepChargeComposite.this.valueC2;
										StepChargeComposite.this.stepSliderC3.setSelection(StepChargeComposite.this.valueC3);
										StepChargeComposite.this.stepSliderC3.redraw();
									}
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.stepLabel = new CLabel(this.stepGroup2, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2345));
						}
						{
							this.stepTextA2 = new Text(this.stepGroup2, SWT.CENTER | SWT.BORDER);
							this.stepTextA2.setEditable(false);
							this.stepTextA2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderA2 = new Slider(this.stepGroup2, SWT.BORDER);
							this.stepSliderA2.setMinimum(0);
							this.stepSliderA2.setMaximum(2000);
							this.stepSliderA2.setIncrement(1);
							this.stepSliderA2.setSelection(520);
							this.valueA2 = 10 + this.stepSliderA2.getSelection();
							this.stepSliderA2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderA2.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueA2 = 10 + StepChargeComposite.this.stepSliderA2.getSelection();
									StepChargeComposite.this.stepTextA2.setText(String.format("%.2f", (StepChargeComposite.this.valueA2 / 100.0))); //$NON-NLS-1$
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.impulsStep2 = new Button(this.stepGroup2, SWT.CHECK | SWT.LEFT);
							this.impulsStep2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.impulsStep2.setText(Messages.getString(MessageIds.GDE_MSGT2346));
							this.impulsStep2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "impulsStep2.widgetSelected, event=" + evt);
									StepChargeComposite.this.impulseValue2 = StepChargeComposite.this.impulsStep2.getSelection();
									if (StepChargeComposite.this.impulseValue2) StepChargeComposite.this.reflexStep2.setSelection(false);
								}
							});
						}
						{
							this.reflexStep2 = new Button(this.stepGroup2, SWT.CHECK | SWT.LEFT);
							this.reflexStep2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.reflexStep2.setText(Messages.getString(MessageIds.GDE_MSGT2347));
							this.reflexStep2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "reflexStep2.widgetSelected, event=" + evt);
									StepChargeComposite.this.reflexValue2 = StepChargeComposite.this.reflexStep2.getSelection();
									if (StepChargeComposite.this.reflexValue2) StepChargeComposite.this.impulsStep2.setSelection(false);
								}
							});
						}
						this.stepGroup2.layout();
					}
					{
						this.stepGroup3 = new Group(this.stepAdjustmentComposite, SWT.NONE);
						this.stepGroup3.setLayout(new FillLayout(SWT.VERTICAL));
						this.stepGroup3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stepGroup3.setText(Messages.getString(MessageIds.GDE_MSGT2343, new String[] { "3" }));
						{
							this.stepLabel = new CLabel(this.stepGroup3, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2344));
						}
						{
							this.stepTextC3 = new Text(this.stepGroup3, SWT.CENTER | SWT.BORDER);
							this.stepTextC3.setEditable(false);
							this.stepTextC3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderC3 = new Slider(this.stepGroup3, SWT.BORDER);
							this.stepSliderC3.setMinimum(0);
							this.stepSliderC3.setMaximum(9810);
							this.stepSliderC3.setIncrement(10);
							this.stepSliderC3.setSelection(150);
							this.valueC3 = 100 + this.stepSliderC3.getSelection();
							this.stepSliderC3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderC1.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueC3 = 100 + StepChargeComposite.this.stepSliderC3.getSelection();
									StepChargeComposite.this.stepTextC3.setText(String.format("%d", StepChargeComposite.this.valueC3)); //$NON-NLS-1$
									if (StepChargeComposite.this.valueC3 < StepChargeComposite.this.valueC2) {
										StepChargeComposite.this.valueC2 = StepChargeComposite.this.valueC3;
										StepChargeComposite.this.stepSliderC2.setSelection(StepChargeComposite.this.valueC2);
										StepChargeComposite.this.stepSliderC2.redraw();
									}
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.stepLabel = new CLabel(this.stepGroup3, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2345));
						}
						{
							this.stepTextA3 = new Text(this.stepGroup3, SWT.CENTER | SWT.BORDER);
							this.stepTextA3.setEditable(false);
							this.stepTextA3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderA3 = new Slider(this.stepGroup3, SWT.BORDER);
							this.stepSliderA3.setMinimum(0);
							this.stepSliderA3.setMaximum(2000);
							this.stepSliderA3.setIncrement(1);
							this.stepSliderA3.setSelection(520);
							this.valueA3 = 10 + this.stepSliderA3.getSelection();
							this.stepSliderA3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderA3.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueA3 = 10 + StepChargeComposite.this.stepSliderA3.getSelection();
									StepChargeComposite.this.stepTextA3.setText(String.format("%.2f", (StepChargeComposite.this.valueA3 / 100.0))); //$NON-NLS-1$
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.impulsStep3 = new Button(this.stepGroup3, SWT.CHECK | SWT.LEFT);
							this.impulsStep3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.impulsStep3.setText(Messages.getString(MessageIds.GDE_MSGT2346));
							this.impulsStep3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "impulsStep3.widgetSelected, event=" + evt);
									StepChargeComposite.this.impulseValue3 = StepChargeComposite.this.impulsStep3.getSelection();
									if (StepChargeComposite.this.impulseValue3) StepChargeComposite.this.reflexStep3.setSelection(false);
								}
							});
						}
						{
							this.reflexStep3 = new Button(this.stepGroup3, SWT.CHECK | SWT.LEFT);
							this.reflexStep3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.reflexStep3.setText(Messages.getString(MessageIds.GDE_MSGT2347));
							this.reflexStep3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "reflexStep3.widgetSelected, event=" + evt);
									StepChargeComposite.this.reflexValue3 = StepChargeComposite.this.reflexStep3.getSelection();
									if (StepChargeComposite.this.reflexValue3) StepChargeComposite.this.impulsStep3.setSelection(false);
								}
							});
						}
						this.stepGroup3.layout();
					}
					{
						this.stepGroup4 = new Group(this.stepAdjustmentComposite, SWT.NONE);
						this.stepGroup4.setLayout(new FillLayout(SWT.VERTICAL));
						this.stepGroup4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stepGroup4.setText(Messages.getString(MessageIds.GDE_MSGT2343, new String[] { "4" }));
						{
							this.stepLabel = new CLabel(this.stepGroup4, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2344));
						}
						{
							this.stepTextC4 = new Text(this.stepGroup4, SWT.CENTER | SWT.BORDER);
							this.stepTextC4.setEditable(false);
							this.stepTextC4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderC4 = new Slider(this.stepGroup4, SWT.BORDER);
							this.stepSliderC4.setMinimum(0);
							this.stepSliderC4.setMaximum(9810);
							this.stepSliderC4.setIncrement(10);
							this.stepSliderC4.setSelection(900 - 450);
							this.valueC4 = 100 + this.stepSliderC4.getSelection();
							this.stepSliderC4.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderC1.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueC4 = 100 + StepChargeComposite.this.stepSliderC4.getSelection();
									StepChargeComposite.this.stepTextC4.setText(String.format("%d", StepChargeComposite.this.valueC4)); //$NON-NLS-1$
									if (StepChargeComposite.this.valueC4 < StepChargeComposite.this.valueC3) {
										StepChargeComposite.this.valueC3 = StepChargeComposite.this.valueC4;
										StepChargeComposite.this.stepSliderC3.setSelection(StepChargeComposite.this.valueC3);
										StepChargeComposite.this.stepSliderC3.redraw();
									}
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.stepLabel = new CLabel(this.stepGroup4, SWT.CENTER);
							this.stepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stepLabel.setText(Messages.getString(MessageIds.GDE_MSGT2345));
						}
						{
							this.stepTextA4 = new Text(this.stepGroup4, SWT.CENTER | SWT.BORDER);
							this.stepTextA4.setEditable(false);
							this.stepTextA4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						}
						{
							this.stepSliderA4 = new Slider(this.stepGroup4, SWT.BORDER);
							this.stepSliderA4.setMinimum(0);
							this.stepSliderA4.setMaximum(2000);
							this.stepSliderA4.setIncrement(1);
							this.stepSliderA4.setSelection(520);
							this.valueA4 = 10 + this.stepSliderA4.getSelection();
							this.stepSliderA4.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "stepSliderA4.widgetSelected, event=" + evt);
									StepChargeComposite.this.valueA4 = 10 + StepChargeComposite.this.stepSliderA4.getSelection();
									StepChargeComposite.this.stepTextA4.setText(String.format("%.2f", (StepChargeComposite.this.valueA4 / 100.0))); //$NON-NLS-1$
									if (evt.detail != 0) {
										StepChargeComposite.this.stepChargeCanvas.redraw();
									}
								}
							});
						}
						{
							this.impulsStep4 = new Button(this.stepGroup4, SWT.CHECK | SWT.LEFT);
							this.impulsStep4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.impulsStep4.setText(Messages.getString(MessageIds.GDE_MSGT2346));
							this.impulsStep4.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "impulsStep4.widgetSelected, event=" + evt);
									StepChargeComposite.this.impulseValue4 = StepChargeComposite.this.impulsStep4.getSelection();
									if (StepChargeComposite.this.impulseValue4) StepChargeComposite.this.reflexStep4.setSelection(false);
								}
							});
						}
						{
							this.reflexStep4 = new Button(this.stepGroup4, SWT.CHECK | SWT.LEFT);
							this.reflexStep4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.reflexStep4.setText(Messages.getString(MessageIds.GDE_MSGT2347));
							this.reflexStep4.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									StepChargeComposite.log.log(Level.FINEST, "reflexStep4.widgetSelected, event=" + evt);
									StepChargeComposite.this.reflexValue4 = StepChargeComposite.this.reflexStep4.getSelection();
									if (StepChargeComposite.this.reflexValue4) StepChargeComposite.this.impulsStep4.setSelection(false);
								}
							});
						}
						this.stepGroup4.layout();
					}
				}
				{
					this.configComposite = new Composite(this.stepChargeGroup, SWT.NONE);
					RowLayout configCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					FormData configCompositeLData = new FormData();
					configCompositeLData.left = new FormAttachment(0, 1000, 5);
					configCompositeLData.top = new FormAttachment(1000, 1000, -140);
					configCompositeLData.right = new FormAttachment(1000, 1000, -5);
					configCompositeLData.bottom = new FormAttachment(1000, 1000, -5);
					this.configComposite.setLayoutData(configCompositeLData);
					this.configComposite.setLayout(configCompositeLayout);
					this.configComposite.setBackground(this.stepChargeGroup.getBackground());
					{
						Composite spacer = new Composite(this.configComposite, SWT.NONE);
						RowData spacerLData = new RowData();
						spacerLData.width = 30;
						spacerLData.height = 20;
						spacer.setLayoutData(spacerLData);

						this.dischargeCheck = new Button(this.configComposite, SWT.CHECK | SWT.LEFT);
						RowData dischargeCheckLData = new RowData();
						dischargeCheckLData.width = 500;
						dischargeCheckLData.height = 20;
						this.dischargeCheck.setLayoutData(dischargeCheckLData);
						this.dischargeCheck.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.dischargeCheck.setText(Messages.getString(MessageIds.GDE_MSGT2348));
						this.dischargeCheck.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								StepChargeComposite.log.log(Level.FINEST, "dischargeCheck.widgetSelected, event=" + evt);
								StepChargeComposite.this.parent.notifyListeners(SWT.Selection, new Event());
							}
						});
					}
					{
						int[] tricleCurrentValue = new int[] { 50 };
						this.trickleCurrentControl = new ParameterConfigControl(this.configComposite, tricleCurrentValue, 0, "%d", Messages.getString(MessageIds.GDE_MSGT2279), 220, "0 - 500 [mA]", 120, false,
								50, 150, 0, 500);
						this.trickleCurrentControl.getSlider().notifyListeners(SWT.Selection, new Event());
					}
					{
						int[] peakSensityValue = new int[] { 5 };
						this.peakConfigControl = new ParameterConfigControl(this.configComposite, peakSensityValue, 0, "%d", "Peak Empfindlichkeit", 220, "0 - 15 [mV/Z]", 120, false, 50, 150, 0, 15);
						this.peakConfigControl.getSlider().notifyListeners(SWT.Selection, new Event());
					}
					{
						int[] cutOffTempValue = new int[] { 80 };
						this.cutOffTempControl = new ParameterConfigControl(this.configComposite, cutOffTempValue, 0, "%d", "Abschalttemperatur", 220, "10 - 80 [°C]", 120, false, 50, 150, 10, 80);
						this.cutOffTempControl.getSlider().notifyListeners(SWT.Selection, new Event());
					}
				}
			}
			this.setContent(this.stepChargeGroup);
			this.stepChargeGroup.setSize(620, 490);
			this.addControlListener(new ControlListener() {
				@Override
				public void controlResized(ControlEvent evt) {
					StepChargeComposite.log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt); //$NON-NLS-1$
					StepChargeComposite.this.stepChargeGroup.setSize(getClientArea().width, 490);
				}

				@Override
				public void controlMoved(ControlEvent evt) {
					StepChargeComposite.log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt); //$NON-NLS-1$
					StepChargeComposite.this.stepChargeGroup.setSize(getClientArea().width, 490);
				}
			});
			this.layout();

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
