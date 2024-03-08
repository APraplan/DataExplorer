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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Logger;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.OrientationRequested;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.histo.ui.HistoSummaryWindow;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.ui.tab.ObjectDescriptionWindow;
import gde.ui.tab.StatisticsWindow;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * simple print configuration dialog
 * @author Winfried Brügmann
 */
public class PrintSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger	log	= Logger.getLogger(PrintSelectionDialog.class.getName());

	Shell								dialogShell;
	Button							printButton;
	Group								configurationGroup;
	Button							portraitButton;
	Button							landscapeReverseButton;
	Button							cancelButton;
	Button							objectButton;
	Button							statisticsButton;
	Button							graphicsButton;
	Button							curveCompareButton;
	Button							histoSummaryButton;
	Group								orientationGroup;
	Button							landscapeButton;

	final DataExplorer	application;
	private Button			headerButton;

	private enum Artefact {
		GRAPHICS, COMPARE, STATISTICS, OBJECT, //
		HISTOSUMMARY
	};

	/**
	* Auto-generated main method to display this
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			PrintSelectionDialog inst = new PrintSelectionDialog(shell, SWT.NULL);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PrintSelectionDialog(Shell parent, int style) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(null);
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/Print.gif")); //$NON-NLS-1$
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0441));
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(400, 400);
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
				@Override
				public void handleEvent(Event event) {
					switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						PrintSelectionDialog.this.dialogShell.close();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
					}
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					PrintSelectionDialog.this.application.resetShellIcon();
				}
			});
			{
				this.headerButton = new Button(this.dialogShell, SWT.CHECK | SWT.LEFT);
				this.headerButton.setText(Messages.getString(MessageIds.GDE_MSGT0456));
				this.headerButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0457));
				this.headerButton.setBounds(14, 7, 366, 27);
				this.headerButton.setSelection(true);
			}
			{
				this.configurationGroup = new Group(this.dialogShell, SWT.NONE);
				this.configurationGroup.setLayout(null);
				this.configurationGroup.setText(Messages.getString(MessageIds.GDE_MSGT0448));
				this.configurationGroup.setBounds(7, 36, 168, 286);
				this.configurationGroup.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "configurationGroup.paintControl, event=" + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						boolean isRecordSetGraphicsPrintable = false;
						if (activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null) isRecordSetGraphicsPrintable = activeRecordSet.size() > 0;
						}
						PrintSelectionDialog.this.graphicsButton.setEnabled(isRecordSetGraphicsPrintable);
						PrintSelectionDialog.this.graphicsButton.setSelection(isRecordSetGraphicsPrintable); // ET unclear why this line was set on comment
						PrintSelectionDialog.this.statisticsButton.setEnabled(isRecordSetGraphicsPrintable);
						PrintSelectionDialog.this.statisticsButton.setSelection(isRecordSetGraphicsPrintable); // ET unclear why this line was set on comment

						boolean isObjectOriented = PrintSelectionDialog.this.application.isObjectoriented();
						PrintSelectionDialog.this.objectButton.setEnabled(isObjectOriented);
						PrintSelectionDialog.this.objectButton.setSelection(isObjectOriented); // ET unclear why this line was set on comment

						boolean isCopareWindowPrintable = PrintSelectionDialog.this.application.isWithCompareSet();
						PrintSelectionDialog.this.curveCompareButton.setEnabled(isCopareWindowPrintable);
						PrintSelectionDialog.this.curveCompareButton.setSelection(isCopareWindowPrintable); // ET unclear why this line was set on comment

						boolean isHistoWindowPrintable = activeChannel != null
								? DataExplorer.getInstance().getHistoExplorer().map(h -> h.hasRecords()).orElse(false) //
								: false;

						PrintSelectionDialog.this.histoSummaryButton.setEnabled(isHistoWindowPrintable);
						PrintSelectionDialog.this.histoSummaryButton.setSelection(isHistoWindowPrintable);
					}
				});
				{
					this.graphicsButton = new Button(this.configurationGroup, SWT.CHECK | SWT.LEFT);
					this.graphicsButton.setText(Messages.getString(MessageIds.GDE_MSGT0453));
					this.graphicsButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0458));
					this.graphicsButton.setImage(SWTResourceManager.getImage("gde/resource/Graphics.gif")); //$NON-NLS-1$
					this.graphicsButton.setSelection(true);
					this.graphicsButton.setBounds(8, 12, 148, 45);
				}
				{
					this.statisticsButton = new Button(this.configurationGroup, SWT.CHECK | SWT.LEFT);
					this.statisticsButton.setText(Messages.getString(MessageIds.GDE_MSGT0350));
					this.statisticsButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0459));
					this.statisticsButton.setImage(SWTResourceManager.getImage("gde/resource/Statistics.gif")); //$NON-NLS-1$
					this.statisticsButton.setSelection(true);
					this.statisticsButton.setBounds(8, 57, 148, 45);
				}
				{
					this.objectButton = new Button(this.configurationGroup, SWT.CHECK | SWT.LEFT);
					this.objectButton.setText(Messages.getString(MessageIds.GDE_MSGT0455));
					this.objectButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0460));
					this.objectButton.setImage(SWTResourceManager.getImage("gde/resource/Object.gif")); //$NON-NLS-1$
					this.objectButton.setBounds(8, 102, 148, 45);
				}
				{
					this.curveCompareButton = new Button(this.configurationGroup, SWT.CHECK | SWT.LEFT);
					this.curveCompareButton.setText(Messages.getString(MessageIds.GDE_MSGT0442));
					this.curveCompareButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0461));
					this.curveCompareButton.setImage(SWTResourceManager.getImage("gde/resource/Graphics.gif")); //$NON-NLS-1$
					this.curveCompareButton.setSelection(false);
					this.curveCompareButton.setBounds(8, 147, 148, 45);
				}
				{
					this.histoSummaryButton = new Button(this.configurationGroup, SWT.CHECK | SWT.LEFT);
					this.histoSummaryButton.setText(Messages.getString(MessageIds.GDE_MSGT0865));
					this.histoSummaryButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0866));
					this.histoSummaryButton.setImage(SWTResourceManager.getImage("gde/resource/HistoSummary.gif")); //$NON-NLS-1$
					this.histoSummaryButton.setSelection(false);
					this.histoSummaryButton.setBounds(8, 192, 148, 45);
				}
			}
			{
				this.orientationGroup = new Group(this.dialogShell, SWT.NONE);
				this.orientationGroup.setLayout(null);
				this.orientationGroup.setBounds(181, 36, 203, 206);
				this.orientationGroup.setText(Messages.getString(MessageIds.GDE_MSGT0443));
				{
					this.portraitButton = new Button(this.orientationGroup, SWT.RADIO | SWT.LEFT);
					this.portraitButton.setText(Messages.getString(MessageIds.GDE_MSGT0444));
					this.portraitButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0445));
					this.portraitButton.setImage(SWTResourceManager.getImage("gde/resource/Portrait.gif")); //$NON-NLS-1$
					this.portraitButton.setSelection(true);
					this.portraitButton.setBounds(8, 16, 183, 65);
					this.portraitButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portraitButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PrintSelectionDialog.this.portraitButton.setSelection(true);
							PrintSelectionDialog.this.landscapeReverseButton.setSelection(false);
							PrintSelectionDialog.this.landscapeButton.setSelection(false);
						}
					});
				}
				{
					this.landscapeButton = new Button(this.orientationGroup, SWT.RADIO | SWT.LEFT);
					this.landscapeButton.setText(Messages.getString(MessageIds.GDE_MSGT0446));
					this.landscapeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0447));
					this.landscapeButton.setImage(SWTResourceManager.getImage("gde/resource/Landscape.gif")); //$NON-NLS-1$
					this.landscapeButton.setSelection(false);
					this.landscapeButton.setBounds(8, 74, 183, 65);
					this.landscapeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "landscapeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PrintSelectionDialog.this.portraitButton.setSelection(false);
							PrintSelectionDialog.this.landscapeReverseButton.setSelection(false);
							PrintSelectionDialog.this.landscapeButton.setSelection(true);
						}
					});
				}
				{
					this.landscapeReverseButton = new Button(this.orientationGroup, SWT.RADIO | SWT.LEFT);
					this.landscapeReverseButton.setText(Messages.getString(MessageIds.GDE_MSGT0449));
					this.landscapeReverseButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0450));
					this.landscapeReverseButton.setImage(SWTResourceManager.getImage("gde/resource/LandscapeReverse.gif")); //$NON-NLS-1$
					this.landscapeReverseButton.setSelection(false);
					this.landscapeReverseButton.setBounds(8, 134, 183, 65);
					this.landscapeReverseButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portraitAllButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PrintSelectionDialog.this.landscapeReverseButton.setSelection(true);
							PrintSelectionDialog.this.portraitButton.setSelection(false);
							PrintSelectionDialog.this.landscapeButton.setSelection(false);
						}
					});
				}
			}
			{
				this.printButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.printButton.setText(Messages.getString(MessageIds.GDE_MSGT0451));
				this.printButton.setBounds(212, 330, 149, 30);
				this.printButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						final int orientation = PrintSelectionDialog.this.landscapeButton.getSelection() ? PageFormat.REVERSE_LANDSCAPE
								: PrintSelectionDialog.this.portraitButton.getSelection() ? PageFormat.PORTRAIT : PageFormat.LANDSCAPE;
						EnumSet<Artefact> artefacts = EnumSet.noneOf(Artefact.class);
						if (PrintSelectionDialog.this.graphicsButton.getSelection()) artefacts.add(Artefact.GRAPHICS);
						if (PrintSelectionDialog.this.curveCompareButton.getSelection()) artefacts.add(Artefact.COMPARE);
						if (PrintSelectionDialog.this.statisticsButton.getSelection()) artefacts.add(Artefact.STATISTICS);
						if (PrintSelectionDialog.this.objectButton.getSelection()) artefacts.add(Artefact.OBJECT);
						if (PrintSelectionDialog.this.histoSummaryButton.getSelection()) artefacts.add(Artefact.HISTOSUMMARY);
						initiatePrinting(PrintSelectionDialog.this.headerButton.getSelection(), orientation, artefacts);
						PrintSelectionDialog.this.dialogShell.dispose();
					}
				});
			}
			{
				this.cancelButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.cancelButton.setText(Messages.getString(MessageIds.GDE_MSGT0452));
				this.cancelButton.setBounds(29, 330, 149, 30);
				this.cancelButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "cancelButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						PrintSelectionDialog.this.dialogShell.dispose();
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
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void initiatePrinting(final boolean isPrintRequestHeader, final int orientation, EnumSet<Artefact> artefacts) {
		final int currentTabIndex = this.application.getTabSelectionIndex();
		org.eclipse.swt.graphics.Image graphicsImageSWT, compareImageSWT, statisticsImageSWT, objectImageSWT;
		org.eclipse.swt.graphics.Image histoSummaryImageSWT;
		final java.awt.Image graphicsImageAWT, compareImageAWT, statisticsImageAWT, objectImageAWT;
		final java.awt.Image histoSummaryImageAWT;

		//get all required images
		if (artefacts.contains(Artefact.GRAPHICS) && this.application.getGraphicsPrintImage() != null) {
			this.application.selectTab(c -> c instanceof GraphicsWindow && ((GraphicsWindow) c).getGraphicsType().equals(GraphicsType.NORMAL));
			WaitTimer.delay(250);
			graphicsImageAWT = convertToAWT((graphicsImageSWT = this.application.getGraphicsTabContentAsImage()).getImageData());
			graphicsImageSWT.dispose();
		}
		else
			graphicsImageAWT = null;

		if (artefacts.contains(Artefact.STATISTICS) && this.application.getStatisticsTabContentAsImage() != null) {
			this.application.selectTab(c -> c instanceof StatisticsWindow);
			WaitTimer.delay(250);
			statisticsImageAWT = convertToAWT((statisticsImageSWT = this.application.getStatisticsTabContentAsImage()).getImageData());
			statisticsImageSWT.dispose();
		}
		else
			statisticsImageAWT = null;

		if (this.application.isObjectoriented() && artefacts.contains(Artefact.OBJECT) && this.application.getObjectTabContentAsImage() != null) {
			this.application.selectTab(c -> c instanceof ObjectDescriptionWindow);
			WaitTimer.delay(250);
			objectImageAWT = convertToAWT((objectImageSWT = this.application.getObjectTabContentAsImage()).getImageData());
			objectImageSWT.dispose();
		}
		else
			objectImageAWT = null;

		if (artefacts.contains(Artefact.COMPARE) && this.application.getGraphicsPrintImage() != null) {
			this.application.selectTab(c -> c instanceof GraphicsWindow && ((GraphicsWindow) c).getGraphicsType().equals(GraphicsType.COMPARE));
			WaitTimer.delay(250);
			compareImageAWT = convertToAWT((compareImageSWT = this.application.getGraphicsPrintImage()).getImageData());
			compareImageSWT.dispose();
		}
		else
			compareImageAWT = null;

		if (artefacts.contains(Artefact.HISTOSUMMARY)) {
			Optional<org.eclipse.swt.graphics.Image> image = this.application.getHistoExplorer().map(h -> h.getHistoSummaryContentAsImage());
			if (image.isPresent()) {
				this.application.selectTab(c -> c instanceof HistoSummaryWindow);
				WaitTimer.delay(250);
				histoSummaryImageAWT = convertToAWT((histoSummaryImageSWT = image.orElse(null)).getImageData());
				histoSummaryImageSWT.dispose();
			} else
				histoSummaryImageAWT = null;
		} else
			histoSummaryImageAWT = null;

		// select the tab which was active before
		this.application.selectTab(currentTabIndex);

		Thread printThread = new Thread() {
			@Override
			public void run() {
				Book book = new Book();
				PrinterJob printJob = PrinterJob.getPrinterJob();
				printJob.setPageable(book);

				//prepare the page layout
				PrintRequestAttributeSet printAttrSet = new HashPrintRequestAttributeSet();
				printAttrSet.add(new JobName(GDE.NAME_LONG, Settings.getInstance().getLocale()));
				switch (orientation) {
				case PageFormat.LANDSCAPE:
					printAttrSet.add(OrientationRequested.LANDSCAPE);
					break;
				case PageFormat.REVERSE_LANDSCAPE:
					printAttrSet.add(OrientationRequested.REVERSE_LANDSCAPE);
					break;
				default:
				case PageFormat.PORTRAIT:
					printAttrSet.add(OrientationRequested.PORTRAIT);
					break;
				}
				// show the choose printer dialog
				if (printJob.printDialog(printAttrSet)) {
					//get the page format to calculate scaling
					PageFormat documentPageFormat = printJob.getPageFormat(printAttrSet);
					log.log(Level.FINE, "documentPageFormat width = " + documentPageFormat.getWidth());
					log.log(Level.FINE, "documentPageFormat height = " + documentPageFormat.getHeight());
					log.log(Level.FINE, "documentPageFormat orientation = " + documentPageFormat.getOrientation());
					log.log(Level.FINE, "pageFormat.getImageableWidth() = " + documentPageFormat.getImageableWidth());
					log.log(Level.FINE, "pageFormat.getImageableHeight() = " + documentPageFormat.getImageableHeight());

					String fileName;
					Channel activeChannel = Channels.getInstance().getActiveChannel();
					if (activeChannel != null) {
						fileName = activeChannel.getFileName();
						fileName = fileName == null ? GDE.NAME_LONG + GDE.STRING_MESSAGE_CONCAT : fileName + GDE.STRING_MESSAGE_CONCAT;
					}
					else {
						fileName = GDE.NAME_LONG + GDE.STRING_MESSAGE_CONCAT;
					}

					if (documentPageFormat.getOrientation() == PageFormat.REVERSE_LANDSCAPE) {
						if (artefacts.contains(Artefact.GRAPHICS))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.STATISTICS))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.OBJECT)) book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.COMPARE))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.HISTOSUMMARY))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
					}
					else if (documentPageFormat.getOrientation() == PageFormat.LANDSCAPE) {
						if (artefacts.contains(Artefact.GRAPHICS))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.STATISTICS))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.OBJECT)) book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.COMPARE))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
						if (artefacts.contains(Artefact.HISTOSUMMARY))
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
					}
					else if (documentPageFormat.getOrientation() == PageFormat.PORTRAIT) {
						EnumSet<Artefact> printJobs = artefacts.clone();

						if (printJobs.containsAll(EnumSet.of(Artefact.GRAPHICS, Artefact.STATISTICS))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.GRAPHICS, Artefact.STATISTICS));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.GRAPHICS, Artefact.OBJECT))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.GRAPHICS, Artefact.OBJECT));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.GRAPHICS, Artefact.COMPARE))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.GRAPHICS, Artefact.COMPARE));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.GRAPHICS, Artefact.HISTOSUMMARY))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.GRAPHICS, Artefact.HISTOSUMMARY));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.GRAPHICS))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0143) : "", graphicsImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.GRAPHICS));
						}

						if (printJobs.containsAll(EnumSet.of(Artefact.STATISTICS, Artefact.OBJECT))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.STATISTICS, Artefact.OBJECT));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.STATISTICS, Artefact.COMPARE))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.STATISTICS, Artefact.COMPARE));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.STATISTICS, Artefact.HISTOSUMMARY))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.STATISTICS, Artefact.HISTOSUMMARY));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.STATISTICS))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0350) : "", statisticsImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.STATISTICS));
						}

						if (printJobs.containsAll(EnumSet.of(Artefact.OBJECT, Artefact.COMPARE))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.OBJECT, Artefact.COMPARE));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.OBJECT, Artefact.HISTOSUMMARY))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.OBJECT, Artefact.HISTOSUMMARY));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.OBJECT))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0403) : "", objectImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.OBJECT));
						}

						if (printJobs.containsAll(EnumSet.of(Artefact.COMPARE, Artefact.HISTOSUMMARY))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT,
									isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.COMPARE, Artefact.HISTOSUMMARY));
						}
						else if (printJobs.containsAll(EnumSet.of(Artefact.COMPARE))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0144) : "", compareImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.COMPARE));
						}

						if (printJobs.containsAll(EnumSet.of(Artefact.HISTOSUMMARY))) {
							book.append(new Document(isPrintRequestHeader ? fileName + Messages.getString(MessageIds.GDE_MSGT0867) : "", histoSummaryImageAWT), documentPageFormat);
							printJobs.removeAll(EnumSet.of(Artefact.HISTOSUMMARY));
						}
					}
					try {
						printJob.print();
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						PrintSelectionDialog.this.application.openMessageDialog(GDE.shell, e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
				}
			}
		};
		printThread.start();
	}

	/**
	 * This class is the painter for the document content.
	 * Depending of the image data loaded it will paint graphics, statistics or object characteristics
	 */
	static class Document extends Component implements Printable {
		private static final long	serialVersionUID	= 1L;

		final boolean							isPrintRequestHeader;
		final Image								awtBufferedImage1, awtBufferedImage2;
		final String							docType1, docType2;

		Document(String documentType, Image awtImage) {
			super();
			this.docType1 = documentType;
			this.awtBufferedImage1 = awtImage;
			this.docType2 = ""; //$NON-NLS-1$
			this.awtBufferedImage2 = null;
			this.isPrintRequestHeader = this.docType1 != null && this.docType1.length() > 1;
		}

		Document(String documentType1, Image awtImage1, String documentType2, Image awtImage2) {
			super();
			this.docType1 = documentType1;
			this.awtBufferedImage1 = awtImage1;
			this.docType2 = documentType2;
			this.awtBufferedImage2 = awtImage2;
			this.isPrintRequestHeader = this.docType1 != null && this.docType1.length() > 1;
		}

		/**
		 * implementation of the print method
		 * @param g the graphics context
		 * @param pageFormat
		 * @param page
		 * @return
		 */
		@Override
		public int print(Graphics g, PageFormat pageFormat, int page) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY()); // set the origin to 0,0 for the top left corner
			g2d.setPaint(Color.black);
			g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, GDE.WIDGET_FONT_SIZE)); //$NON-NLS-1$
			String date = StringHelper.getDate();
			Rectangle2D rectDate = g2d.getFontMetrics().getStringBounds(date, g2d);

			log.log(Level.FINE, "this.awtBufferedImage.getWidth(this) = " + this.awtBufferedImage1.getWidth(this));
			log.log(Level.FINE, "this.awtBufferedImage.getHeight(this) = " + this.awtBufferedImage1.getHeight(this));

			if (this.isPrintRequestHeader) {
				g2d.drawString(this.docType1, 2, 10);
				g2d.drawString(date, (int) (pageFormat.getImageableWidth() - rectDate.getWidth()), 10);
			}

			int offsetY = (this.isPrintRequestHeader ? 20 : 0);
			double usableImageHeight = (this.awtBufferedImage2 == null ? pageFormat.getImageableHeight() : pageFormat.getImageableHeight() / 2) - offsetY;

			double scaleFactor1 = pageFormat.getImageableWidth() / this.awtBufferedImage1.getWidth(this);
			if (scaleFactor1 * this.awtBufferedImage1.getHeight(this) < (usableImageHeight - offsetY)) {
				g2d.drawImage(this.awtBufferedImage1, 0, offsetY, (int) pageFormat.getImageableWidth(), (int) (scaleFactor1 * this.awtBufferedImage1.getHeight(this)), this);
			}
			else {
				scaleFactor1 = usableImageHeight / this.awtBufferedImage1.getHeight(this);
				int printWidth = (int) (scaleFactor1 * this.awtBufferedImage1.getWidth(this));
				int printHeight = (int) usableImageHeight;
				int offsetX = (int) ((pageFormat.getImageableWidth() - printWidth) / 2);
				g2d.drawImage(this.awtBufferedImage1, offsetX, offsetY, printWidth, printHeight, this);
			}

			if (this.awtBufferedImage2 != null) {

				if (this.isPrintRequestHeader) {
					g2d.drawString(this.docType2, 2, (int) (pageFormat.getImageableHeight() / 2 + 10));
					g2d.drawString(date, (int) (pageFormat.getImageableWidth() - rectDate.getWidth()), (int) (pageFormat.getImageableHeight() / 2 + 10));
				}

				offsetY = (int) (pageFormat.getImageableHeight() / 2 + (this.isPrintRequestHeader ? 20 : 0));
				usableImageHeight = pageFormat.getImageableHeight() / 2 - (this.isPrintRequestHeader ? 20 : 0);

				double scaleFactor2 = pageFormat.getImageableWidth() / this.awtBufferedImage2.getWidth(this);
				if (scaleFactor2 * this.awtBufferedImage2.getHeight(this) < usableImageHeight) {
					g2d.drawImage(this.awtBufferedImage2, 0, offsetY, (int) pageFormat.getImageableWidth(), (int) (scaleFactor2 * this.awtBufferedImage2.getHeight(this)), this);
				}
				else {
					scaleFactor2 = usableImageHeight / this.awtBufferedImage2.getHeight(this);
					int printWidth = (int) (scaleFactor2 * this.awtBufferedImage2.getWidth(this));
					int printHeight = (int) usableImageHeight;
					int offsetX = (int) ((pageFormat.getImageableWidth() - printWidth) / 2);
					g2d.drawImage(this.awtBufferedImage2, offsetX, offsetY, printWidth, printHeight, this);
				}
			}
			return (Printable.PAGE_EXISTS);
		}
	}

	BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		}
		RGB[] rgbs = palette.getRGBs();
		byte[] red = new byte[rgbs.length];
		byte[] green = new byte[rgbs.length];
		byte[] blue = new byte[rgbs.length];
		for (int i = 0; i < rgbs.length; i++) {
			RGB rgb = rgbs[i];
			red[i] = (byte) rgb.red;
			green[i] = (byte) rgb.green;
			blue[i] = (byte) rgb.blue;
		}
		if (data.transparentPixel != -1) {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
		}
		else {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
		}
		BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
		WritableRaster raster = bufferedImage.getRaster();
		int[] pixelArray = new int[1];
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				int pixel = data.getPixel(x, y);
				pixelArray[0] = pixel;
				raster.setPixel(x, y, pixelArray);
			}
		}
		return bufferedImage;
	}

	//////// backup from pure SWT printing where color problems exist while printing jpeg images (Blaustich) //////////
	//	// Show the Choose Printer dialog
	//	PrintDialog dialog = new PrintDialog(GDE.shell, SWT.NULL);
	//	PrinterData printerData = dialog.open();
	//
	//	if (printerData != null) {
	//		// Create the printer object
	//		Printer printer = new Printer(printerData);
	//
	//		// Calculate the scale factor between the screen resolution and printer
	//		// resolution in order to correctly size the image for the printer
	//		Point screenDPI = Display.getCurrent().getDPI();
	//		Point printerDPI = printer.getDPI();
	//
	//		// Determine the bounds of the entire area of the printer
	//		Rectangle trim = printer.computeTrim(0, 0, 0, 0);
	//		Rectangle clientArea = printer.getClientArea();
	//		Rectangle bounds = printer.getBounds();
	//		log.log(Level.INFO, "trim = " + trim);
	//		log.log(Level.INFO, "clientArea = " + clientArea);
	//		log.log(Level.INFO, "bounds = " + bounds);
	//		Rectangle printBounds = new Rectangle(-trim.x, -trim.y, clientArea.width-(trim.width), clientArea.height-(trim.height));
	//		log.log(Level.INFO, "printBounds = " + printBounds);
	//
	//		double scaleFactor = printerDPI.x / screenDPI.x;
	//
	//		// Start the print job
	//		if (isLandscape) {
	//
	//		}
	//		else { // is portrait
	//			if (printer.startJob(de.DE_NAME_LONG)) {
	//				if (isGraphics && printer.startPage()) {
	//					GC gc = new GC(printer);
	//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
	//					//gc.drawRectangle(printBounds);
	//					Point pt = drawHeader(printBounds, gc, "Graphics");
	//
	//					Image graphicsImage = this.application.getGraphicsAsImage();
	//					ImageData graphicsImageData = graphicsImage.getImageData();
	//					Image graphicsPrinterImage = new Image(printer, graphicsImageData);
	//					scaleFactor = 1.0 * printBounds.width / graphicsImageData.width;
	//					gc.drawImage(graphicsPrinterImage, 0, 0, graphicsImageData.width, graphicsImageData.height,
	//						printBounds.x, printBounds.y + pt.y + 20, (int) (scaleFactor * graphicsImageData.width), (int) (scaleFactor * graphicsImageData.height));
	//					graphicsPrinterImage.dispose();
	//					graphicsImage.dispose();
	//					isGraphics = false;
	//
	//					if (isStatistics) {
	//						gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
	//						String statistics = this.application.getStatisticsAsText();
	//						statistics = statistics.substring(statistics.indexOf(GDE.LINE_SEPARATOR));
	//						gc.drawText(statistics, printBounds.x, printBounds.y + printBounds.height / 2);
	//						isStatistics = false;
	//					}
	//					else if (isObject) {
	//						Image objectImage = this.application.getObjectContentAsImage();
	//						ImageData objectImageData = objectImage.getImageData();
	//						Image objectPrinterImage = new Image(printer, objectImageData);
	//						scaleFactor = 1.0 * printBounds.width / objectImageData.width;
	//						gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height,
	//							printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
	//						objectPrinterImage.dispose();
	//						objectImage.dispose();
	//						isObject = false;
	//					}
	//
	//					gc.dispose();
	//					printer.endPage();
	//				}
	//				if (isStatistics && printer.startPage()) {
	//					GC gc = new GC(printer);
	//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
	//					Point pt = drawHeader(printBounds, gc, "Statistics");
	//
	//					gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
	//					String statistics = this.application.getStatisticsAsText();
	//					statistics = statistics.substring(statistics.indexOf(GDE.LINE_SEPARATOR));
	//					gc.drawText(statistics, printBounds.x, printBounds.y + pt.y + 20);
	//					isStatistics = false;
	//
	//					if (isObject) {
	//						Image objectImage = this.application.getObjectContentAsImage();
	//						ImageData objectImageData = objectImage.getImageData();
	//						Image objectPrinterImage = new Image(printer, objectImageData);
	//						scaleFactor = 1.0 * printBounds.width / objectImageData.width;
	//						gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height,
	//							printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
	//						objectPrinterImage.dispose();
	//						objectImage.dispose();
	//						isObject = false;
	//					}
	//
	//					gc.dispose();
	//					printer.endPage();
	//				}
	//				if (isObject && printer.startPage()) {
	//					GC gc = new GC(printer, SWT.IMAGE_JPEG);
	//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
	//					Point pt = drawHeader(printBounds, gc, "Object characteristics");
	//
	//					Image objectImage = this.application.getObjectContentAsImage();
	//					ImageData objectImageData = objectImage.getImageData();
	//					Image objectPrinterImage = new Image(printer, objectImageData);
	//					scaleFactor = 1.0 * printBounds.width / objectImageData.width;
	//					gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height,
	//						printBounds.x, printBounds.y + pt.y + 20, (int)(scaleFactor * objectImageData.width),	(int)(scaleFactor * objectImageData.height));
	//					objectPrinterImage.dispose();
	//					objectImage.dispose();
	//					isObject = false;
	//
	//					gc.dispose();
	//					printer.endPage();
	//				}
	//			}
	//		}
	//		// End the job and dispose the printer
	//		printer.endJob();
	//		printer.dispose();

	//	Point drawHeader(Rectangle printBounds, GC gc, String type) {
	//		gc.drawText(de.DE_NAME_LONG + GDE.STRING_MESSAGE_CONCAT + type, printBounds.x, printBounds.y);
	//		String date = StringHelper.getDate();
	//		Point pt = gc.textExtent(date); // date string dimensions
	//		gc.drawText(date, printBounds.width-pt.x, printBounds.y);
	//		return pt;
	//	}

}
