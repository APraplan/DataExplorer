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
    
    Copyright (c) 2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wb;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

public class Triangoli extends DeviceConfiguration implements IDevice {
	final static Logger									log														= Logger.getLogger(Triangoli.class.getName());

	final DataExplorer								application;
	final Channels										channels;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Triangoli(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = Triangoli.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_OPEN_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Triangoli(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = Triangoli.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_OPEN_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * @return the tool tip text for the import menu bar button
	 */
	public static String getImportToolTip() {
		DeviceConfiguration onlinecommanderConfiguration = Analyzer.getInstance().getDeviceConfigurations().get("Triangoli");
		String fileExtentions = onlinecommanderConfiguration != null ? onlinecommanderConfiguration.getDataBlockPreferredFileExtention() : GDE.STRING_QUESTION_MARK;
		return Messages.getString(MessageIds.GDE_MSGT2754, new Object[] { fileExtentions });
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2755));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2756));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2757));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8) + ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}
			recordSet.addNoneCalculationRecordsPoints(points,
						(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8)	+ ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
		if (recordSet.getRecordSetDescription().contains(GDE.STRING_LEFT_PARENTHESIS))
			recordSet.setRecordSetDescription(this.getName() + recordSet.getRecordSetDescription().substring(recordSet.getRecordSetDescription().lastIndexOf(GDE.STRING_LEFT_PARENTHESIS)));
		else if (recordSet.getRecordSetDescription().contains(GDE.STRING_MESSAGE_CONCAT))
			recordSet.setRecordSetDescription(this.getName() + recordSet.getRecordSetDescription().substring(recordSet.getRecordSetDescription().indexOf(GDE.STRING_MESSAGE_CONCAT)));
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
		     // 0=RfSignal 1=RfSignalStrength
		     // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
		     // 18=LD 19=WindSpeed 20=WindDirection
				switch(ordinal) {
				default:
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
					break;
				case 0: 
					dataTableRow[index + 1] = String.format("%.0f",(record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
					break;
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
		     // 0=RfSignal 1=RfSignalStrength
		     // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
		     // 18=LD 19=WindSpeed 20=WindDirection
				containsGPSdata = activeRecordSet.get(12).hasReasonableData() && activeRecordSet.get(13).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		final int latOrdinal = 12, lonOrdinal = 13;
		return record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 5;

		return this.kmzMeasurementOrdinal;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
		     // 0=RfSignal 1=RfSignalStrength
		     // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
		     // 18=LD 19=WindSpeed 20=WindDirection
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				final int latOrdinal = 12, lonOrdinal = 13, altOrdinal = 6, climbOrdinal = 7;
				exportFileName = new FileHandler().exportFileKMZ(lonOrdinal, latOrdinal, altOrdinal, additionalMeasurementOrdinal, climbOrdinal, -1, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		final int latOrdinal = 12, lonOrdinal = 13, altOrdinal = 6, climbOrdinal = 7, speedOrdinal = 5;
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2753), lonOrdinal, latOrdinal, altOrdinal, speedOrdinal, climbOrdinal, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double newValue = 0;

    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		final int latOrdinal = 12, lonOrdinal = 13;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //13=Latitude, 14=Longitude
			newValue = value / 1000.;
		}
		else {
			double factor = record.getFactor(); // != 1 if a unit translation is required
			double offset = record.getOffset(); // != 0 if a unit translation is required
			double reduction = record.getReduction(); // != 0 if a unit translation is required
			if (record.getOrdinal() == 6 || record.getOrdinal() == 14) {
				PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
				boolean isSubtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
				if (isSubtractFirst) {
					reduction = record.getFirst() / 1000.0;
				}
			}
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
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double newValue = 0;

    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		final int latOrdinal = 12, lonOrdinal = 13;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 12=Latitude, 13=Longitude
			newValue = value * 1000.0;
		}
		else {
			double factor = record.getFactor(); // != 1 if a unit translation is required
			double offset = record.getOffset(); // != 0 if a unit translation is required
			double reduction = record.getReduction(); // != 0 if a unit translation is required
			if (record.getOrdinal() == 6 || record.getOrdinal() == 14) {
				PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
				boolean isSubtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
				if (isSubtractFirst) {
					reduction = record.getFirst() / 1000.0;
				}
			}

			newValue = (value - offset) / factor + reduction;
		}

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {

    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		recordSet.syncScaleOfSyncableRecords();
		this.application.updateStatisticsData();
		this.channels.getActiveChannel().applyTemplate(recordSet.getName(), false);
		this.updateVisibilityStatus(recordSet, true);
		this.application.getActiveChannel().setFileDescription(recordSet.getFormatedTime_sec(0, true).trim().split(GDE.STRING_BLANK)[0]);
		recordSet.setSaved(true);
	}

	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLovDataByteSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {

		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
    // 0=RfSignal 1=RfSignalStrength
    // 2=SattellitesInUse 3=SatFix 4=ServoPulse 5=GroundSpeed 6=Altitude 7=Vario 8=BatteryVoltage 9=ENL 10=Pitch 11=Roll 12=Latitude 13=Longitude 14=GpsAltitude 15=GroundTrack 16=AirSpeed 17=Netto
    // 18=LD 19=WindSpeed 20=WindDirection
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() && record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && (measurement.isActive() || measurement.isCalculation()));
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	@Override
	public void open_closeCommPort() {
		// TODO Auto-generated method stub
		
	}

}
