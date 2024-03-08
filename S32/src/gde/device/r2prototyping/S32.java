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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.r2prototyping;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * S32 device class as default 4 Channels with data 
 * @author Winfried Brügmann
 */
public class S32 extends DeviceConfiguration implements IDevice {
	protected final static Logger		log								= Logger.getLogger(S32.class.getName());


	protected final DataExplorer		application;
	protected final Channels				channels;
	protected final S32Dialog				dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public S32(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.r2prototyping.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new S32Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3804), Messages.getString(MessageIds.GDE_MSGT3804));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public S32(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.r2prototyping.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new S32Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3804), Messages.getString(MessageIds.GDE_MSGT3804));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
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
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)) * 4;
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
			lovDataSize = deviceDataBufferSize/3;
			//prepare convert buffer for conversion
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize/3);
			for (int j = deviceDataBufferSize/3; j < deviceDataBufferSize; j++) { //start at minimum length of data buffer 
				convertBuffer[j] = dataBuffer[offset+j];
				++lovDataSize;
				if (dataBuffer[offset+j] == 0x0A && dataBuffer[offset+j-1] == 0x0D)
					break;
			}

			//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
			//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
			//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
			//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
			//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
			//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
			System.arraycopy(convertBuffer, 0, timeBuffer, 0, timeBuffer.length);
			long dateTime = (long) (lastDateTime + 50);
			try {
				dateTime = Long.parseLong(String.format("%c%c%c%c%c%c000", (char)timeBuffer[4], (char)timeBuffer[5], (char)timeBuffer[6], (char)timeBuffer[7], (char)timeBuffer[8], (char)timeBuffer[9]).trim()); //10 digits //$NON-NLS-1$
			}
			catch (NumberFormatException e) {
				// ignore
			}
			deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime);
			log.log(Level.FINE, String.format("%d; %4.1fd ms - %d : %s", i, deltaTime, dateTime, new String(timeBuffer).trim())); //$NON-NLS-1$
			sumTimeDelta += deltaTime;
			lastDateTime = dateTime;

			recordSet.addPoints(convertDataBytes(points, convertBuffer), sumTimeDelta);
			offset += lovDataSize+8;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * set data line end points - this method will be called within getConvertedLovDataBytes only and requires to set startPos and crlfPos to zero before first call
	 * - data line start is defined with '$ ;'
	 * - end position is defined with '0d0a' (CRLF)
	 * @param dataBuffer
	 * @param startPos
	 * @param crlfPos
	 */
	protected void setDataLineStartAndLength(byte[] dataBuffer, int[] refStartLength) {
		int startPos = refStartLength[0] + refStartLength[1];

		for (; startPos < dataBuffer.length; ++startPos) {
			if (dataBuffer[startPos] == 0x24) {
				if (dataBuffer[startPos + 2] == 0x31 || dataBuffer[startPos + 3] == 0x31) break; // "$ ;" or "$  ;" (record set number two digits
			}
		}
		int crlfPos = refStartLength[0] = startPos;

		for (; crlfPos < dataBuffer.length; ++crlfPos) {
			if (dataBuffer[crlfPos] == 0x0D) if (dataBuffer[crlfPos + 1] == 0X0A) break; //0d0a (CRLF)
		}
		refStartLength[1] = crlfPos - startPos;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
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
			
			//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
			//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
			//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
			//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
			//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
			//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
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
			//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
			//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
			//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
			//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
			//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
			//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
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
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			double offset = 0; // != 0 if curve has an defined offset
			double reduction = 0;
			double factor = 1; // != 1 if a unit translation is required
			//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
			//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
			//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
			//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
			//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
			//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				offset = record.getOffset(); // != 0 if curve has an defined offset
				reduction = record.getReduction();
				factor = record.getFactor(); // != 1 if a unit translation is required

				dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
		boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
		property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
		boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

		try {
			if (subtractFirst) {
				reduction = record.getFirst() / 1000.0;
			}
			else if (subtractLast) {
				reduction = record.getLast() / 1000.0;
			}
		}
		catch (Throwable e) {
			reduction = 0;
		}

		//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
		//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
		//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
		//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
		//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
		//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
		double newValue = (value - reduction) * factor + offset;
		
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
		boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
		property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
		boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

		try {
			if (subtractFirst) {
				reduction = record.getFirst() / 1000.0;
			}
			else if (subtractLast) {
				reduction = record.getLast() / 1000.0;
			}
		}
		catch (Throwable e) {
			reduction = 0;
		}

		//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
		//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
		//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
		//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
		//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
		//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
		double newValue = (value - offset) / factor + reduction;
		
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int displayableCounter = 0;
		Record record;
		//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
		//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
		//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
		//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
		//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
		//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, record.getName() + " = " + this.getMeasurementNameReplacement(recordSet.getChannelConfigNumber(), i)); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, record.getName() + " hasReasonableData = " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.application.getActiveChannel().applyTemplateBasics(recordSet.getName());
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//0=Spannung BEC, 1=Strom BEC, 2=Spannung, 3=Strom, 4=Strom intern, 5=Leerlauf, 6=PWM, 7=Drehzahl Uni, 8=Drehzahl, 9=Kapazität, 
		//10=Temperatur PA, 11=Temperatur BEC, 12=Leistung, 13=Leistung intern, 14=Strom BEC max, 15=Strom Motor max, 
		//16=ALARM: Kapazität, 17=ALARM: Spannung, 18=ALARM: Temp PA, 19=ALARM: Spg BEC drop, 
		//20=ALARM: Temp ext 1, 21=ALARM: Temp ext 2, 22=ALARM: Temp ext 3, 23=ALARM: Temp ext 4, 24=ALARM: Temp ext 5, 
		//25=Temperatur ext 1, 26=Temperatur ext 2, 27=Temperatur ext 3, 28=Temperatur ext 4, 29=Temperatur ext 5, 
		//30=Drehzahl ext, 31=Speed GPS, 32=Höhe GPS, 33=Speed, 34=BID:Zellentype, 35=BID:Zellennummer, 36=BID:Kapazität, 37=BID:Ladung, 38=BID:Entladung, 39=BID:MaxDis]
		this.application.updateStatisticsData();
	}

	/**
	 * @return the dialog
	 */
	@Override
	public S32Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void open_closeCommPort() {
		switch (application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			this.importDeviceData();
			break;
		}
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3800), SWT.SINGLE);

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					S32.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								CSVSerialDataReaderWriter.read(selectedImportFile, S32.this, recordNameExtend, null, 
										new DataParser(S32.this.getDataBlockTimeUnitFactor(), 
												S32.this.getDataBlockLeader(), S32.this.getDataBlockSeparator().value(), 
												S32.this.getDataBlockCheckSumType(), S32.this.getDataBlockSize(InputTypes.FILE_IO)));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					S32.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
	
	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {			
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT3808, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT3808));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}
	
	/**
	 * query the process name according defined states
	 * @param buffer
	 * @return
	 */
	public String getProcessName(byte[] buffer) {
		return this.getRecordSetStateNameReplacement(Integer.parseInt((new String(buffer).split(this.getDataBlockSeparator().value())[1])));
	}

	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	@Override
	public boolean recordSetNumberFollowChannel() {
		return true;
	}

	/**
	 * query if the channel in use has dependency with each other to initiate file description synchronization 
	 * @return true for devices with one source of data distributed over channel record sets (HoTTAdapter, Av4ms_FV_762)
	 */
	public boolean useChannelWithSyncedDescription() { return true; }
}
