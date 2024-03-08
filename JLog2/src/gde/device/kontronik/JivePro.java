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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.kontronik;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.device.smmodellbau.JLog2;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class JivePro extends JLog2 {
	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public JivePro(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public JivePro(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)) * 6;
		int[] points = new int[recordSet.size()];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = 0;
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		double lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0;
		int timeBufferSize = 10;
		byte[] timeBuffer = new byte[timeBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			lovDataSize = deviceDataBufferSize/4;
			//prepare convert buffer for conversion
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize/3);
			for (int j = deviceDataBufferSize/4; j < deviceDataBufferSize; j++) { //start at minimum length of data buffer 
				convertBuffer[j] = dataBuffer[offset+j];
				++lovDataSize;
				if (dataBuffer[offset+j] == 0x0A && dataBuffer[offset+j-1] == 0x0D)
					break;
			}

			//0=Drehzahl, 1=Spannung Akku, 2=Strom Akku, 3=Kapazität Akku, 4=Strom Motor,  5=Strom Motor(peak), 6=Temperatur FET,
			//7=PWM, 8=Spannung BEC, 9=Strom BEC, 10=Temperatur BEC, 11=Gas (in), 12=Gas (eff),  13=Timing
			System.arraycopy(convertBuffer, 0, timeBuffer, 0, timeBuffer.length);
			long dateTime = (long) (lastDateTime + 50);
			try {
				dateTime = Long.parseLong(String.format("%c%c%c%c%c%c000", (char)timeBuffer[4], (char)timeBuffer[5], (char)timeBuffer[6], (char)timeBuffer[7], (char)timeBuffer[8], (char)timeBuffer[9]).trim()); //10 digits //$NON-NLS-1$
			}
			catch (NumberFormatException e) {
				// ignore
			}
			deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d; %4.1fd ms - %d : %s", i, deltaTime, dateTime, new String(timeBuffer).trim())); //$NON-NLS-1$
			sumTimeDelta += deltaTime;
			lastDateTime = dateTime;

			recordSet.addPoints(convertDataBytes(points, convertBuffer), sumTimeDelta);
			offset += lovDataSize+8;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		// prepare the serial CSV data parser
		DataParser data = new  DataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockLeader(), this.getDataBlockSeparator().value(), null, null, this.getDataBlockSize(InputTypes.FILE_IO), this.getDataBlockFormat(InputTypes.FILE_IO), true);
		int[] startLength = new int[] {0,0};
		byte[] lineBuffer = null;
				
		try {
			setDataLineStartAndLength(dataBuffer, startLength);
			lineBuffer = new byte[startLength[1]];
			System.arraycopy(dataBuffer, startLength[0], lineBuffer, 0, startLength[1]);
			data.parse(new String(lineBuffer), 0);
			int[] values = data.getValues();
			
			//0=Drehzahl, 1=Spannung Akku, 2=Strom Akku, 3=Kapazität Akku, 4=Strom Motor,  5=Strom Motor(peak), 6=Temperatur FET,
			//7=PWM, 8=Spannung BEC, 9=Strom BEC, 10=Temperatur BEC, 11=Gas (in), 12=Gas (eff),  13=Timing
			for (int i = 0; i < values.length; i++) {
				points[i] = values[i];
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = recordSet.isTimeStepConstant() ? 0 : GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
			log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$
		}

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			//0=Drehzahl, 1=Spannung Akku, 2=Strom Akku, 3=Kapazität Akku, 4=Strom Motor,  5=Strom Motor(peak), 6=Temperatur FET,
			//7=PWM, 8=Spannung BEC, 9=Strom BEC, 10=Temperatur BEC, 11=Gas (in), 12=Gas (eff),  13=Timing
			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2800));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					JivePro.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								Integer channelConfigNumber = dialog != null && !dialog.isDisposed() ? dialog.getTabFolderSelectionIndex() + 1 : null;
								String recordNameExtend = selectedImportFile.contains(GDE.STRING_UNDER_BAR) ? selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_UNDER_BAR)+1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT)) : selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.FILE_SEPARATOR)+1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								RecordSet redRecordSet = CSVSerialDataReaderWriter.read(selectedImportFile, JivePro.this, recordNameExtend, channelConfigNumber, 
										new DataParser(JivePro.this.getDataBlockTimeUnitFactor(), 
												JivePro.this.getDataBlockLeader(), JivePro.this.getDataBlockSeparator().value(), 
												JivePro.this.getDataBlockCheckSumType(), JivePro.this.getDataBlockSize(InputTypes.FILE_IO)));
								try {
									BufferedReader lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedImportFile), "ISO-8859-1")); //$NON-NLS-1$		
									String line;
									while ((line = lineReader.readLine()) != null) {
										if (line.toLowerCase().contains("kosmik")) {
											redRecordSet.setRecordSetDescription(redRecordSet.getRecordSetDescription() 
													+  GDE.LINE_SEPARATOR + line.substring(2).trim());
											lineReader.close();
											break;
										}
									}
								}
								catch (Exception e) {
									// ignore
								}
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					JivePro.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
	
	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	@Override
	public boolean recordSetNumberFollowChannel() {
		return false;
	}
	
	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	@Override
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {			
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGW2809, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGW2809));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}
}
