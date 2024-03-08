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
package gde.ui.dialog;

import gde.GDE;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to adjust the measurement scale end values
 * @author Winfried Brügmann
 */
public class AxisEndValuesDialog extends Dialog {
	final static Logger log = Logger.getLogger(AxisEndValuesDialog.class.getName());

	Shell			dialogShell;
	Canvas		canvas;
	CLabel		minValueLabel;
	CLabel		maxValueLabel;
	Button		okBbutton;
	CCombo		maxValueSelect;
	CCombo		minValueSelect;
	boolean 	isInit = false;
	double[]	newValues	= new double[2];
	String[]	initialValues = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "10", "25", "50", "100" };//$NON-NLS-$
	final DataExplorer application;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			AxisEndValuesDialog inst = new AxisEndValuesDialog(shell, SWT.NULL);
			double[] oldMinMax = {7.0, 1.0};
			double[] newMinMax = inst.open( oldMinMax );
			System.out.println("newMinMax = " + newMinMax[0] + ", " + newMinMax[1]); //$NON-NLS-1$
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AxisEndValuesDialog(Shell parent, int style) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
	}

	public double[] open(final double[] oldMinMax) {
		try {
			this.isInit = true;
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(345, 272);
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0151));
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/Measure.gif")); //$NON-NLS-1$
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
	      public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	AxisEndValuesDialog.this.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					try {
						double minValue = Double.valueOf(AxisEndValuesDialog.this.minValueSelect.getText().trim().replace(',', '.'));
						double maxValue = Double.valueOf(AxisEndValuesDialog.this.maxValueSelect.getText().trim().replace(',', '.'));
						if (maxValue < minValue) {
							minValue = Double.valueOf(AxisEndValuesDialog.this.maxValueSelect.getText().trim().replace(',', '.'));
							maxValue = Double.valueOf(AxisEndValuesDialog.this.minValueSelect.getText().trim().replace(',', '.'));
						}
						else if (maxValue == minValue) {
							minValue = minValue - 0.1;
							maxValue = maxValue + 0.1;
						}
						AxisEndValuesDialog.this.newValues = new double[]{minValue, maxValue};
					}
					catch (NumberFormatException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
					
					AxisEndValuesDialog.this.application.resetShellIcon();
				}
			});
			this.dialogShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (AxisEndValuesDialog.this.isInit) {
						log.log(Level.FINEST, "paintControl.paintControl, event=" + evt); //$NON-NLS-1$
						generateAndSetSelectionValues(AxisEndValuesDialog.this.maxValueSelect, oldMinMax[1], 20);
						generateAndSetSelectionValues(AxisEndValuesDialog.this.minValueSelect, oldMinMax[0], 20);
					}
					AxisEndValuesDialog.this.isInit = false;
				}
			});
			{
				FormData okBbuttonLData = new FormData();
				okBbuttonLData.width = 79;
				okBbuttonLData.height = 26;
				okBbuttonLData.left = new FormAttachment(0, 1000, 17);
				okBbuttonLData.top = new FormAttachment(0, 1000, 99);
				this.okBbutton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okBbutton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.okBbutton.setLayoutData(okBbuttonLData);
				this.okBbutton.setText("OK"); //$NON-NLS-1$
				this.okBbutton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okBbutton.widgetSelected, event=" + evt); //$NON-NLS-1$
						AxisEndValuesDialog.this.dialogShell.dispose();
					}
				});
			}
			{
				FormData canvasLData = new FormData();
				canvasLData.width = 200;
				canvasLData.height = 200;
				canvasLData.top = new FormAttachment(0, 1000, 18);
				canvasLData.right = new FormAttachment(1000, 1000, -24);
				canvasLData.bottom = new FormAttachment(1000, 1000, -21);
				canvasLData.left = new FormAttachment(0, 1000, 113);
				this.canvas = new Canvas(this.dialogShell, SWT.BORDER);
				this.canvas.setLayoutData(canvasLData);
				this.canvas.setBackgroundImage(SWTResourceManager.getImage("gde/resource/SmallGraph.gif")); //$NON-NLS-1$
			}
			{
				this.maxValueLabel = new CLabel(this.dialogShell, SWT.NONE);
				FormData maxValueLabelLData = new FormData();
				maxValueLabelLData.width = 89;
				maxValueLabelLData.height = 22;
				maxValueLabelLData.left = new FormAttachment(0, 1000, 12);
				maxValueLabelLData.top = new FormAttachment(0, 1000, 12);
				this.maxValueLabel.setLayoutData(maxValueLabelLData);
				this.maxValueLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.maxValueLabel.setText(Messages.getString(MessageIds.GDE_MSGT0152));
			}
			{
				FormData maxValueSelectLData = new FormData();
				maxValueSelectLData.width = 84;
				maxValueSelectLData.height = GDE.IS_LINUX ? 22 : 20;
				maxValueSelectLData.left = new FormAttachment(0, 1000, 12);
				maxValueSelectLData.top = new FormAttachment(0, 1000, 38);
				this.maxValueSelect = new CCombo(this.dialogShell, SWT.BORDER);
				this.maxValueSelect.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.maxValueSelect.setLayoutData(maxValueSelectLData);
				this.maxValueSelect.setItems(this.initialValues);
				this.maxValueSelect.select(7);
				this.maxValueSelect.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(Level.FINEST, "maxValueSelect.keyPressed, event=" + evt); //$NON-NLS-1$
						if (evt.character == SWT.CR) {
							AxisEndValuesDialog.this.dialogShell.dispose();
						}
					}
				});
			}
			{
				this.minValueLabel = new CLabel(this.dialogShell, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.width = 89;
				cLabel1LData.height = 22;
				cLabel1LData.left = new FormAttachment(0, 1000, 12);
				cLabel1LData.top = new FormAttachment(0, 1000, 169);
				this.minValueLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.minValueLabel.setLayoutData(cLabel1LData);
				this.minValueLabel.setText(Messages.getString(MessageIds.GDE_MSGT0153));
			}
			{
				FormData minValueSelectLData = new FormData();
				minValueSelectLData.width = 84;
				minValueSelectLData.height = GDE.IS_LINUX ? 22 : 20;
				minValueSelectLData.left = new FormAttachment(0, 1000, 12);
				minValueSelectLData.top = new FormAttachment(0, 1000, 195);
				this.minValueSelect = new CCombo(this.dialogShell, SWT.BORDER);
				this.minValueSelect.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.minValueSelect.setLayoutData(minValueSelectLData);
				this.minValueSelect.setItems(this.initialValues);
				this.minValueSelect.select(5);
				this.minValueSelect.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(Level.FINEST, "minValueSelect.keyPressed, event=" + evt); //$NON-NLS-1$
						if (evt.character == SWT.CR) {
							AxisEndValuesDialog.this.dialogShell.dispose();
						}
					}
				});
			}
			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return this.newValues;
	}

	/**
	 * generates a new string array of size, where the given value is in the array center
	 * set the array to the combo and select the given value
	 * @param combo to be modified
	 * @param value to be used as center
	 * @param size of the array generated
	 */
	void generateAndSetSelectionValues(final CCombo combo, final double value, int size) {
		int tmpSize = size + (size % 2) + 1;
		String strDoubleValue = String.format("%.3f", value); //$NON-NLS-1$
		int intValue = Double.valueOf(value).intValue();
		String[] tmpValues = new String[tmpSize];
		for (int i = intValue-tmpSize/2, j = tmpSize-1; i <= intValue+tmpSize/2; i++, j--) {
			tmpValues[j] = Integer.valueOf(i).toString();
		}
		combo.setItems(tmpValues);
		combo.setItem(10, strDoubleValue);
		combo.select(10);
	}

	/**
	 * query display status (isDisposed)
	 */
	public boolean isDisposed() {
		return this.dialogShell.isDisposed();
	}
}
