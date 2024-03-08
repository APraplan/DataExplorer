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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Graupner Ultramat base class
 * @author Winfried Brügmann
 */
public abstract class Ultramat extends DeviceConfiguration implements IDevice {
	final static Logger	log	= Logger.getLogger(Ultramat.class.getName());

	public enum GraupnerDeviceType {
		//0=Ultramat50, 1=UltraDuoPlus40, 2=UltramatTrio14, 3=Ultramat18 4=Ultramat45, 5=Ultramat60, 6=UltramatTrioPlus16S ?=Ultramat12 ?=Ultramat16 ?=Ultramat16S
		UltraDuoPlus50, UltraDuoPlus40, UltraTrioPlus14, Ultramat18, UltraDuoPlus45, UltraDuoPlus60, UltraTrioPlus16S, Ultramat16S, UltraDuoPlus80, UltraQuick70, UltraAcDcEq
	};

	protected String[]														USAGE_MODE;
	protected String[]														CHARGE_MODE;
	protected String[]														DISCHARGE_MODE;
	protected String[]														DELAY_MODE;
	protected String[]														CURRENT_MODE;
	protected String[]														ERROR_MODE;

	protected static final String									OPERATIONS_MODE_LINK_DISCHARGE	= "05";																		//$NON-NLS-1$
	protected static final String									OPERATIONS_MODE_LINK_CHARGE			= "09";																		//$NON-NLS-1$
	protected static final String									OPERATIONS_MODE_ERROR						= "06";																		//$NON-NLS-1$
	protected static final String									OPERATIONS_MODE_NONE						= "00";																		//$NON-NLS-1$

	protected Schema															schema;
	protected JAXBContext													jc;
	protected UltraDuoPlusType										ultraDuoPlusSetup;
	protected String															firmware												= GDE.STRING_MINUS;
	protected GathererThread											dataGatherThread;
	protected HashMap<String, CalculationThread>	calculationThreads							= new HashMap<String, CalculationThread>();

	protected final Settings settings = Settings.getInstance();
	protected final DataExplorer									application;
	protected final UltramatSerialPort						serialPort;
	protected final Channels											channels;
	protected UltraDuoPlusDialog									dialog;

	public static final String[]									cycleDataRecordNames						= Messages.getString(gde.messages.MessageIds.GDE_MSGT0398).split(GDE.STRING_COMMA);
	public static final String[]									cycleDataUnitNames							= { "V", "V", "mAh", "mAh", "m\u2126", "m\u2126" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	public static final String[]									cycleDataTableNames							= Messages.getString(gde.messages.MessageIds.GDE_MSGT0399).split(GDE.STRING_COMMA);
	public static final double[]									cycleDataFactors								= { 1.0, 1.0, 1000.0, 1000.0, 100.0, 100.0 };
	public static final int[]											cycleDataSyncRefOrdinal					= { -1, 0, -1, 2, -1, 4 };
	public static final int[][]										cycleDataColors									= { {0,0,255},{12,12,255}, {128,0,0},{128,12,12},  {255,0,0},{255,12,12}};

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Ultramat(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new UltramatSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = new UltraDuoPlusDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Ultramat(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new UltramatSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = new UltraDuoPlusDialog(this.application.getShell(), this);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
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
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return GDE.STRING_EMPTY;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 150;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real log, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException
	 */
	@Override
	public abstract void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException;

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public abstract int[] convertDataBytes(int[] points, byte[] dataBuffer);

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
	public abstract void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException;

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BAtterietemperatur 6=VersorgungsSpg 7=Balance
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(rowIndex) / 1000.0) * factor));
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
	@Override
	public double translateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance
		// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6
		// 13=SpannungZelle7 14=SpannungZelle8 15=SpannungZelle9 16=SpannungZelle10 17=SpannungZelle11 18=SpannungZelle12
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor + offset;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance
		// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6
		// 13=SpannungZelle7 14=SpannungZelle8 15=SpannungZelle9 16=SpannungZelle10 17=SpannungZelle11 18=SpannungZelle12
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	@Override
	public abstract void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck);

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {

		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
				int displayableCounter = 0;


				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 5 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				Record record = recordSet.get(3);//3=Leistung
				if (record != null && (record.size() == 0 || !record.hasReasonableData())) {
					this.calculationThreads.put(record.getName(), new CalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;

				record = recordSet.get(4);//4=Energie
				if (record != null && (record.size() == 0 || !record.hasReasonableData())) {
					this.calculationThreads.put(record.getName(), new CalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;

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
	public UltramatSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * @return the device specific dialog instance
	 */
	@Override
	public UltraDuoPlusDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
					this.serialPort.write(UltramatSerialPort.RESET);
					try {
						byte[] dataBuffer = this.serialPort.getData(false);
						this.firmware = this.getFirmwareVersion(dataBuffer);
						//check if device fits this.device.getProductCode(dataBuffer)
						if (!this.getDeviceTypeIdentifier().name().equals(this.getClass().getSimpleName()) && this.getDeviceTypeIdentifier().ordinal() != this.getProductCode(dataBuffer)) {
							int answer = this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW2202, new String[] {GraupnerDeviceType.values()[this.getProductCode(dataBuffer)].toString()}));
							if (answer == SWT.YES) {
								this.application.getDeviceSelectionDialog().setupDevice(GraupnerDeviceType.values()[this.getProductCode(dataBuffer)].toString());
								this.serialPort.close();
								return;
							}
						}

						//load cached memory configurations to enable memory name to object key match
						switch (this.getDeviceTypeIdentifier()) {
						case UltraDuoPlus40:
						case UltraDuoPlus45:
						case UltraDuoPlus50:
						case UltraDuoPlus60:
							try {
								if (!(this.isProcessing(1, dataBuffer) || this.isProcessing(2, dataBuffer))) {
									this.serialPort.write(UltramatSerialPort.RESET_CONFIG);
									String deviceIdentifierName = this.serialPort.readDeviceUserName();
									this.serialPort.write(UltramatSerialPort.RESET);

									this.jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$
									this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
											new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$
									Unmarshaller unmarshaller = this.jc.createUnmarshaller();
									unmarshaller.setSchema(this.schema);
									this.ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(Settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
											+ deviceIdentifierName.replace(GDE.CHAR_BLANK, GDE.CHAR_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
								}
							}
							catch (Exception e) {
								if (e instanceof FileNotFoundException) {
									this.application.openMessageDialog(e.getLocalizedMessage());
								}
								log.log(Level.WARNING, e.getMessage(), e);
							}
							break;
						default:
							break;
						}
					}
					catch (FileNotFoundException e) {
						if (this.serialPort.isConnected()) this.serialPort.write(UltramatSerialPort.RESET);
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					}
					catch (SerialPortException e) {
						if (this.serialPort.isConnected()) this.serialPort.write(UltramatSerialPort.RESET);
						throw e;
					}
					catch (TimeOutException e) {
						if (this.serialPort.isConnected()) this.serialPort.write(UltramatSerialPort.RESET);
						throw new SerialPortException(e.getMessage());
					}
					catch (Exception e) {
						if (this.serialPort.isConnected()) this.serialPort.write(UltramatSerialPort.RESET);
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						throw e;
					}

					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new GathererThread();
						try {
							if (this.serialPort.isConnected()) {
								this.dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (ApplicationConfigurationException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
				}
			}
			else {
				if (this.dataGatherThread != null) {
					try {
						this.dataGatherThread.stopDataGatheringThread(false, null);
					}
					catch (Exception e) {
						// ignore, while stopping no exception will be thrown
					}
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
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		return new int[] { 0, 2 };
	}

	/**
	 * query if the target measurement reference ordinal used by the given desktop type
	 * @return the target measurement reference ordinal, -1 if reference ordinal not set
	 */
	@Override
	public int getDesktopTargetReferenceOrdinal(DesktopPropertyTypes desktopPropertyType) {
		DesktopPropertyType property = this.getDesktopProperty(desktopPropertyType);
		return property != null ? property.getTargetReferenceOrdinal() : -1;
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, 6=Ultramat16S ?=Ultramat16
	 */
	public abstract GraupnerDeviceType getDeviceTypeIdentifier();

	/**
	 * query the firmware version
	 * @param dataBuffer
	 * @return v2.0
	 */
	public String getFirmwareVersion(byte[] dataBuffer) {
		return String.format("v%.2f", (Integer.parseInt(String.format("%c%c", (char) dataBuffer[1], (char) dataBuffer[2]), 16) / 100.0)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * query the product code 0=Ultramat50, 1=Ultramat40, 2=Ultramat14Trio, 3=Ultramat18, 4=ultramat45, 5=Ultramat60, 6=Ultramat80
	 * @param dataBuffer
	 * @return v2.0
	 */
	public int getProductCode(byte[] dataBuffer) {
		//1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, ?=Ultramat16S
		return Integer.parseInt(String.format("%c%c", (char) dataBuffer[3], (char) dataBuffer[4]), 16); //$NON-NLS-1$
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1
	 * @param dataBuffer
	 * @return true if channel 1 is active
	 */
	public abstract boolean isProcessing(int outletNum, byte[] dataBuffer);

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * @param dataBuffer
	 * @return 0 = no processing, 1 = charge, 2 = discharge, 3 = pause, 4 = current operation finished, 5 = error
	 */
	public abstract int getProcessingMode(byte[] dataBuffer);

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	public String getProcessingType(byte[] dataBuffer) {
		return GDE.STRING_EMPTY;
	}

	/**
	 * find best match of memory name with object key and select, if no match no object key will be changed
	 * @param batteryMemoryName
	 * @return
	 */
	public void matchBatteryMemory2ObjectKey(String batteryMemoryName) {
		Object[] tmpResult = null;
		for (String tmpObjectKey : this.application.getObjectKeys()) {
			if (tmpObjectKey.equals(batteryMemoryName)) {
				tmpResult = new Object[] { tmpObjectKey, 100 };
				break;
			}
			String[] batteryNameParts = batteryMemoryName.split(" |-|_"); //$NON-NLS-1$
			int hitCount = 0;
			for (String namePart : batteryNameParts) {
				if (namePart.length() > 1 && tmpObjectKey.contains(namePart)) ++hitCount;
			}
			if (hitCount > 0) {
				if (tmpResult == null || hitCount > (Integer) tmpResult[1]) {
					tmpResult = new Object[] { tmpObjectKey, hitCount };
					log.log(java.util.logging.Level.FINE, "result updated = " + tmpObjectKey + " hitCount = " + hitCount); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		if (tmpResult != null) {
			this.application.selectObjectKey((String) tmpResult[0]);
		}
	}

	/**
	 * query the battery memory number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public int getBatteryMemoryNumber(int outletNum, byte[] dataBuffer) {
		return -1;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public abstract int getCycleNumber(int outletNum, byte[] dataBuffer);

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	public boolean isLinkedMode(byte[] dataBuffer) {
		return false;
	}

	/**
	 * set the temperature unit right after creating a record set according to the used oultet channel
	 * @param channelNumber
	 * @param recordSet
	 * @param dataBuffer
	 */
	public void setTemperatureUnit(int channelNumber, RecordSet recordSet, byte[] dataBuffer) {
		//no temperature available
	}

	/**
	 * convert a setup string to integer values array
	 * @param values as integer array to have the size information
	 * @param setupString to be converted
	 * @return
	 */
	public synchronized int[] convert2IntArray(int[] values, String setupString) {
		for (int i = 0; i < values.length; i++) {
			values[i] = Integer.parseInt(
					String.format(DeviceCommPort.FORMAT_4_CHAR, setupString.charAt(i * 4), setupString.charAt(i * 4 + 1), setupString.charAt(i * 4 + 2), setupString.charAt(i * 4 + 3)), 16);
		}
		return values;
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
