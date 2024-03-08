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
package gde.device.junsi;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Junsi iCharger base device class
 * @author Winfried Brügmann
 */
public abstract class iCharger extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(iCharger.class.getName());

	public final static String		CONFIG_EXT_TEMP_CUT_OFF			= "ext_temp_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_WAIT_TIME						= "wait_time"; //$NON-NLS-1$
	public final static String		CONFIG_IN_VOLTAGE_CUT_OFF		= "in_voltage_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_SAFETY_TIME					= "safety_time"; //$NON-NLS-1$
	public final static String		CONFIG_SET_CAPASITY					= "capacity_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING						= "processing"; //$NON-NLS-1$
	public final static String		CONFIG_BATTERY_TYPE					= "battery_type"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING_TIME			= "processing_time"; //$NON-NLS-1$

	protected final DataExplorer									application;
	protected   		iChargerSerialPort						serialPort;
	protected final Channels											channels;
	protected       GathererThread								gathererThread;
	protected String[]														BATTERIE_TYPES;
	
	protected static double energySum = 0.0; 

	/**
	 * @param batTypeName LiPo, LiIo, LiFe,...
	 * @return index according device specific enumeration
	 */
	public int getBatTypeIndex(String batTypeName) {	
		for (int i=0; i<this.BATTERIE_TYPES.length; ++i) {
			if (this.BATTERIE_TYPES[i].toLowerCase().equals(batTypeName.toLowerCase()))
				return i;
		}
		return -1;
	}

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public iCharger(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new iChargerSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null)
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public iCharger(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		if (deviceConfig.getSerialPortType() != null) {
			this.serialPort = new iChargerSerialPort(this, this.application);
		}
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null)
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
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
		return 158;
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
		//device specific implementation required
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		//device specific implementation required
		return points;
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
		byte leader = this.getDataBlockLeader().getBytes()[0];
		byte delimiter = this.getDataBlockSeparator().value().getBytes()[0];
		byte[] lineSep = this.getDataBlockEnding();

		for (; startPos < dataBuffer.length; ++startPos) {
			if (dataBuffer[startPos] == leader) {
				if (dataBuffer[startPos + 2] == delimiter || dataBuffer[startPos + 3] == delimiter) break; // "$ ;" or "$  ;" (record set number two digits
			}
		}
		int crlfPos = refStartLength[0] = startPos;

		for (; crlfPos < dataBuffer.length; ++crlfPos) {
			if (dataBuffer[crlfPos] == lineSep[0] || dataBuffer[crlfPos + 1] == lineSep[1]) break; //0d0a (CRLF)
		}
		refStartLength[1] = crlfPos - startPos;
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
		//device specific implementation required
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if(record.getOrdinal() >= 9 && record.getUnit().equals("V"))
					try {
						dataTableRow[index + 1] = String.format("%.3f", (((record.realGet(rowIndex) / 1000.0) - reduction) * factor)); //$NON-NLS-1$
					}
					catch (Exception e) {
						dataTableRow[index + 1] = String.format("%.3f", (((record.realGet(record.realSize()-1) / 1000.0) - reduction) * factor)); //$NON-NLS-1$
					}
				else
					try {
						dataTableRow[index + 1] = record.getDecimalFormat().format((((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
					}
					catch (Exception e) {
						dataTableRow[index + 1] = record.getDecimalFormat().format((((record.realGet(record.realSize()-1) / 1000.0) - reduction) * factor));
					}
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
		//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor + offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
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

		//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
		recordSet.setAllDisplayable();
		for (int i=7; i<recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData()); //$NON-NLS-1$

				if (record.isActive() && record.isDisplayable()) {
					++displayableCounter;
				}
		}

		if (log.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
				//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
				int displayableCounter = 0;


				// check if measurements isActive == false and set to isDisplayable == false
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					if (record.isActive() && record.hasReasonableData()) {
						++displayableCounter;
					}
				}

				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public iChargerSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.gathererThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber());
						try {
							if (this.serialPort.isConnected()) {
								this.gathererThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.gathererThread != null) {
					this.gathererThread.stopDataGatheringThread(false, null);
				}
				this.serialPort.close();
			}
		}
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10
		return new int[] {1, 3};
	}

	/**
	 * query the process name according defined states
	 * @param buffer
	 * @return
	 */
	public String getProcessName(byte[] buffer) throws Exception {
		return this.getRecordSetStateNameReplacement(Integer.parseInt((new String(buffer).split(this.getDataBlockSeparator().value())[1])));
	}

	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	public abstract int getNumberOfLithiumCells();

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

