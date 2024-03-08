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
package gde.device.htronic;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * AkkuMaster C4 device class implementation
 * @author Winfried Brügmann
 */
public class AkkuMasterC4 extends DeviceConfiguration implements IDevice {
	final static Logger														log									= Logger.getLogger(AkkuMasterC4.class.getName());

	final DataExplorer														application;
	final AkkuMasterC4Dialog											dialog;
	final AkkuMasterC4SerialPort									serialPort;
	final Channels																channels;
	HashMap<String, AkkuMasterCalculationThread>	calculationThreads	= new HashMap<String, AkkuMasterCalculationThread>();

	/**
	 * constructor using the device properties file for initialization
	 * @param deviceProperties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public AkkuMasterC4(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public AkkuMasterC4(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		//nothing to do here
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
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 * @return size of the device data in bytes
	 */
	public int getLovDataByteSize() {
		return 55; // 0x33 = 51 + 4 (counter)
	}
	
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertDataBuffer = new byte[14 + 16];
		int[] points = new int[this.getNumberOfMeasurements(1)];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) { 		
			//StringBuilder sb = new StringBuilder();
			//for (byte b : dataBuffer) {
			//	sb.append(String.format("%02x", b)).append(" ");
			//}
			//log.log(Level.FINE, sb.toString());
			
			//discharge   			      Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
			//	                      Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
			//33 00 00 00 51 82 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 ca 00 00 07 a3 01 25 19 00 00 00 01 00 3c 0d 0a 20 20 35 38 33 30 00 00 00 00 48 02 00 00             
			//offset      51 82 00 03 00 0C 08 98 02 58 02 58                      52 00 02 00 00 0B C7 00 00 0D 00 00 00 01
			//charge  				        Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
			//		                    Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
			//33 00 00 00 51 81 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 cb 07 f0 0d 02 01 25 1f 03 17 38 01 00 3c 0d 0a 20 31 38 31 30 30 00 00 00 00 13 07 00 00 
			//offset      51 81 00 03 00 0C 08 98 02 58 02 58                      52 02 85 0A 4A 0C DE 01 04 1F 04 17 1B 01

			System.arraycopy(dataBuffer,  4 + i*lovDataSize, convertDataBuffer,  0, 14); //configurationBuffer.length
			System.arraycopy(dataBuffer, 23 + i*lovDataSize, convertDataBuffer, 14, 16); //measurementsBuffer.length
			recordSet.addPoints(convertDataBytes(points, convertDataBuffer));
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {	

		// build the point array according curves from record set
		//int[] points = new int[getRecordSet().size()];
		
		HashMap<String, Object> values = new HashMap<String, Object>(7);
		byte[] configurationBuffer	= new byte[14];
		System.arraycopy(dataBuffer, 0, configurationBuffer, 0, configurationBuffer.length);
		byte[] measurementsBuffer		= new byte[16];
		System.arraycopy(dataBuffer, 14, measurementsBuffer, 0, measurementsBuffer.length);
		values = AkkuMasterC4SerialPort.getConvertedValues(values, AkkuMasterC4SerialPort.convertConfigurationAnswer(configurationBuffer), AkkuMasterC4SerialPort.convertMeasurementValues(measurementsBuffer));


		points[0] = (Integer) values.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE); //Spannung 	[mV]
		points[1] = (Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CURRENT); //Strom 			[mA]
		// display adaption * 1000  -  / 1000
		points[2] = ((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)) * 1000; //Kapazität	[mAh] 
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			
			recordSet.addPoints(points);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
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
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				dataTableRow[index + 1] = record.getDecimalFormat().format(record.realGet(rowIndex) / 1000.0);
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value) { //$NON-NLS-1$
		double newValue = value;
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		log.log(Level.FINEST, String.format("value calculated for %s - %f", record.getName(), newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value) { //$NON-NLS-1$
		double newValue = value;
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		log.log(Level.FINEST, String.format("value calculated for %s - %f", record.getName(), newValue)); //$NON-NLS-1$
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
		recordSet.setAllDisplayable();
	}
	
	/**
	 * function to calculate values for inactive or to be calculated records
	 * @param recordSet
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
				Record record = recordSet.get(3);//3=Leistung/Power
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(record.getName(), new AkkuMasterCalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				record = recordSet.get(4);//4=Energie/Energy
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(record.getName(), new AkkuMasterCalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the device dialog
	 */
	@Override
	public AkkuMasterC4Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the device serialPort
	 */
	@Override
	public AkkuMasterC4SerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[0];
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				Channel activChannel = Channels.getInstance().getActiveChannel();
				if (activChannel != null) {
					if (this.getDialog().isDisposed())
						this.getDialog().open();
					else 
						this.getDialog().startUpdateVersionThread();
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements 
	 */
	@Override
	public int	getCurrentSmoothIndex() {
		return 2;
	}
}
