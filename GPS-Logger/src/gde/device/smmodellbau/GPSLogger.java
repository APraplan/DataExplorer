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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
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
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.io.LogViewReader;
import gde.io.NMEAParser;
import gde.io.NMEAReaderWriter;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

/**
 * GPS-Logger device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class GPSLogger extends DeviceConfiguration implements IDevice {
	final static Logger		log								= Logger.getLogger(GPSLogger.class.getName());

	final static String		SM_GPS_LOGGER_INI				= "SM GPS-Logger.ini";													//$NON-NLS-1$
	final static String		SM_GPS_LOGGER_INI_DIR		= "SM GPS-Logger setup";												//$NON-NLS-1$
	final static String		SM_GPS_LOGGER_DIR_STUB	= "GPS-Logger";																	//$NON-NLS-1$
	static String					selectedSetupFilePath;																									//path to setup ini file

	final DataExplorer		application;
	final Channels				channels;
	final GPSLoggerDialog	dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public GPSLogger(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.gpslogger.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new GPSLoggerDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2004), Messages.getString(MessageIds.GDE_MSGT2004));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public GPSLogger(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.gpslogger.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new GPSLoggerDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2004), Messages.getString(MessageIds.GDE_MSGT2004));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
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
		// prepare the serial CSV data parser
		NMEAParser data = new NMEAParser(this.getDataBlockLeader(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(), Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)), this,
				this.channels.getActiveChannelNumber(), this.getUTCdelta());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		byte[] lineBuffer;
		byte[] subLengthBytes;
		int subLenght;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		try {
			int lastLength = 0;
			for (int i = 0; i < recordDataSize; i++) {
				subLengthBytes = new byte[4];
				System.arraycopy(dataBuffer, lastLength, subLengthBytes, 0, 4);
				subLenght = LogViewReader.parse2Int(subLengthBytes) - 8;
				//System.out.println((subLenght+8));
				lineBuffer = new byte[subLenght];
				System.arraycopy(dataBuffer, 4 + lastLength, lineBuffer, 0, subLenght);
				String textInput = new String(lineBuffer,"ISO-8859-1");
				//System.out.println(textInput);
				StringTokenizer st = new StringTokenizer(textInput);
				Vector<String> vec = new Vector<String>();
				while (st.hasMoreTokens())
					vec.add(st.nextToken("\r\n"));
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
				//CH1-UniLog
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				//CH2-UniLog2
				//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
				//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				
				//begin GDE 3.4.9
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//CH1-UniLog
				//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
				//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
				//CH2-UniLog2
				//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
				//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
				//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
				data.parse(vec, vec.size());
				lastLength += (subLenght+12);

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet, true);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(gde.messages.MessageIds.GDE_MSGW0543);
			log.log(java.util.logging.Level.WARNING, msg, e);
			this.application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		//noop due to previous parsed CSV data
		return points;
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		// check for device file contained record properties which are not contained in actual configuration
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;

		String[] recordKeys = recordSet.getRecordNames();

		StringBuilder sb = new StringBuilder().append(GDE.LINE_SEPARATOR);

		Vector<String> cleanedRecordNames = new Vector<String>();
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original name
		Vector<String> noneCalculationRecordNames = new Vector<String>();

		try {
			switch (fileRecordsProperties.length) {
			case 15: //Android GPS-Logger < 1.5.2
			case 20: //Android GPS-Logger2/3 < 1.5.2
			case 24: //Android GPS-Logger_UL < 1.5.2
			case 29: //Android GPS-Logger2/3_UL < 1.5.2
			case 32: //Android GPS-Logger_UL2 < 1.5.2
			case 37: //Android GPS-Logger2/3_UL2 < 1.5.2
				
			case 17: //Android GPS-Logger >=1.5.2
			case 26: //Android GPS-Logger_UL >=1.5.2 + Android GPS-Logger2/3 >=1.5.2
			case 34: //Android GPS-Logger_UL2 >=1.5.2
			case 35: //Android GPS-Logger2/3_UL >=1.5.2
			case 43: //Android GPS-Logger2/3_UL2 >=1.5.2
				for (int i = 0; i < recordSet.size(); ++i) {
					recordSet.get(i).setName("????"+i); // make names unique to enable update later on
				}
				return super.crossCheckMeasurements(fileRecordsProperties, recordSet);
				
			default: //GDE handling
				if (fileRecordsProperties.length != recordKeys.length) {
					//begin GDE 3.4.9 Ch1=41 measurements, CH2=49 measurements
					//SMGPS  added 15=GlideRatio 16=SpeedGlideRatio;
					for (int i = 0, j = 0; i < recordKeys.length; i++) {
						switch (i) {
						case 15: //GlideRatio
						case 16: //SpeedGlideRatio
							sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
							recordSet.get(i).setActive(null);
							break;
						default:
							if (j < fileRecordsProperties.length) {
								HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
								sb.append(String.format("%02d %19s match %19s isAvtive = %s\n", i, recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
								cleanedRecordNames.add(recordKeys[i]);
								noneCalculationRecordNames.add(recordProps.get(Record.NAME));
								if (fileRecordsProperties[j].contains("_isActive=false")) recordSet.get(i).setActive(false);
								++j;
							}
							else {//some Android saved record sets contain less fileRecordsProperties, mark rest as calculation
								sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
								recordSet.get(i).setActive(null);
							}
							break;
						}
					}
				}
				else { //already adapted record set stored
					for (int i = 0; i < recordKeys.length; i++) {
						if (!fileRecordsProperties[i].contains("_isActive")) {
							sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
							recordSet.get(i).setActive(null);
							cleanedRecordNames.add(recordKeys[i]);
						}
						else {
							HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[i], Record.DELIMITER, Record.propertyKeys);
							sb.append(String.format("%02d %19s match %19s isAvtive = %s\n", i, recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
							cleanedRecordNames.add(recordKeys[i]);
							noneCalculationRecordNames.add(recordProps.get(Record.NAME));
							if (fileRecordsProperties[i].contains("_isActive=false")) recordSet.get(i).setActive(false);
						}
					}
				}
				break;
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, String.format("recordKey to fileRecordsProperties mismatch, check:\n %s \nfileRecordsProperties.length = %d recordKeys.length = %d %s", e.getMessage(),
					fileRecordsProperties.length, recordKeys.length, sb.toString()));
		}

		recordKeys = cleanedRecordNames.toArray(new String[1]);
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original incoming name
		recordSet.setNoneCalculationRecordNames(noneCalculationRecordNames.toArray(new String[1]));

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());

		return recordKeys;
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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
			//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
			//CH1-UniLog
			//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
			//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
			//CH2-UniLog2
			//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
			//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
			
			//begin GDE 3.4.9
			//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
			//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
			//CH1-UniLog
			//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
			//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
			//CH2-UniLog2
			//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
			//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
			//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addNoneCalculationRecordsPoints(points);
			else
				recordSet.addNoneCalculationRecordsPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		this.updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * @param record
	 * @return true if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { 
			// 0=GPS-latitude 1=GPS-longitude 
			return true;
		}
		return false;
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
				//CH1-UniLog
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				//CH2-UniLog2
				//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
				//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				
				//begin GDE 3.4.9
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//CH1-UniLog
				//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
				//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
				//CH2-UniLog2
				//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
				//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
				//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
				if (record.getOrdinal() > 1) {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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

		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		if (record.getOrdinal() == 2 || record.getOrdinal() == 8) { 
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
		}

		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int grad = ((int)(value / 1000));
			double minuten = (value - (grad*1000.0))/10.0;
			newValue = grad + minuten/60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		if (record.getOrdinal() == 2 || record.getOrdinal() == 8) { 
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
		}

		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int grad = (int)value;
			double minuten =  (value - grad*1.0) * 60.0;
			newValue = (grad + minuten/100.0)*1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;
		MeasurementType measurement;
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() && record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				if (log.isLoggable(Level.FINE) && record.hasReasonableData() && measurement.isActive())
					log.log(Level.FINE, record.getName() + " hasReasonableData "); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		//calculate azimuth/track
		Record recordAzimuth = recordSet.get(14);
    //exclude azimuth calculation while opening older OSD files
    if (!recordAzimuth.hasReasonableData() && !recordAzimuth.getName().startsWith("Gl")) { //GlideRatio or Gleitzahl
			recordAzimuth.clear();
			for (Integer value : GPSHelper.calculateAzimuth(this, recordSet, 0, 1, 2)) {
				recordAzimuth.add(value); // use add to fill min/max which get used to detect display able state
			}
			//recordAzimuth.addAll(GPSHelper.calculateAzimuth(this, recordSet, 0, 1, 2));
		}
		//GPSHelper.calculateSpeed2D(this, recordSet, 0, 1, 7);
		//GPSHelper.calculateSpeed3D(this, recordSet, 0, 1, 8, 7);
    //GPSHelper.calculateTripLength(this, recordSet, 0, 1, 8, 0, 11);

		this.application.updateStatisticsData();
	}

	/**
	 * @return the dialog
	 */
	@Override
	public GPSLoggerDialog getDialog() {
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
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2000));

		Thread reader = new Thread("reader"){
			@Override
			public void run() {
				try {
					GPSLogger.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_NMEA)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.CHAR_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_NMEA;
						}
						log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								Integer channelConfigNumber = application.getActiveChannelNumber(); 
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
									//check for GPS-Logger containing $UL2
									DataInputStream binReader = new DataInputStream(new FileInputStream(selectedImportFile));
									byte[] buffer = new byte[1024];
									int numBytes = binReader.read(buffer);
									while (numBytes > 0 && !new String(buffer).contains("$UL2") && !new String(buffer).contains("$UNILOG")) {
										numBytes = binReader.read(buffer);
									}
									if (numBytes > 0 && new String(buffer).contains("$UL2")) {
										channelConfigNumber = 2;//channelCongig 2 : UniLog2
										GPSLogger.this.channels.switchChannel(channelConfigNumber, GDE.STRING_EMPTY); //channelCongig 2 : UniLog2
									}
									else if (numBytes > 0 && new String(buffer).contains("$UNILOG")) {
										channelConfigNumber = 1;//channelCongig 1 : UniLog
										GPSLogger.this.channels.switchChannel(channelConfigNumber, GDE.STRING_EMPTY); //channelCongig 1 : UniLog
									}
									binReader.close();

								NMEAReaderWriter.read(selectedImportFile, GPSLogger.this, recordNameExtend, channelConfigNumber);
							}
							catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					GPSLogger.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZ3DAbsoluteItem;
		MenuItem convertGPXItem;
		MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2005));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2006));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2007));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			convertGPXItem = new MenuItem(exportMenue, SWT.PUSH);
			convertGPXItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0728));
			convertGPXItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertGPXItem action performed! " + e); //$NON-NLS-1$
					export2GPX(false);
				}
			});

			if (this.getName().endsWith("2") || this.getName().endsWith("3")) { //GPS-Logger2/3
				convertGPXGarminItem = new MenuItem(exportMenue, SWT.PUSH);
				convertGPXGarminItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0729));
				convertGPXGarminItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						log.log(java.util.logging.Level.FINEST, "convertGPXGarminItem action performed! " + e); //$NON-NLS-1$
						export2GPX(true);
					}
				});
			}
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2KMZ3D(int type) {
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2003), 1, 0, 2, 7, 9, 11, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2GPX(final boolean isGarminExtension) {
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		if (isGarminExtension)
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 	0, 1, 2, 7, 3, 5, 6, 4, new int[] {15,16,17});
		else
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 	0, 1, 2, 7, 3, 5, 6, 4, new int[0]);
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
				//CH1-UniLog
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				//CH2-UniLog2
				//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
				//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				
				//begin GDE 3.4.9
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//CH1-UniLog
				//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
				//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
				//CH2-UniLog2
				//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
				//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
				//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
				containsGPSdata = activeRecordSet.get(0).hasReasonableData() && activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(2).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExportTmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
				//CH1-UniLog
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				//CH2-UniLog2
				//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
				//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				
				//begin GDE 3.4.9
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//CH1-UniLog
				//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
				//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
				//CH2-UniLog2
				//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
				//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
				//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(1, 0, 2, additionalMeasurementOrdinal, 9, 11, -1, true, isExportTmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 7;

		return this.kmzMeasurementOrdinal;
	}
		
	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth;
		//CH1-UniLog
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		//CH2-UniLog2
		//Unilog2 15=voltage_UL 16=current_UL2 17=capacity_UL2 18=power_UL2 19=energy_UL2 20=balance_UL 21=cellVoltage1 22=cellVolt2_ul 23=cellVolltage3_UL 24=cellVoltage4_UL 25=cellVoltage5_UL 26=cellVoltage6_UL 27=revolution_UL 28=a1_UL 29=a2_UL 30=a3_UL 31=temp_UL;
		//M-LINK 32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
		
		//begin GDE 3.4.9
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//CH1-UniLog
		//Unilog 17=voltageUniLog 18=currentUniLog 19=powerUniLog 20=revolutionUniLog 21=voltageRxUniLog 22=heightUniLog 23=a1UniLog 24=a2UniLog 25=a3UniLog;
		//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 33=valAdd07 34=valAdd08 35=valAdd09 36=valAdd10 37=valAdd11 38=valAdd12 39=valAdd13 40=valAdd14;
		//CH2-UniLog2
		//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
		//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
		//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
		Record recordLatitude = recordSet.get(0);
		Record recordLongitude = recordSet.get(1);
		Record baroAlitude = recordSet.get(8);
		Record gpsAlitude = recordSet.get(2);
		
		return String.format("%02d%05d%s%03d%05d%s%c%05d%05d", 																																														//$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S",//$NON-NLS-1$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W",//$NON-NLS-1$
				fixValidity, Double.valueOf(baroAlitude.get(index) / 10000.0 + startAltitude + offsetAltitude).intValue(), Double.valueOf(gpsAlitude.get(index) / 1000.0 + offsetAltitude).intValue());
	}

	String getDefaultConfigurationFileName() {
		return GPSLogger.SM_GPS_LOGGER_INI;
	}
	
	String getConfigurationFileDirecotry() {
		if (GPSLogger.selectedSetupFilePath == null) {
			String searchPath = GDE.OBJECT_KEY == null 
					? this.getDataBlockPreferredDataLocation().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX)
					: FileUtils.getDeviceImportDirectory(this);
			if (searchPath.contains(GPSLogger.SM_GPS_LOGGER_DIR_STUB)) {
				searchPath = searchPath.substring(0, searchPath.indexOf(GPSLogger.SM_GPS_LOGGER_DIR_STUB)) + GPSLogger.SM_GPS_LOGGER_INI_DIR;
			}
			else {
				String dataFilePath = Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX;
				if (searchPath.equals(dataFilePath)) {
					searchPath = searchPath + this.getName() + GDE.STRING_FILE_SEPARATOR_UNIX;
				}

				if (searchPath.endsWith(GDE.STRING_FILE_SEPARATOR_UNIX))
					searchPath = searchPath + GPSLogger.SM_GPS_LOGGER_INI_DIR;
				else 
					searchPath = searchPath + GDE.STRING_FILE_SEPARATOR_UNIX + GPSLogger.SM_GPS_LOGGER_INI_DIR;
			}
			return searchPath;
		}
		return GPSLogger.selectedSetupFilePath.substring(0, GPSLogger.selectedSetupFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2008, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2008));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}
	
	/**
	 * get the measurement ordinal of altitude, speed and trip length
	 * @return empty integer array if device does not fulfill complete requirement
	 */
	@Override
	public int[] getAtlitudeTripSpeedOrdinals() { 
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track;
		return new int[] { 8, 11, 7}; 
	}  
	
	/**
	 * cross check and update given channel configuration to drive correct selection, p.e. if simplified OSD stored with Android version
	 * @param channelConfig "1 : UniLog2"
	 * @param recordSetInfo map containing actual recordSet related information
	 * @return new signature of channelConfiguration "2 : UniLog2"
	 */
	public String crossCheckChannelConfig(String channelConfig, HashMap<String, String> recordSetInfo) { 
		if (channelConfig.endsWith("UniLog2")) {
			String updateChannelConfig = "2 : UniLog2";
			recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, updateChannelConfig);
			return updateChannelConfig; 
		}
		return channelConfig;
	}

}
