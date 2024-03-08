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

    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
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
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.SerialPortException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * @author brueg
 *
 */
public class Pulsar3 extends DeviceConfiguration implements IDevice {
	final static Logger	log	= Logger.getLogger(Pulsar3.class.getName());

	protected final Settings					settings			= Settings.getInstance();
	protected final DataExplorer			application		= DataExplorer.getInstance();
	protected final Channels					channels			= Channels.getInstance();
	protected final PulsarSerialPort	serialPort;
	protected PulsarGathererThread		gathererThread;
	protected boolean									isFileIO			= false;
	protected boolean									isSerialIO		= false;
	protected Map<Integer, String>		batteryTypes	= new HashMap<Integer, String>();

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Pulsar3(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.elprog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3903), Messages.getString(MessageIds.GDE_MSGT3903));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		this.serialPort = new PulsarSerialPort(this, this.application);
		initBatteryTypes();
		LogViewReader.putDeviceMap("pulsar 3", "Pulsar3"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Pulsar3(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.elprog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3903), Messages.getString(MessageIds.GDE_MSGT3903));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		this.serialPort = new PulsarSerialPort(this, this.application);
		initBatteryTypes();
		LogViewReader.putDeviceMap("pulsar 3", "Pulsar3"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * initialize known battery types
	 */
	protected void initBatteryTypes() {
		batteryTypes.put(0, "NiCd");
		batteryTypes.put(1, "NiMH");
		batteryTypes.put(4, "LiIo");
		batteryTypes.put(5, "LiPo");
		batteryTypes.put(7, "LiFe");
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT3904, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT3904));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					if (!isSerialIO) open_closeCommPort();
					else importDeviceData();
				}
			});
		}
	}

	/**
	 * import device specific *.acp data files
	 */
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3900));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							String recordNameExtend = GDE.STRING_EMPTY;
							try {
								BufferedReader reader = null;
								try {
									reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedImportFile), "ISO-8859-1")); //$NON-NLS-1$
									String line = reader.readLine();
									if (line.startsWith("#")) {
										recordNameExtend = batteryTypes.get(Integer.valueOf(line.substring(4, 6)));
										recordNameExtend = recordNameExtend == null ? GDE.STRING_EMPTY : recordNameExtend;
									}
									reader.close();
								}
								catch (Exception e) {
									//ignore and use empty string as battery type since it is unknown
									if (reader != null) reader.close();
								}
								CSVSerialDataReaderWriter.read(selectedImportFile, Pulsar3.this, recordNameExtend, 1,
										new  PulsarDataParser(getDataBlockTimeUnitFactor(), getDataBlockLeader(), getDataBlockSeparator().value(), null, null, Math.abs(getDataBlockSize(InputTypes.FILE_IO)), getDataBlockFormat(InputTypes.FILE_IO), false, 2)
								);
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					application.setPortConnected(false);
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
		return GDE.STRING_BLANK;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
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
	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = this.getLovDataByteSize();
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize);
			//no cell internal Ri stored in LogView files
			recordSet.addPoints(convertDataBytes(points, convertBuffer));
			offset += lovDataSize+10;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {

		PulsarDataParser data = new PulsarDataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockLeader(), this.getDataBlockSeparator().value(), null, null,
				Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)), this.getDataBlockFormat(InputTypes.SERIAL_IO), false);

		try {
			//System.out.println(new String(dataBuffer));
			byte[] lineBuffer = new byte[Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO))];
			System.arraycopy(dataBuffer, 0, lineBuffer, 0, dataBuffer.length);
			data.parse(new String(lineBuffer), 1);
		}
		catch (DevicePropertiesInconsistenceException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return data.getValues(points);
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param doUpdateProgressBar
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize;
			if (Pulsar3.log.isLoggable(Level.FINER))
				Pulsar3.log.log(Level.FINER, i + " i*dataBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

				recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	* function to prepare a data table row of record set while translating available measurement values
	* @return pointer to filled data table row with formated values
	*/
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
		//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if(record.getOrdinal() > 7 && record.getUnit().equals("V"))
					try {
						dataTableRow[index + 1] = String.format("%.3f", ((record.realGet(rowIndex) / 1000.0) * factor)); //$NON-NLS-1$
					}
					catch (Exception e) {
						dataTableRow[index + 1] = String.format("%.3f", ((record.realGet(record.realSize()-1) / 1000.0) * factor)); //$NON-NLS-1$
					}
				else
					try {
						dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(rowIndex) / 1000.0) * factor));
					}
					catch (Exception e) {
						dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(record.realSize()-1) / 1000.0) * factor));
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
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	@Override
	public double translateValue(Record record, double value) {
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
		//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
		//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive record which need to be calculated
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
				//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
				//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
				//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
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
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int displayableCounter = 0;
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
		//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
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
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.gathererThread = new PulsarGathererThread(this.application, this, this.serialPort, activChannel.getNumber());
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
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=RiZelle1 25=RiZelle2 26=RiZelle3 27=RiZelle4 28=RiZelle5 29=RiZelle6 ... 39=RiZelle16
		//40=BalancerZelle1 41=BalancerZelle2 42=BalancerZelle3 43=BalancerZelle4 44=BalancerZelle5 45=BalancerZelle6 ... 55=BalancerZelle16
		return new int[] {0, 2};
	}

	/**
	 * query the process name according defined states
	 * @param buffer
	 * @return
	 */
	public String getProcessName(byte[] buffer) throws Exception {
		//#03C05____B4,00070,11625,06572,000,00064,00037,3879,0,3887,0,3880,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0
		return String.format("%s [%s]", this.getRecordSetStateNameReplacement(buffer[3]), batteryTypes.get(Integer.valueOf(new String(buffer).substring(4, 6))));
	}

	/**
	 * @param buffer
	 * @return true|false depending on program finish or manual finish
	 */
	public boolean isProcessing(byte[] buffer) {
		//detect program ending
		//#03D05__M_B4 -> manual ending
		//#03C05__EPB4 -> program finished
		//#04D01__EE_0 -> program cycle ended
		switch ((char) buffer[8]) {
		case 'M': //manual finish
		case 'E': //program finished
			return false;

		case '_':
		default:
			return true;
		}
	}
}
