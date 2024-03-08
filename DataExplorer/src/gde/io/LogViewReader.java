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
package gde.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import gde.Analyzer;
import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;


/**
 * @author Winfried Brügmann
 * This class reads and writes LogView file format
 */
public class LogViewReader {
	final static Logger					log					= Logger.getLogger(LogViewReader.class.getName());

	final static DataExplorer		application	= DataExplorer.getInstance();
	final static Channels 			channels 		= Channels.getInstance();
	public static class LogViewDeviceMap extends HashMap<String, String> {
		private static final long	serialVersionUID	= 1L;

		public boolean containsKey(String key) {
			String openFormat = "openformat";
			String jlog2 = "jlog2";
			if (key.startsWith(openFormat)) {
				if (key.contains(jlog2)) {
					for (String tmpKey : this.keySet()) {
						if (tmpKey.startsWith(openFormat) && tmpKey.contains("jlog2"))
							return super.containsKey(openFormat + "\\" + jlog2);
					}
				}
			}
			return super.containsKey(key);
		}
		 public String get(String key) {
				String openFormat = "openformat";
				String jlog2 = "jlog2";
				if (key.startsWith(openFormat)) {
					if (key.contains(jlog2)) {
						for (String tmpKey : this.keySet()) {
							if (tmpKey.startsWith(openFormat) && tmpKey.contains("jlog2"))
								return super.get(openFormat + "\\" + jlog2);
						}
					}
				}
				return super.get(key);
			}
	}
	final static LogViewDeviceMap 	deviceMap		=	new LogViewDeviceMap();
	final static HashMap<String, String> 	lov2osdMap	=	new HashMap<String, String>();

	// fill device Map with
	static {
		deviceMap.put("htronic akkumaster c4", "AkkuMasterC4"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("akkumatik", "Akkumatik"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("picolario", "Picolario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station 902", "eStation902"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc6", "eStationBC6"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc6dx", "eStationBC6 80W"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc610", "eStationBC610"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc8", "eStationBC8"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("pichler p6", "PichlerP6"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("imax b6", "iMax B6"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("pichler p60", "PichlerP60 50W"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("pichler p6 80w", "PichlerP60 80W"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("pichler p60 80w 220v", "PichlerP60 80W"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech datavario", "DataVario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech datavario duo", "DataVarioDuo"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech linkvario", "LinkVario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech linkvario duo", "LinkVarioDuo"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("qc copter", "QC-Copter"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("unilog", "UniLog"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm unilog", "UniLog"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm unilog 2", "UniLog2"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm jlog2", "JLog2"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("lipowatch", "LiPoWatch"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm lipowatch", "LiPoWatch"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm gps logger", "GPS-Logger"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("nmea 0183", "NMEA-Adapter"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 40", "UltraDuoPlus40"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 45", "UltraDuoPlus45"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 50", "UltraDuoPlus50"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 60", "UltraDuoPlus60"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 80", "UltraDuoPlus80"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("robbe powerpeak twin 1000w", "Robbe Powerpeak Twin 1000W"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("robbe powerpeak iv", "Robbe PowerPeak IV"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("megapower gemini 2014 duo2", "UltraDuoPlus80"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultramat 18", "Ultramat18"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultramat 12 plus pocket", "Ultramat12"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultramat 16", "Ultramat16"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultramat 16s", "Ultramat16S"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra trio plus 14", "UltraTrioPlus14"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra trio plus 16", "UltraTrioPlus16S"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 106b", "iCharger106B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 1010b", "iCharger1010B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 206b", "iCharger206B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 208b", "iCharger208B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 306b", "iCharger306B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 3010b", "iCharger3010B"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 308 duo", "iCharger308DUO"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi icharger 4010 duo", "iCharger4010DUO"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("junsi celllog 8s", "CellLog 8S"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("openformat\\jlog2", "JLog2"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("openformat\\kosmik", "Kosmik"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("openformat\\av4ms_fv_762", "AV4ms_FV_762"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("openformat\\usb-wde1", "USB-WDE1"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("elv usb-wde1", "USB-WDE1"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("schulze next 10.36 car", "next 10.36-8"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("schulze next 10.36", "next 10.36-8"); //$NON-NLS-1$ //$NON-NLS-2$
		
		// add more supported devices here, key in lower case
	}

	/**
	 * enable adding entries to LogViewDeviceMap on device level
	 */
	public static String putDeviceMap(String key, String value) {
		return deviceMap.put(key, value);
	}


	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static RecordSet read(String filePath) throws Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		String channelConfig = GDE.STRING_EMPTY;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetComment = GDE.STRING_EMPTY;
		String recordSetProperties = GDE.STRING_EMPTY;
		String[] recordsProperties;
		int recordDataSize = 0;
		int recordSetDataBytes = 0, lastRecordSetDataBytes = 0;
		long recordSetDataPointer;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = LogViewReader.application.getActiveDevice();
		boolean isFirstRecordSetDisplayed = false;

		device.getLovKeyMappings(lov2osdMap);

		HashMap<String, String> header = readHeader(data_in);
		int channelNumber = device.recordSetNumberFollowChannel() ? Integer.valueOf(header.get(GDE.CHANNEL_CONFIG_NUMBER)).intValue() : channels.getActiveChannelNumber();
		ChannelTypes channelType = device.getChannelTypes(channelNumber);
		//String channelConfigName = channelType.equals(ChannelTypes.TYPE_OUTLET.name()) ? device.getChannelName(channelNumber) : header.get(GDE.CHANNEL_CONFIG_NAME);
		String channelConfigName = device.getChannelNameReplacement(channelNumber);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "channelConfigName = " + channelConfigName + " (" + GDE.CHANNEL_CONFIG_TYPE + channelType + "; " + GDE.CHANNEL_CONFIG_NUMBER + channelNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		header.put(GDE.CHANNEL_CONFIG_TYPE, channelType.name());
		header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
		//header.put(GDE.RECORD_SET_DATA_POINTER, GDE.STRING_EMPTY+position);
		//header.containsKey(LOV_NUM_MEASUREMENTS)

		// record sets with it properties
		int numberRecordSets = Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
		List<HashMap<String,String>> recordSetsInfo = new ArrayList<HashMap<String,String>>(numberRecordSets);
		for (int i=1; i<=numberRecordSets; ++i) {
			// RecordSetName : 1) Flugaufzeichnung||::||RecordSetComment : empfangen: 12.05.2006, 20:42:52||::||RecordDataSize : 22961||::||RecordSetDataBytes : 367376
			String recordSetInfo = header.get((i)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME);
			recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + GDE.CHANNEL_CONFIG_NAME + header.get(GDE.CHANNEL_CONFIG_NAME);
			if (header.containsKey(RecordSet.TIME_STEP_MS)) {
				// do not use LogView time
				//recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL + header.get(RecordSet.TIME_STEP_MS);
				recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL +  device.getTimeStep_ms();
			}
			else { // format version 1.1x dos not have this info in  file (no devices with variable time step supported)
				recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL +  device.getTimeStep_ms();
			}
			// append record properties if any available
			recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER;
			recordSetInfo = recordSetInfo + device.getConvertedRecordConfigurations(header, lov2osdMap, channelNumber);
			recordSetsInfo.add(getRecordSetProperties(recordSetInfo));
		}

		try { // build the data structure
			long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS).trim());

			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
				//recordDataSize = new Long(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).longValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channelNumber);
				if (channel == null) { // channelConfiguration not found
					String msg = Messages.getString(MessageIds.GDE_MSGI0018, new Object[] { recordSetName }) + " " + Messages.getString(MessageIds.GDE_MSGI0019) + "\n" + Messages.getString(MessageIds.GDE_MSGI0020);
					DataExplorer.getInstance().openMessageDialogAsync(msg);
					channel = channels.addChannel(channelConfig, channelType, Analyzer.getInstance());
				}
				recordSet = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true, true);
				//apply record sets properties
				recordSet.setRecordSetDescription(recordSetComment);
				recordSet.setDeserializedProperties(recordSetProperties);
				recordSet.setSaved(true);
				try {
					String[] timeStamp = recordSetComment.split(GDE.STRING_BLANK + GDE.STRING_OR + GDE.STRING_COMMA);
					int index = 0;
					while (!timeStamp[index].contains(GDE.STRING_DOT) && !timeStamp[index].contains(GDE.STRING_DASH))
						++index;
					int year = timeStamp[index].contains(GDE.STRING_DOT)
							? Integer.parseInt(timeStamp[index].substring(6, 10))
									: Integer.parseInt(timeStamp[index].substring(0, 4));
					year = year >= 2000 ? year : year > 50 ? year + 1900 : year + 2000;
					int month = timeStamp[index].contains(GDE.STRING_DOT)
							? Integer.parseInt(timeStamp[index].substring(3, 5))
									: Integer.parseInt(timeStamp[index].substring(5, 7));
					int day = timeStamp[index].contains(GDE.STRING_DOT)
							? Integer.parseInt(timeStamp[index].substring(0, 2))
									:	Integer.parseInt(timeStamp[index].substring(8, 10));
					while (!timeStamp[index].contains(GDE.STRING_COLON))
						++index;
					int hour = Integer.parseInt(timeStamp[index].substring(0, 2));
					int minute = Integer.parseInt(timeStamp[index].substring(3, 5));
					int second = Integer.parseInt(timeStamp[index].substring(6, 8));
					GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
					recordSet.setStartTimeStamp(calendar.getTimeInMillis());
				}
				catch (Exception e) {
					//ignore and use GDE value }
				}
				//recordSet.setObjectKey(recordSetInfo.get(GDE.OBJECT_KEY));

				//apply record sets records properties
				for (int i = 0; i < recordsProperties.length; ++i) {
					Record record = recordSet.get(i);
					record.setSerializedProperties(recordsProperties[i]); //name, unit, symbol, active, ...
					record.setSerializedDeviceSpecificProperties(recordsProperties[i]); // factor, offset, ...
				}

				channel.put(recordSetName, recordSet);
			}
			MenuToolBar menuToolBar = LogViewReader.application.getMenuToolBar();
			if (menuToolBar != null) {
				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
			}

			String[] firstRecordSet = new String[2];
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (firstRecordSet[0] == null || firstRecordSet[1] == null) {
					firstRecordSet[0] = channelConfig;
					firstRecordSet[1] = recordSetName;
				}
				recordDataSize = Integer.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE).trim()).intValue();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordDataSize = " + recordDataSize);
				recordSetDataBytes = Long.valueOf(recordSetInfo.get(GDE.RECORD_SET_DATA_BYTES).trim()).intValue();
				if (lastRecordSetDataBytes == 0) { // file contains more then one record set
					lastRecordSetDataBytes = recordSetDataBytes;
				}
				else {
					recordSetDataBytes = recordSetDataBytes - lastRecordSetDataBytes;
					lastRecordSetDataBytes = Long.valueOf(recordSetInfo.get(GDE.RECORD_SET_DATA_BYTES).trim()).intValue();
				}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordSetDataSize = " + recordSetDataBytes);
				recordSetDataPointer = position;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetDataPointer, recordSetDataPointer));
				channel = channels.get(channelNumber);
				recordSet = channel.get(recordSetName);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, recordSetDataBytes);
				//channel.setActiveRecordSet(recordSet);

				byte[] buffer = new byte[recordSetDataBytes];

				if (recordSetName.equals(firstRecordSet[1])) {
					long startTime = new Date().getTime();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "data buffer size = " + buffer.length); //$NON-NLS-1$
					data_in.readFully(buffer);
					log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
					device.addConvertedLovDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, GDE.isWithUi());
					log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
					device.updateVisibilityStatus(recordSet, true);
					if (application.getMenuToolBar() != null) {
						channel.applyTemplate(recordSet.getName(), true);
					}
				}


				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null && application.getMenuToolBar() != null) {
					isFirstRecordSetDisplayed = true;
					channel.setFileName(filePath);
					channel.setFileDescription(header.get(GDE.FILE_COMMENT));
					channel.setSaved(true);
					channels.switchChannel(channelNumber, firstRecordSet[1]);
				}

				position += buffer.length;
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("data pointer position = 0x%X", position)); //$NON-NLS-1$
			}
			return recordSet;
		}
		finally {
			data_in.close ();
			data_in = null;
			file_input = null;
		}
	}


	/**
	 * read record set data with given file seek pointer and record size
	 * @param recordSet
	 * @param filePath
	 * @throws DataInconsitsentException
	 */
	public static void readRecordSetsData(RecordSet recordSet, String filePath, boolean doUpdateProgressBar) throws FileNotFoundException, IOException, DataInconsitsentException {
		RandomAccessFile random_in = null;

		try {
			long recordSetFileDataPointer = recordSet.getFileDataPointer();
			int recordFileDataSize = recordSet.getFileDataSize();
			IDevice device = recordSet.getDevice();
			long startTime = new Date().getTime();
			byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordSetDataSize = " + buffer.length);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetFileDataPointer, recordSetFileDataPointer));

			random_in = new RandomAccessFile(new File(filePath), "r"); //$NON-NLS-1$
			random_in.seek(recordSetFileDataPointer);
			recordSetFileDataPointer = random_in.getFilePointer();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetFileDataPointer, recordSetFileDataPointer));
			random_in.readFully(buffer);
			random_in.close();

			device.addConvertedLovDataBufferAsRawDataPoints(recordSet, buffer, recordFileDataSize, doUpdateProgressBar);
			log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));

			device.updateVisibilityStatus(recordSet, true);
			if (application.getMenuToolBar() != null) {
				channels.getActiveChannel().applyTemplate(recordSet.getName(), true);
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if (random_in != null) random_in.close();
		}
	}

	/**
	 * get parsed record set properties containing all data found by OSD_FORMAT_DATA_KEYS
	 * @param recordSetProperties
	 * @return hash map with string type data
	 */
	public static HashMap<String, String> getRecordSetProperties(String recordSetProperties) {
		return StringHelper.splitString(recordSetProperties, GDE.DATA_DELIMITER, GDE.LOV_FORMAT_DATA_KEYS);
	}

	/**
	 * get the basic header data like the version, header size, ... (no difference for all known format versions)
	 * @param data_in
	 * @throws IOException
	 * @throws NotSupportedFileFormat
	 */
	private static HashMap<String, String> getBaseHeaderData(HashMap<String, String> header, DataInputStream data_in) throws IOException, NotSupportedFileFormatException {
		long position = 0;
		//read total header size
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long headerSize = parse2Long(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.LOV_HEADER_SIZE + headerSize);
		header.put(GDE.LOV_HEADER_SIZE, GDE.STRING_EMPTY+headerSize);

		// read LOV stream version
		buffer = new byte[4];
		position += data_in.read(buffer);
		int streamVersion = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.LOV_STREAM_VERSION + streamVersion);
		header.put(GDE.LOV_STREAM_VERSION, GDE.STRING_EMPTY+streamVersion);

		// read LOV tmp string size
		buffer = new byte[4];
		position += data_in.read(buffer);
		int tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String stringVersion = new String(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.LOV_SSTREAM_VERSION + stringVersion);
		if (streamVersion != Integer.valueOf(stringVersion.split(":V")[1])) { //$NON-NLS-1$
			NotSupportedFileFormatException e = new NotSupportedFileFormatException(Messages.getString(MessageIds.GDE_MSGE0008, new Object[] { streamVersion, stringVersion }));
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		header.put(GDE.LOV_SSTREAM_VERSION, stringVersion);

		// read LOV saved with version
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String lovFormatVersion = new String(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.LOV_FORMAT_VERSION + lovFormatVersion);
		header.put(GDE.LOV_FORMAT_VERSION, lovFormatVersion);

		// read LOV first saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CREATION_TIME_STAMP + new String(buffer));
		header.put(GDE.CREATION_TIME_STAMP, new String(buffer));

		// read LOV last saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, GDE.LAST_UPDATE_TIME_STAMP + new String(buffer));
		header.put(GDE.LAST_UPDATE_TIME_STAMP, new String(buffer));

		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}

	/**
	 * get LogView data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException
	 * @throws NotSupportedFileFormat
	 * @throws Exception
	 */

	public static HashMap<String, String> getHeader(final String filePath) throws IOException, NotSupportedFileFormatException, Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		HashMap<String, String> header = null;
		try {
			header = readHeader(data_in);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e instanceof IOException) {
				throw (IOException)e;
			}
			else if (e instanceof NotSupportedFileFormatException) {
				throw (NotSupportedFileFormatException)e;
			}
			else
				throw e;
		}
		finally {
			data_in.close();
		}
		return header;
	}

	/**
	 * method to read the data using a given input stream
	 * @param flogFilePath
	 * @param data_in
	 * @return
	 * @throws IOException
	 * @throws NotSupportedException
	 * @throws DataInconsitsentException
	 * @throws NotSupportedFileFormat
	 */
	public static HashMap<String, String> readHeader(DataInputStream data_in) throws IOException, NotSupportedFileFormatException, NotSupportedException, DataInconsitsentException {
		HashMap<String, String> header = new HashMap<String, String>();

		getBaseHeaderData(header, data_in);
		String streamVersion = header.get(GDE.LOV_STREAM_VERSION);
		String[] aVersion = header.get(GDE.LOV_FORMAT_VERSION).split(GDE.STRING_BLANK);
		String useVersion = aVersion.length > 1 ? aVersion[1] : "";
		if (aVersion.length >= 3) useVersion = useVersion + GDE.STRING_BLANK + aVersion[2];
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "using format version " + useVersion); //$NON-NLS-1$

		if (useVersion.equals("1.13")) { //$NON-NLS-1$
			header = getHeaderInfo_1_13(data_in, header);
			header = getRecordSetInfo_1_13(data_in, header);
		}
		else if (useVersion.startsWith("1.14") || useVersion.equals("1.15")) { //$NON-NLS-1$ //$NON-NLS-2$
			header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_15(data_in, header);
		}
		else if (useVersion.equals("1.50 ALPHA")) { //$NON-NLS-1$
			header = getHeaderInfo_1_50_ALPHA(data_in, header);
			//header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_50_ALPHA(data_in, header);
		}
		else if (useVersion.equals("1.50 PreBETA") || useVersion.startsWith("2.0 BETA")) { //$NON-NLS-1$ //$NON-NLS-2$
			header = getHeaderInfo_1_50_BETA(data_in, header);
			//header = getHeaderInfo_2_0(data_in, header);
			header = getRecordSetInfo_1_50_BETA(data_in, header);
		}
		else if (streamVersion.equals("4")) { //$NON-NLS-1$
			header = getHeaderInfo_4(data_in, header);
			header = getRecordSetInfo_4(data_in, header);
		}
		else if (streamVersion.equals("5")) { //$NON-NLS-1$
			header = getHeaderInfo_5(data_in, header);
			header = getRecordSetInfo_5(data_in, header);
		}
		else {
			NotSupportedFileFormatException e = new NotSupportedFileFormatException(Messages.getString(MessageIds.GDE_MSGI0021, new Object[] { useVersion } ));
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.13
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_1_13(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);


		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(headerSize-position);
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}

	/**
	 * get the record set and dependent record parameters of format version 1.13
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_13(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(8);

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(28);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.15
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_1_15(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);


		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(headerSize-position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);

		return header;
	}

	/**
	 * get the record set and dependent record parameters of format version 1.15
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_15(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(8);

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.50 ALPHA
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_1_50_ALPHA(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);


		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(headerSize-position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);

		return header;
	}


	/**
	 * get the record set and dependent record parameters of format version 1.50 ALPHA
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_50_ALPHA(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$


			position += data_in.skipBytes(2);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$

			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			//position += data_in.skipBytes(122);
			//log.log(Level.INFO, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			int dataSize = parse2Int(buffer);

			if (tmpDataSize != dataSize) {
				log.log(Level.WARNING, "data size calculation wrong");
			}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
				sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

				position += data_in.skipBytes(216);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			//position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.50 BETA
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_1_50_BETA(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		// read file comment
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer);
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, rtfString);

		String fileComment = parseRtfString(rtfString);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			fileCommentSize = parse2Int(buffer);
			buffer = new byte[(int)fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(8);

		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$

		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP))
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((Integer.valueOf(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}

	/**
	 * get the record set and dependent record parameters of format version 2.0
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_50_BETA(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(2);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$

			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());

			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, rtfString);

			String recordSetComment = parseRtfString(rtfString);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, new String(buffer));
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position));			 //$NON-NLS-1$

			position += data_in.skip(175);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format stream version 4
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_4(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		// read file comment
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer);
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, rtfString);

		String fileComment = parseRtfString(rtfString);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			fileCommentSize = parse2Int(buffer);
			buffer = new byte[(int)fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(8);

		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$

		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP))
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((Integer.valueOf(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}

	/**
	 * get the record set and dependent record parameters of format stream version
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_4(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$

			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, rtfString);

			String recordSetComment = parseRtfString(rtfString);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, new String(buffer));


			position += data_in.skip(175);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format stream version 5
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException
	 */
	private static HashMap<String, String> getHeaderInfo_5(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));
		long headerSize = Long.parseLong(header.get(GDE.LOV_HEADER_SIZE));
		byte[] buffer = new byte[0];
		long fileCommentSize = 0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read file comment
		buffer = new byte[8];
		position += data_in.read(buffer);
		fileCommentSize = parse2Long(buffer);
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);

		String fileComment = parseRtfString(rtfString);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			fileCommentSize = parse2Int(buffer);
			buffer = new byte[(int)fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new String(buffer).contains(GDE.STRING_EQUAL) ? Integer.valueOf(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue() : 1;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);

		position += data_in.skip(8);

		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$

		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP))
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((Integer.valueOf(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}


	/**
	 * parse RTF String into plain text
	 * @param rtfString
	 * @return
	 */
	static String parseRtfString(String rtfString) {
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, rtfString);
		StringBuilder fileComment = new StringBuilder();

		if (rtfString.indexOf("\\plain ") != -1 ) { // plain text exist in RTF string
			String[] array = rtfString.split("plain ");
			for (int i = 1; i < array.length; i++) {
				int beginIndex = 0, endIndex = 0;
				while ((beginIndex = array[i].indexOf(" ", beginIndex)) != -1				// \fs22 Text
						&& (endIndex = array[i].indexOf("\\", beginIndex)) != -1) {
					fileComment.append(array[i].substring(beginIndex+1, endIndex));
					beginIndex = endIndex+1;
				}
			}
		}
		while (fileComment.length() > 1 && (fileComment.lastIndexOf("\n") == fileComment.length()-1 || fileComment.lastIndexOf("\r") == fileComment.length()-1) )
			fileComment.deleteCharAt(fileComment.length()-1);

		return fileComment.toString();
	}

	/**
	 * get the record set and dependent record parameters of format stream version 5
	 * @param analyzer
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	private static HashMap<String, String> getRecordSetInfo_5(DataInputStream data_in, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long position = Long.parseLong(header.get(GDE.DATA_POINTER_POS));

		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);

		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);

			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int dataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			dataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			dataSize = dataSize > parse2Int(buffer) ? dataSize : parse2Int(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);

			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, new String(buffer));
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			header.put(GDE.LOV_CONFIG_DATA, config.toString());

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);

			String recordSetComment = parseRtfString(rtfString);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment.toString());
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment.toString()).append(GDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, new String(buffer));
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[115];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, new String(buffer));
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			//4 byte before R_%

//			//search for R_% <17> %@
//			position += data_in.skip(100);
//			Vector<Byte> byteBuffer = new Vector<Byte>();
//			buffer = new byte[1];
//			boolean isSignature = false;
//			while (!isSignature) {
//				position += data_in.read(buffer);
//				byteBuffer.add(buffer[0]);
//				if (buffer[0] == 'R') {
//					position += data_in.read(buffer);
//					if (buffer[0] != '_')
//						continue;
//					position += data_in.read(buffer);
//					if (buffer[0] == '%')
//						isSignature = true;
//				}
//			}
//
//			//time format
//			buffer = new byte[4];
//			for (int j = 0; j < buffer.length; j++) {
//				buffer[i] = byteBuffer.get(byteBuffer.size()-2-j);
//			}
//			int numberChars = parse2Int(buffer)-3;

			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberChars = parse2Int(buffer);

			if (numberChars > 100) {
				log.log(Level.SEVERE, "numberChars = " + numberChars); //$NON-NLS-1$
				throw new DataInconsitsentException("Corrupt input file");
			}
			buffer = new byte[numberChars];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "R_%" + new String(buffer) + "%@");
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			position += data_in.skip(56);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%s%d (%X%X%X%X%X%X%X%X)", GDE.RECORD_SET_DATA_BYTES, recordSetDataBytes, buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]));
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		return header;
	}

	/**
	 * parse data buffer to integer value, length meight be 1, 2, 3, 4, 8 bytes
	 * @param buffer
	 */
	public static int parse2Int(byte[] buffer) {
		switch (buffer.length) {
		case 1:
			return (buffer[0] & 0xff);
		case 2:
			return (((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		case 3:
			return (((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		default:
		case 4:
			return (((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		case 8:
			return (((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
//		case 16:
//			return (((buffer[7] & 0xff) << 56) | ((buffer[6] & 0xff) << 48) | ((buffer[5] & 0xff) << 40) | ((buffer[4] & 0xff) << 32) | ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		}
	}

	/**
	 * parse data buffer to long value, data buffer length must be 8 bytes
	 * @param buffer
	 */
	public static long parse2Long(byte[] buffer) {
		long tmpLong1 = ((long)(buffer[3] & 0xff) << 24) + ((buffer[2] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + ((buffer[0] & 0xff) << 0);
		long tmpLong2 = (((long)buffer[7] & 255) << 56) + ((long)(buffer[6] & 255) << 48) + ((long)(buffer[5] & 255) << 40) + ((long)(buffer[4] & 255) << 32);
    return  tmpLong2 + tmpLong1;

	}

	/**
	 * map LogView device names with GDE device names if possible
	 * @param deviceName
	 * @return
	 * @throws NotSupportedException
	 */
	private static String mapLovDeviceNames(String deviceName) throws NotSupportedException {
		String deviceKey = deviceName.toLowerCase().trim();
		deviceKey = deviceMap.containsKey(deviceKey) ? deviceKey : deviceKey.split("_")[0];
		if (!deviceMap.containsKey(deviceKey)) {
			String msg = Messages.getString(MessageIds.GDE_MSGW0016, new Object[] { deviceName });
			NotSupportedException e = new NotSupportedException(msg);
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}

		return deviceMap.get(deviceKey) != null ? deviceMap.get(deviceKey) : deviceMap.get(deviceKey.split("_")[0]);
	}
}
