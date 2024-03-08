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
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.exception.GDEInternalException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 * This class reads and writes DataExplorer file format
 */
public class OsdReaderWriter {
	final static Logger				log					= Logger.getLogger(OsdReaderWriter.class.getName());

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * Determine the record set for display.
	 * Criteria are the record set name trunk and channel number (supports a language neutral selection).
	 * @author Thomas Eickert (USER)
	 */
	private final class RecordSetSelector {

		private boolean	isFixed;
		private int			channelNumber				= -1;
		private String	channelName					= null;
		private String	recordSetNameTrunk	= null;

		RecordSetSelector(String newRecordSetName) {
			this();
			// ET this prevents finding the correct channel   --  this.isFixed = !settings.isFirstRecordSetChoice();
			this.recordSetNameTrunk = newRecordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? newRecordSetName : newRecordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
		}

		RecordSetSelector() {
			this.channelNumber = application.getActiveChannel().getNumber();
			this.channelName = application.getActiveChannel().getChannelConfigKey();
		}

		void setBestFit(int newChannelNumber, String newChannelName, String newRecordSetName) {
			if (!this.isFixed) {
				String newNameTrunk = newRecordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? newRecordSetName : newRecordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (Settings.getInstance().isFirstRecordSetChoice()) {
					// standard case
					this.isFixed = true;
					this.channelNumber = newChannelNumber;
					this.recordSetNameTrunk = newNameTrunk;
				}
				else if (this.recordSetNameTrunk == null) {
					// try to preserve the channel selection (use case: opened via standard menu etc.)
					if (this.channelNumber == newChannelNumber) {
						this.isFixed = true;
						this.channelNumber = newChannelNumber;
						this.recordSetNameTrunk = newNameTrunk;
					}
				}
				else if (this.recordSetNameTrunk.equals(newNameTrunk)) {
					// a specific record set is requested (use case: opened from histo)
					this.isFixed = true;
					this.channelNumber = newChannelNumber;
					this.recordSetNameTrunk = newNameTrunk;
				}
				else if (this.channelName.equals(newChannelName)) {
					throw new UnsupportedOperationException("channel number does not match but the channel name does"); // ET for data conformity checks only  -  may be removed
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("isFixed isFirstRecordSetChoice=%b  recordSetNameTrunk=%-22s  channelNumber=%d  channelName=%-22s",
						Settings.getInstance().isFirstRecordSetChoice(), this.recordSetNameTrunk, this.channelNumber, this.channelName));
			}
		}

		boolean isBestFitFound() {
			return this.isFixed;
		}

		boolean isMatchToBestFit(int newChannelNumber, String newRecordSetName) {
			if (this.isFixed) {
				return this.recordSetNameTrunk.equals(newRecordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? newRecordSetName : newRecordSetName.substring(0, RecordSet.MAX_NAME_LENGTH))
						&& this.channelNumber == newChannelNumber;
			}
			else
				return false;
		}
	}

	/**
	 * get data file header data
	 * @param inputStream
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws NotSupportedFileFormatException with empty message string
	 */
	public static HashMap<String, String> getHeader(InputStream inputStream) throws IOException, NotSupportedFileFormatException {
		String exceptionMessageFilePath = "";
		return readHeader(exceptionMessageFilePath, new DataInputStream(FileUtils.wrapIfZipStream(inputStream)));
	}

	/**
	 * get data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */
	public static HashMap<String, String> getHeader(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException {
		try (InputStream inputStream = new FileInputStream(new File(filePath));) {
			String exceptionMessageFilePath = filePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
			return readHeader(exceptionMessageFilePath, new DataInputStream(FileUtils.wrapIfZipStream(inputStream)));
		}
	}

	/**
	 * method to read the data using a given input stream
	 * @param filePath
	 * @param data_in
	 * @return
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */
	protected static HashMap<String, String> readHeader(final String filePath, DataInputStream data_in) throws IOException, NotSupportedFileFormatException {
		String line;
		HashMap<String, String> header = new HashMap<String, String>();
		int headerCounter = GDE.OSD_FORMAT_HEADER_KEYS.length + 1;

		line = data_in.readUTF();
		line = line.substring(0, line.length() - 1);
		log.log(Level.FINE, line);
		if (!line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) && !line.startsWith(GDE.LEGACY_FILE_VERSION))
			throw new NotSupportedFileFormatException(filePath);

		String sVersion = line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) ? line.substring(GDE.DATA_EXPLORER_FILE_VERSION.length(), GDE.DATA_EXPLORER_FILE_VERSION.length() + 1).trim()
				: line.substring(GDE.LEGACY_FILE_VERSION.length(), GDE.LEGACY_FILE_VERSION.length() + 1).trim();
		int version;
		try {
			version = Integer.valueOf(sVersion).intValue(); // one digit only
		}
		catch (NumberFormatException e) {
			log.log(Level.SEVERE, "can not interprete red version information " + sVersion);
			throw new NotSupportedFileFormatException(filePath);
		}

		switch (version) {
		case 1:
		case 2: // added OBJECT_KEY to header
		case 3: // added startTimeStamp to recordSet
		case 4: // enable more measurements which leads to property string length more than 2**16 character
			header.put(GDE.DATA_EXPLORER_FILE_VERSION, GDE.STRING_EMPTY + version);
			boolean isHeaderComplete = false;
			while (!isHeaderComplete && headerCounter-- > 0) {
				line = data_in.readUTF();
				line = line.substring(0, (line.length() > 0 ? line.length() - 1 : 0));
				log.log(Level.FINE, line);
				for (String headerKey : GDE.OSD_FORMAT_HEADER_KEYS) {
					if (line.startsWith(headerKey)) {
						log.log(Level.FINE, line);
						header.put(headerKey, line.substring(headerKey.length()));
						if (line.startsWith(GDE.RECORD_SET_SIZE)) {
							headerCounter = Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
							//read record set descriptors
							int lastReordNumber = headerCounter;
							while (headerCounter-- > 0) {
								switch (version) {
								case 1:
								case 2:
								case 3:
									// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
									line = data_in.readUTF();
									line = line.substring(0, line.length() - 1);
									break;

								default:
								case 4:
									// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
									int length = data_in.readInt();
									byte[] bytes = new byte[length];
									data_in.readFully(bytes);
									line = new String(bytes, "UTF8");
									line = line.substring(0, line.length() - 1);
									break;
								}
								if (line.startsWith(GDE.RECORD_SET_NAME)) {
									log.log(Level.FINE, line);
									header.put((lastReordNumber - headerCounter) + GDE.STRING_BLANK + GDE.RECORD_SET_NAME, line.substring(GDE.RECORD_SET_NAME.length()));
								}
							}
							isHeaderComplete = true;
						}
						break;
					}
				}
			}
			break;

		default:
			throw new NotSupportedFileFormatException(filePath);
		}

		return header;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static RecordSet read(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		return read(filePath, new OsdReaderWriter().new RecordSetSelector());
	}

	/**
	 * read complete file data and display the requested record set
	 * @param filePath
	 * @param recordSetName
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static RecordSet read(String filePath, String recordSetName) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		return read(filePath, new OsdReaderWriter().new RecordSetSelector(recordSetName));
	}

	private static RecordSet read(String filePath, RecordSetSelector recordSetSelector) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		filePath = filePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)));
		ZipEntry zip_entry = zip_input.getNextEntry();
		FileInputStream file_input = null;
		DataInputStream data_in = null;
		if (zip_entry != null) {
			data_in = new DataInputStream(zip_input);
		}
		else {
			zip_input.close();
			zip_input = null;
			file_input = new FileInputStream(new File(filePath));
			data_in = new DataInputStream(file_input);
		}
		String channelConfig = GDE.STRING_EMPTY;
		String recordSetName = GDE.STRING_EMPTY;
		int recordDataSize = 0;
		long recordSetDataPointer = 0;
		Channel channel = null;
		RecordSet recordSet = null;

		HashMap<String, String> header = getHeader(filePath);
		ChannelTypes channelType = ChannelTypes.valueOf(header.get(GDE.CHANNEL_CONFIG_TYPE).trim());
		String objectKey = header.get(GDE.OBJECT_KEY) != null ? header.get(GDE.OBJECT_KEY) : GDE.STRING_EMPTY;
		while (!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
			log.log(Level.FINE, "skip"); //$NON-NLS-1$

		List<HashMap<String, String>> recordSetsInfo = readRecordSetsInfo4AllVersions(data_in, header);
		try { // build the data structure
			for (HashMap<String, String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordDataSize = Long.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				//recordSetDataPointer = Long.valueOf(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();

				channelConfig = Analyzer.getInstance().getActiveDevice().crossCheckChannelConfig(channelConfig, recordSetInfo);
				channel = channels.get(channels.getChannelNumber(channelConfig));
				if (channel == null) { // 1.st try channelConfiguration not found
					try { // get channel last digit and use as channel config ordinal
						channel = channels.get(Integer.valueOf(channelConfig.substring(channelConfig.length() - 1)));
						channelConfig = channel.getChannelConfigKey();
						recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
					}
					catch (NumberFormatException e) {
						// ignore and keep channel as null
					}
					catch (NullPointerException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 2.nd try channelConfiguration not found
					try { // try to get channel startsWith configuration name
						channel = channels.get(channels.getChannelNumber(channelConfig.split(" ")[0]));
						channelConfig = channel.getChannelConfigKey();
						recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
					}
					catch (NullPointerException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 3.rd try channelConfiguration not found
					String msg = Messages.getString(MessageIds.GDE_MSGI0018, new Object[] { recordSetName }) + " " + Messages.getString(MessageIds.GDE_MSGI0019) + "\n" + Messages.getString(MessageIds.GDE_MSGI0020);
					DataExplorer.getInstance().openMessageDialogAsync(msg);
					channel = channels.addChannel(channelConfig, channelType, Analyzer.getInstance());
				}
				channels.setActiveChannelNumber(channel.getNumber());
				OsdReaderWriter.application.selectObjectKey(objectKey); //calls channel.setObjectKey(objectKey);
				// "3 : Motor"
				channelConfig = channelConfig.contains(GDE.STRING_COLON) ? channelConfig.split(GDE.STRING_COLON)[1].trim() : channelConfig.trim();
				// "Motor 3"
				channelConfig = channelConfig.contains(GDE.STRING_BLANK) ? channelConfig.split(GDE.STRING_BLANK)[0].trim() : channelConfig.trim();
				//"Motor"

				recordSet = buildRecordSet(recordSetName, channel.getNumber(), recordSetInfo, true, Analyzer.getInstance());
				channel.put(recordSetName, recordSet);

				if (!recordSetSelector.isBestFitFound()) recordSetSelector.setBestFit(channel.getNumber(), channelConfig, recordSetName);
			}

			MenuToolBar menuToolBar = OsdReaderWriter.application.getMenuToolBar();
			if (menuToolBar != null) {
				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
			}

			long unreadDataPointer = -1;
			for (HashMap<String, String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordDataSize = Long.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordDataSize = " + recordDataSize);
				recordSetDataPointer = Long.valueOf(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER)).longValue();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordSetDataPointer = " + recordSetDataPointer);
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				int numberRecordAndTimeStamp = recordSet.getNoneCalculationRecordNames().length + (recordSet.isTimeStepConstant() ? 0 : 1);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize);

				if (recordSetSelector.isBestFitFound() && !recordSetSelector.isMatchToBestFit(channel.getNumber(), recordSetName)) {
					// defer reading any unused recordsets until a recordset is actually required
					if (unreadDataPointer <= -1) unreadDataPointer = recordSetDataPointer;
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, String.format("skipped  channelConfigName=%-22s recordSetName=%-40s unreadDataPointer=%,d", channelConfig, recordSetName, unreadDataPointer));
				}
				else {
					// take the matching record set or take the first one
					if (unreadDataPointer > -1) unreadDataPointer = skipData(data_in, recordSetDataPointer, unreadDataPointer);

					long startTime = new Date().getTime();
					byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
					data_in.readFully(buffer);
					recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, GDE.isWithUi());
					recordSet.updateVisibleAndDisplayableRecordsForTable();
					if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (new Date().getTime() - startTime)));

					if (application.getMenuToolBar() != null) displayRecordSet(filePath, header.get(GDE.FILE_COMMENT), channelConfig, recordSetName);
				}
			}

			//synchronize file description over channels, if required
			Channel activeChannel = channels.getActiveChannel();
			IDevice device = Analyzer.getInstance().getActiveDevice();
			if (activeChannel != null && device != null && device.useChannelWithSyncedDescription()) {
				activeChannel.syncFileDescription(activeChannel, activeChannel.getFileDescription());
			}
			
			return recordSet;
		}
		finally {
			if (zip_input != null) {
				zip_input.closeEntry();
				data_in.close();
				zip_input.close();
			}
			else if (file_input != null) {
				data_in.close();
				file_input.close();
			}
			data_in = null;
			file_input = null;
			zip_input = null;
		}
	}

	/**
	 * @param data_in
	 * @param recordSetDataPointer
	 * @param unreadDataPointer
	 * @return
	 * @throws IOException
	 * @throws EOFException
	 */
	private static long skipData(DataInputStream data_in, long recordSetDataPointer, long unreadDataPointer) throws IOException, EOFException {
		// make up all deferred readings in one single step
		long toSkip = recordSetDataPointer - unreadDataPointer;
		while (toSkip > 0) { // The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
			toSkip -= data_in.skip(toSkip);
			if (toSkip > 0) {
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "toSkip=" + toSkip); //$NON-NLS-1$
				if (data_in.available() == 0) throw new EOFException("recordDataSize / recordSetDataPointer do not match the actual file size"); //$NON-NLS-1$
			}
		}
		unreadDataPointer = -1;
		return unreadDataPointer;
	}

	/**
	 * Display the record set data.
	 * @param filePath
	 * @param fileDescription
	 * @param channelConfigName
	 * @param recordSetName
	 */
	private static void displayRecordSet(String filePath, String fileDescription, String channelConfigName, String recordSetName) {
		Channel channel = channels.get(channels.getChannelNumber(channelConfigName));
		channel.setFileName(filePath);
		channel.setFileDescription(fileDescription);
		channel.setSaved(true);

		String displayRecordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
		channels.switchChannel(channels.getChannelNumber(channelConfigName), displayRecordSetName);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "channelConfigName=" + channelConfigName, "  recordSetName=" + recordSetName); //$NON-NLS-1$
	}

	/**
	 * side effects in case the active device supports a variable measurement size:
	 *  - measurements are removed from the channel
	 *  - measurementType objects are modified
	 * @param recordSetName
	 * @param channelNumber supporting channel mix which does not use the analyzer's channel number
	 * @param recordSetInfo
	 * @param adjustObjectKey defines if the channel's object key is updated by the settings objects key
	 * @return the recordSet filled with basic data delivered by the params but without any values (points)
	 */
	protected static RecordSet buildRecordSet(String recordSetName, int channelNumber, HashMap<String, String> recordSetInfo, boolean adjustObjectKey, Analyzer analyzer) {
		RecordSet recordSet;
		String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
		String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
		String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
		IDevice device = analyzer.getActiveDevice();
		if (device.isVariableMeasurementSize()) {
			//cleanup measurement, if count doesn't match
			int existingNumberMeasurements = device.getDeviceConfiguration().getMeasurementNames(channelNumber).length;
			if (recordsProperties.length != existingNumberMeasurements) {
				for (int i = recordsProperties.length; i < existingNumberMeasurements; i++) {
					device.removeMeasurementFromChannel(channelNumber, device.getMeasurement(channelNumber, recordsProperties.length));
				}
			}
			//build up the record set with variable number of records just fit the sensor data
			String[] recordNames = new String[recordsProperties.length];
			String[] recordSymbols = new String[recordsProperties.length];
			String[] recordUnits = new String[recordsProperties.length];
			for (int i = 0; i < recordsProperties.length; i++) {
				HashMap<String, String> recordProperties = StringHelper.splitString(recordsProperties[i], Record.DELIMITER, Record.propertyKeys);
				MeasurementType gdeMeasurement = device.getMeasurement(channelNumber, i);
				gdeMeasurement.setName(recordNames[i] = recordProperties.get(Record.NAME));
				gdeMeasurement.setUnit(recordUnits[i] = recordProperties.get(Record.UNIT));
				gdeMeasurement.setSymbol(recordSymbols[i] = recordProperties.getOrDefault(Record.SYMBOL, GDE.STRING_EMPTY));
				gdeMeasurement.setActive(Boolean.valueOf(recordProperties.get(Record.IS_ACTIVE)));
			}
			recordSet = RecordSet.createRecordSet(recordSetName, analyzer, channelNumber, recordNames, recordSymbols, recordUnits, true, true, true);
		}
		else {
			recordSet = RecordSet.createRecordSet(recordSetName, analyzer, channelNumber, true, true, true);
		}
		recordSet.setRecordSetDescription(recordSetComment);

		//apply record sets records properties
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder().append(recordSet.getName()).append(GDE.STRING_MESSAGE_CONCAT);
			for (String recordProps : recordsProperties) {
				sb.append(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0])).append(GDE.STRING_COMMA);
			}
			log.log(Level.FINE, sb.toString());
		}

		String[] recordKeys = device.crossCheckMeasurements(recordsProperties, recordSet);
		// check if the file content fits measurements form device properties XML which was used to create the record set
		for (int i = 0; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			if (record != null) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " - setSerializedProperties " + recordsProperties[i].subSequence(6, recordsProperties[i].indexOf('|')));
				record.setSerializedProperties(recordsProperties[i]);
				record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
			}
			else { //possible errors during language keys exchange, check device XML and DeviveXmlResources.properties
				log.log(Level.WARNING, String.format("After cross checking initial recordSet names with recordsProperties a record with name %s could not be found!", recordKeys[i]));
				for (int j = 0; j < recordKeys.length; j++) {
					log.log(Level.WARNING, String.format("%20s - %s", recordKeys[j], recordsProperties[j].subSequence(6, recordsProperties[j].indexOf('|'))));
				}
			}
		}
		device.applyMeasurementSpecialties(recordsProperties, recordSet);
		recordSet.setDeserializedProperties(recordSetProperties);
		recordSet.setSaved(true);
		return recordSet;
	}

	/**
	 * @param data_in
	 * @param header
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private static List<HashMap<String, String>> readRecordSetsInfo4AllVersions(DataInputStream data_in, HashMap<String, String> header)
			throws NumberFormatException, IOException, UnsupportedEncodingException {
		String line;
		// record sets with it properties and records
		List<HashMap<String, String>> recordSetsInfo = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue(); ++i) {
			switch (Integer.valueOf(header.get(GDE.DATA_EXPLORER_FILE_VERSION))) {
			case 1:
			case 2:
			case 3:
				// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
				line = data_in.readUTF();
				line = line.substring(0, line.length() - 1);
				break;

			default:
			case 4:
				// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
				int length = data_in.readInt();
				byte[] bytes = new byte[length];
				data_in.readFully(bytes);
				line = new String(bytes, "UTF8");
				line = line.substring(0, line.length() - 1);
				break;
			}
			recordSetsInfo.add(getRecordSetProperties(line));
		}
		return recordSetsInfo;
	}

	/**
	 * @param recordSetProperties
	 * @return the recordset name extracted form the recordset properties
	 */
	public static String getRecordSetName(String recordSetProperties) {
		return recordSetProperties.substring(0, recordSetProperties.indexOf(GDE.DATA_DELIMITER));
	}

	/**
	 * get parsed record set properties containing all data found by OSD_FORMAT_DATA_KEYS
	 * @param recordSetProperties
	 * @return hash map with string type data
	 */
	public static HashMap<String, String> getRecordSetProperties(String recordSetProperties) {
		return StringHelper.splitString(recordSetProperties, GDE.DATA_DELIMITER, GDE.OSD_FORMAT_DATA_KEYS);
	}

	/**
	 * write channel data to osd file format
	 * - if channel type is TYPE_OUTLET only this channel record sets are part of the written file
	 * - if channel type is TYPE_CONFIG all records sets of all channel configurations are written to the file
	 * @param fullQualifiedFilePath
	 * @param activeChannel
	 * @param useVersion
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void write(String fullQualifiedFilePath, Channel activeChannel, int useVersion) throws FileNotFoundException, IOException {
		fullQualifiedFilePath = fullQualifiedFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		if (activeChannel != null && fullQualifiedFilePath != null && useVersion != 0) {
			ZipOutputStream file_out = new ZipOutputStream(new FileOutputStream(new File(fullQualifiedFilePath)));
			file_out.putNextEntry(new ZipEntry(fullQualifiedFilePath.substring(fullQualifiedFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1)));
			DataOutputStream data_out = new DataOutputStream(file_out);
			IDevice activeDevice = OsdReaderWriter.application.getActiveDevice();
			boolean isObjectOriented = OsdReaderWriter.application.isObjectoriented();
			int filePointer = 0;
			try {
				// before do anything make sure all data is loaded, if data comes from another file
				activeChannel.checkAndLoadData();

				// first line : header with version
				String versionString = GDE.DATA_EXPLORER_FILE_VERSION + useVersion + GDE.STRING_NEW_LINE;
				data_out.writeUTF(versionString);
				filePointer += GDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				String creationString = GDE.CREATION_TIME_STAMP + Instant.now().toString() + GDE.STRING_NEW_LINE;
				data_out.writeUTF(creationString);
				filePointer += GDE.SIZE_UTF_SIGNATURE + creationString.getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + creationString.getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				// second line : size file comment , file comment
				StringBuilder sb = new StringBuilder();
				sb.append(GDE.FILE_COMMENT).append(activeChannel.getFileDescription()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// third line : size device name , device name
				sb = new StringBuilder();
				sb.append(GDE.DEVICE_NAME).append(activeDevice.getName()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fourth line : size channel/config type , channel/config type
				sb = new StringBuilder();
				sb.append(GDE.CHANNEL_CONFIG_TYPE).append(activeChannel.getType().name()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fifth line : object key
				sb = new StringBuilder();
				sb.append(GDE.OBJECT_KEY).append(activeChannel.getObjectKey()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// number of record sets
				sb = new StringBuilder();
				sb.append(GDE.RECORD_SET_SIZE).append(activeChannel.size()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// record sets with it properties
				StringBuilder[] sbs = new StringBuilder[activeChannel.size()];
				String[] recordSetNames = activeChannel.getRecordSetNames();
				// prepare all record set describing data
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							sbs[i] = new StringBuilder();
							sbs[i].append(GDE.RECORD_SET_NAME).append(recordSet.getName()).append(GDE.DATA_DELIMITER)
								.append(GDE.CHANNEL_CONFIG_NAME).append(recordSetChannel.getNumber()).append(GDE.STRING_BLANK_COLON_BLANK).append(recordSet.getChannelConfigName()).append(GDE.DATA_DELIMITER)
								.append(GDE.RECORD_SET_COMMENT).append(recordSet.getRecordSetDescription()).append(GDE.DATA_DELIMITER)
								.append(GDE.RECORD_SET_PROPERTIES).append(recordSet.getSerializeProperties()).append(GDE.DATA_DELIMITER);
							// serialized recordSet configuration data (record names, unit, symbol, isActive, ....) size data points , pointer data start or file name
							for (String recordKey : recordSet.getRecordNames()) {
								sbs[i].append(GDE.RECORDS_PROPERTIES).append(recordSet.get(recordKey).getSerializeProperties());
							}
							sbs[i].append(GDE.DATA_DELIMITER).append(GDE.RECORD_DATA_SIZE).append(String.format("%10s", recordSet.getRecordDataSize(true))).append(GDE.DATA_DELIMITER); //$NON-NLS-1$
							filePointer += GDE.SIZE_BYTES_INTEGER + sbs[i].toString().getBytes("UTF8").length; //$NON-NLS-1$
							filePointer += GDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + GDE.STRING_NEW_LINE.toString().getBytes("UTF8").length; // pre calculated size //$NON-NLS-1$ //$NON-NLS-2$
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE,		"line lenght = " //$NON-NLS-1$
											+ (GDE.SIZE_BYTES_INTEGER + sbs[i].toString().getBytes("UTF8").length + GDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 //$NON-NLS-1$//$NON-NLS-2$
													+ GDE.STRING_NEW_LINE.toString().getBytes("UTF8").length) //$NON-NLS-1$
											+ " filePointer = " + filePointer); //$NON-NLS-1$

							if (log.isLoggable(Level.FINE)) {
								StringBuilder sb1 = new StringBuilder().append(recordSet.getName()).append(GDE.STRING_MESSAGE_CONCAT);
								for (String recordKey : recordSet.getRecordNames()) {
									sb1.append(recordKey).append(GDE.STRING_COMMA);
								}
								log.log(Level.FINE, sb1.toString());
							}
						}
					}
				}
				// prepare all record set data pointer and store record sizes
				HashMap<String, Integer> recordSizes = new HashMap<String, Integer>();
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							recordSet.resetZoomAndMeasurement(); // make sure size() returns right value
							sbs[i].append(GDE.RECORD_SET_DATA_POINTER).append(String.format("%10s", filePointer)).append(GDE.STRING_NEW_LINE); //$NON-NLS-1$
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sbs[i].toString());
							//instead of using writeUTF, write the length and the string separate to workaround java.io.UTFDataFormatException: encoded string too long: 272312 bytes
							data_out.writeInt(sbs[i].toString().getBytes("UTF8").length);
							data_out.write(sbs[i].toString().getBytes("UTF8"));
							int sizeRecord = recordSet.getRecordDataSize(true);
							recordSizes.put(recordSetChannel.getNumber() + GDE.STRING_UNDER_BAR + recordSetNames[i], sizeRecord);
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, recordSetChannel.getNumber() + GDE.STRING_UNDER_BAR + recordSetNames[i] + "=" + sizeRecord);
							int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * sizeRecord;
							int dataSizeRecords = dataSizeRecord * recordSet.getNoneCalculationRecordNames().length;
							int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
							filePointer += (recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp);
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, (recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp) + " filePointer = " + filePointer); //$NON-NLS-1$
						}
					}
				}
				// check if all involved record sets have data (if loaded from file it might be possible that some record set lack of its data)
				long startTime = new Date().getTime();
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							if (!recordSet.hasDisplayableData()) recordSet.loadFileData(recordSetChannel.getFullQualifiedFileName(), GDE.isWithUi());
							String[] noneCalculationRecordNames = recordSet.getNoneCalculationRecordNames();
							int sizeRecord = recordSizes.get(recordSetChannel.getNumber() + GDE.STRING_UNDER_BAR + recordSetNames[i]);
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, recordSetChannel.getNumber() + GDE.STRING_UNDER_BAR + recordSetNames[i] + "=" + sizeRecord);
							int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * sizeRecord;
							int dataSizeRecords = dataSizeRecord * noneCalculationRecordNames.length;
							int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
							byte[] buffer = new byte[recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp];
							byte[] bytes = new byte[GDE.SIZE_BYTES_INTEGER];
							int l = 0;
							if (!recordSet.isTimeStepConstant()) {
								for (int j = 0; j < sizeRecord; ++j, l += GDE.SIZE_BYTES_INTEGER) {
									long timeStamp = recordSet.getTime(j);
									//log.log(Level.FINER, ""+point);
									bytes[0] = (byte) ((timeStamp >>> 24) & 0xFF);
									bytes[1] = (byte) ((timeStamp >>> 16) & 0xFF);
									bytes[2] = (byte) ((timeStamp >>> 8) & 0xFF);
									bytes[3] = (byte) ((timeStamp >>> 0) & 0xFF);
									System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
								}
							}
							if (recordSet.isRaw()) {
								for (int j = 0; j < sizeRecord; ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += GDE.SIZE_BYTES_INTEGER) {
										int point = recordSet.get(noneCalculationRecordNames[k]).realGet(j);
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							else {
								IDevice device = recordSet.getDevice();
								for (int j = 0; j < sizeRecord; ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += GDE.SIZE_BYTES_INTEGER) {
										Record record = recordSet.get(noneCalculationRecordNames[k]);
										int point = Double.valueOf(device.reverseTranslateValue(record, record.realGet(j) / 1000.0) * 1000.0).intValue();
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							data_out.write(buffer, 0, buffer.length);
							recordSet.setSaved(!fullQualifiedFilePath.contains(GDE.TEMP_FILE_STEM));
						}
					}
				}
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}
			finally {
				data_out.flush();
				file_out.closeEntry();
				data_out.close();
				data_out = null;
				file_out.close();
				file_out = null;
				
				//update/write link if object oriented
				if (isObjectOriented && !fullQualifiedFilePath.contains(GDE.TEMP_FILE_STEM)) {
					OperatingSystemHelper.createFileLink(fullQualifiedFilePath,
							OsdReaderWriter.application.getObjectFilePath() + fullQualifiedFilePath.substring(fullQualifiedFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1));
				}
			}
		}
		else {
			GDEInternalException e = new GDEInternalException(Messages.getString(MessageIds.GDE_MSGE0009) + activeChannel + ", " + fullQualifiedFilePath + ", " + useVersion); //$NON-NLS-1$
			DataExplorer.getInstance().openMessageDialogAsync(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * read record set data with given file seek pointer and record size
	 * @param recordSet
	 * @param filePath
	 * @throws DataInconsitsentException
	 */
	public static synchronized void readRecordSetsData(RecordSet recordSet, String filePath, boolean doUpdateProgressBar) throws FileNotFoundException, IOException, DataInconsitsentException {
		filePath = filePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)));
		ZipEntry zip_entry = zip_input.getNextEntry();
		RandomAccessFile random_in = null;
		DataInputStream data_in = null;
		try {
			long recordSetFileDataPointer = recordSet.getFileDataPointer();
			int recordFileDataSize = recordSet.getFileDataSize();
			long startTime = new Date().getTime();
			int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * recordFileDataSize;
			int dataSizeRecords = dataSizeRecord * recordSet.getNoneCalculationRecordNames().length;
			int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
			byte[] buffer = new byte[recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp];

			if (zip_entry != null) {
				data_in = new DataInputStream(zip_input);
				data_in.skip(recordSetFileDataPointer);
				data_in.readFully(buffer);
			}
			else {
				zip_input.close();
				zip_input = null;
				random_in = new RandomAccessFile(new File(filePath), "r"); //$NON-NLS-1$;
				random_in.seek(recordSetFileDataPointer);
				random_in.readFully(buffer);
			}
			recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordFileDataSize, doUpdateProgressBar);
			recordSet.updateVisibleAndDisplayableRecordsForTable();
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
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
			if (zip_input != null && data_in != null) {
				zip_input.closeEntry();
				data_in.close();
				zip_input.close();
			}
			else if (random_in != null) {
				random_in.close();
			}
			data_in = null;
			random_in = null;
			zip_input = null;
		}
	}

	/**
	 * Search through all data files and update the old object key with the new one,
	 * make backup of old *.osd as *.bak,
	 * keep osd file version unchanged
	 * @param oldObjectKey
	 * @param newObjectKey
	 */
	public static void updateObjectKey(String oldObjectKey, String newObjectKey) {
		DataInputStream data_in = null;
		DataOutputStream data_out = null;
		ZipInputStream zip_input = null;
		ZipOutputStream zip_out = null;

		try {
			List<File> files = FileUtils.getFileListing(new File(Settings.getInstance().getDataFilePath()), 1);
			Iterator<File> iterator = files.iterator();
			while (iterator.hasNext()) {
				File file = iterator.next();
				try {
					String actualFilePath = file.getAbsolutePath();
					if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
						log.finer(() -> String.format("working with %s", file.getName())); //$NON-NLS-1$
						if (file.getAbsolutePath().contains("Blade")) System.out.println();
						if (oldObjectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY))) {
							log.fine(() -> String.format("found file with given object key %s", file.getName())); //$NON-NLS-1$
						}
						else {
							iterator.remove();
						}
					}
					else {
						iterator.remove();
					}
				}
				catch (IOException e) {
					log.log(Level.WARNING, file.getAbsolutePath(), e);
				}
				catch (NotSupportedFileFormatException e) {
					log.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
				catch (Throwable t) {
					log.log(Level.WARNING, t.getLocalizedMessage(), t);
				}
			}

			//rename object key of remaining files
			iterator = files.iterator();
			while (iterator.hasNext()) {
				final String filePath = iterator.next().getPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				log.fine(() -> String.format("renaming object key of %s", filePath)); //$NON-NLS-1$
				String tmpFilePath = FileUtils.renameFile(filePath, GDE.FILE_ENDING_TMP); // rename existing file to *.tmp
				File tmpFile = new File(tmpFilePath);
				File updatedFile = new File(filePath);
				try {
					zip_input = new ZipInputStream(new FileInputStream(tmpFile));
					ZipEntry zip_entry = zip_input.getNextEntry();
					if (zip_entry != null) {
						data_in = new DataInputStream(zip_input);
					} else {
						data_in = new DataInputStream(new FileInputStream(tmpFile));
					}

					zip_out = new ZipOutputStream(new FileOutputStream(updatedFile));
					zip_out.putNextEntry(new ZipEntry(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1)));
					data_out = new DataOutputStream(zip_out);

					long filePointer = 0;
					String tmpData;
					tmpData = data_in.readUTF();
					String line = tmpData.substring(0, tmpData.length() - 1);
					log.finer(() -> String.format("OSD first line = %s", line));
					if (!line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) && !line.startsWith(GDE.LEGACY_FILE_VERSION))
						throw new NotSupportedFileFormatException(filePath);
					data_out.writeUTF(tmpData);
					filePointer += tmpData.getBytes("UTF8").length; //$NON-NLS-1$

					String stringVersion = line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) ? line.substring(GDE.DATA_EXPLORER_FILE_VERSION.length(), GDE.DATA_EXPLORER_FILE_VERSION.length() + 1).trim()
							: line.substring(GDE.LEGACY_FILE_VERSION.length(), GDE.LEGACY_FILE_VERSION.length() + 1).trim();
					int version;
					try {
						version = Integer.valueOf(stringVersion).intValue(); // one digit only
					}
					catch (NumberFormatException e) {
						log.log(Level.SEVERE, "can not interprete red version information " + stringVersion);
						throw new NotSupportedFileFormatException(filePath);
					}
					//read/write until object key is detected
					while (!(tmpData = data_in.readUTF()).startsWith(GDE.OBJECT_KEY)) {
						data_out.writeUTF(tmpData);
						filePointer += tmpData.getBytes("UTF8").length; //$NON-NLS-1$
					}
					//write the new object key
					StringBuilder sb = new StringBuilder();
					sb.append(GDE.OBJECT_KEY).append(newObjectKey).append(GDE.STRING_NEW_LINE);
					data_out.writeUTF(sb.toString());
					filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
					tmpData = data_in.readUTF();
					data_out.writeUTF(tmpData);
					filePointer += tmpData.getBytes("UTF8").length; //$NON-NLS-1$
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "filePointer = " + filePointer);

					//next line contains number of record sets
					int numberRecordSets = 0;
					if (tmpData.startsWith(GDE.RECORD_SET_SIZE)) {
						numberRecordSets = Integer.valueOf(tmpData.substring(GDE.RECORD_SET_SIZE.length()).trim()).intValue();
					}
					else {
						throw new NotSupportedFileFormatException(String.format("File %s does not contain number of record sets!", filePath));
					}
					//to correct the file pointers delta of bytes in object key length needs to be calculated
					int deltaSizeObjectKey = newObjectKey.getBytes("UTF8").length - oldObjectKey.getBytes("UTF8").length;

					//begin version dependent writing - read record set descriptors
					while (numberRecordSets-- > 0) {
						switch (version) {
						case 1:
						case 2:
						case 3:
							//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
							tmpData = data_in.readUTF();
							long dataPointer = Long.parseLong(tmpData.substring(tmpData.length() - 11).trim());
							tmpData = tmpData.substring(0, tmpData.length() - 11) + String.format("%10s\n", dataPointer + deltaSizeObjectKey);
							data_out.writeUTF(tmpData);
							break;

						default:
						case 4:
							//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
							int length = data_in.readInt();
							byte[] bytes = new byte[length];
							data_in.readFully(bytes);
							tmpData = new String(bytes);
							dataPointer = Long.parseLong(tmpData.substring(tmpData.length() - 11).trim());
							tmpData = tmpData.substring(0, tmpData.length() - 11) + String.format("%10s\n", dataPointer + deltaSizeObjectKey);
							data_out.writeInt(tmpData.getBytes("UTF8").length);
							data_out.write(tmpData.getBytes("UTF8"));
							break;
						}
					}

					//read/write until EOF binary data
					byte[] buffer = new byte[4096];
					int len = 0;
					while ((len = data_in.read(buffer)) > 0) {
						data_out.write(buffer, 0, len);
					}

					FileUtils.renameFile(tmpFilePath, GDE.FILE_ENDING_BAK); // rename existing file to *.bak
				}
				catch (FileNotFoundException e) {
					log.log(Level.WARNING, e.getMessage(), e);
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0038, new Object[] { e.getMessage() }));
				}
				catch (Exception e) {
					log.log(Level.WARNING, e.getMessage(), e);
					try {
						if (zip_out != null) {
							zip_out.closeEntry();
							if (data_out != null) {
								data_out.flush();
								data_out.close();
								data_out = null;
							}
							zip_out.close();
							zip_out = null;
						}
						if (updatedFile.exists()) if (updatedFile.delete()) log.log(Level.WARNING, "failed to delete " + filePath);
						if (data_in != null) {
							data_in.close();
							data_in = null;
						}
					}
					catch (IOException e1) {
						log.log(Level.WARNING, e1.getMessage(), e1);
					}
					FileUtils.renameFile(tmpFilePath, GDE.FILE_ENDING_OSD); // rename existing file to *.osd
				}
				finally {
					try {
						if (zip_out != null) {
							zip_out.closeEntry();
							if (data_out != null) {
								data_out.flush();
								data_out.close();
								data_out = null;
							}
							zip_out.close();
							zip_out = null;
						}
						if (data_in != null) {
							data_in.close();
							data_in = null;
						}
					}
					catch (IOException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}
		catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}
}
