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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
 ****************************************************************************************/
package gde.device.graupner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.istack.Nullable;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 *
 * @author Winfried Brügmann
 */
public class HoTTbinReader {
	final static String														$CLASS_NAME									= HoTTbinReader.class.getName();
	final static Logger														log													= Logger.getLogger(HoTTbinReader.$CLASS_NAME);
	// 64 byte = 0.01 seconds for 40 seconds maximum sensor scan time (40 / 0.01 = 4000)
	protected static final int										LOG_RECORD_SCAN_START				= 4000;
	protected static final int										NUMBER_LOG_RECORDS_TO_SCAN	= 1500;
	protected static final int										NUMBER_LOG_RECORDS_MIN			= 7000;

	final static DataExplorer											application									= DataExplorer.getInstance();
	final static Channels													channels										= Channels.getInstance();

	static int																		dataBlockSize								= 64;
	static byte[]																	buf;
	static byte[]																	buf0, buf1, buf2, buf3, buf4, buf5, buf6, buf7, buf8, buf9, bufA, bufB, bufC, bufD;
	static long																		timeStep_ms;
	static RecordSet															recordSetReceiver, recordSetGAM, recordSetEAM, recordSetVario, recordSetGPS, recordSetChannel, recordSetESC, recordSetESC2, recordSetESC3, recordSetESC4;
	// todo remove the next lines which are used neither by this class nor by HoTTbinHistoReader
	static int[]																	pointsReceiver, pointsEAM, pointsVario, pointsGPS, pointsChannel, pointsESC, pointsGAM;
	static int																		tmpVoltageRx								= 0;
	static int																		tmpTemperatureRx						= 0;
	static int																		tmpHeight										= 0;
	static int																		tmpTemperatureFet						= 0;
	static int																		tmpTemperatureExt						= 0;
	static int																		tmpVoltage									= 0;
	static int																		tmpCurrent									= 0;
	static int																		tmpRevolution								= 0;
	static int																		tmpClimb1										= 0;
	static int																		tmpClimb3										= 0;
	static int																		tmpClimb10									= 0;
	static int																		tmpVoltage1									= 0;
	static int																		tmpVoltage2									= 0;
	static int																		tmpCapacity									= 0;
	static int																		tmpVelocity									= 0;
	static int																		tmpLatitude									= 0;
	static int																		tmpLatitudeDelta						= 0;
	static double																	latitudeTolerance						= 1;
	static long																		lastLatitudeTimeStep				= 0;
	static int																		tmpLongitude								= 0;
	static int																		tmpLongitudeDelta						= 0;
	static double																	longitudeTolerance					= 1;
	static long																		lastLongitudeTimeStep				= 0;

	static boolean																isJustParsed								= false;
	static boolean																isReceiverOnly							= false;
	static EnumSet<Sensor> 												detectedSensors;
	static boolean																isTextModusSignaled					= false;
	static int																		oldProtocolCount						= 0;
	static Vector<Byte>														blockSequenceCheck;

	protected static final Map<String, RecordSet>	recordSets									= new HashMap<String, RecordSet>();

	protected static PickerParameters							pickerParameters;
	protected static BinParser										rcvBinParser, chnBinParser, varBinParser, gpsBinParser, gamBinParser, eamBinParser, escBinParser;
	
	/**
	 * Individual settings for the SD Log container format ('GRAUPNER SD LOG').
	 * @author Thomas Eickert (USER)
	 */
	public static class SdLogFormat {
		private final String	starterText	= "GRAUPNER SD LOG";
		private final int			headerSize;
		private final int			footerSize;
		private final int			dataBlockSize;

		public SdLogFormat(int headerSize, int footerSize, int dataBlockSize) {
			this.headerSize = headerSize;
			this.footerSize = footerSize;
			this.dataBlockSize = dataBlockSize;
		}
	}

	/**
	 * Adapt a data input stream to a SD Log container format ('GRAUPNER SD LOG').
	 * Check the format basics.
	 * @author Thomas Eickert (USER)
	 */
	public static class SdLogInputStream extends FilterInputStream {

		private final SdLogFormat	sdLogFormat;
		private final long				payloadSize;
		private final long				posMax;
		private long							pos		= 0;
		private long							mark	= 0;

		/**
		 * Create a <code>FilterInputStream</code> respecting the header and footer bytes of a SD log file.
		 * Check the file format.
		 * @param in is a markSupported input stream
		 * @param fileLength is the length of the file which is represented by the <code>in</code> stream
		 */
		public SdLogInputStream(FilterInputStream in, long fileLength, SdLogFormat sdLogFormat) throws DataTypeException, IOException {
			super(in);
			this.sdLogFormat = sdLogFormat;
			posMax = fileLength - sdLogFormat.footerSize - 1;
			payloadSize = fileLength - sdLogFormat.headerSize - sdLogFormat.footerSize;
			if (payloadSize < 0) throw new DataTypeException("file size less than header/footer size");

			if (payloadSize % sdLogFormat.dataBlockSize != 0) throw new DataTypeException("data block size does not match");

			if (!in.markSupported()) throw new DataTypeException("mark/reset not supported");

			byte[] buffer = skipHeader();
			if (!new String(buffer).startsWith(sdLogFormat.starterText)) throw new DataTypeException("starter text does not match");

			if (!verifyHoTTFormat()) throw new DataTypeException("data block counter does not match");
		}

		protected boolean verifyHoTTFormat() throws IOException {
			mark(1 + sdLogFormat.dataBlockSize * 4);
			byte[] buffer;
			boolean isHoTT = true;
			buffer = new byte[sdLogFormat.dataBlockSize];
			for (int i = 0; i < 4; i++) {
				read(buffer);
				if (buffer[0] != i + 1) {
					isHoTT = false;
					break;
				}
			}
			reset();
			return isHoTT;
		}

		@Override
		public synchronized int read() throws IOException {
			if (posMax - pos <= 0) return -1;
			int b = in.read();
			if (b >= 0) pos += 1;
			return b;
		}

		@Override
		public int read(byte b[]) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			long r = posMax - pos;
			if (r <= 0) return -1;
			int i = in.read(b, off, len);
			if (i > r) {
				i = (int) r;
			}
			if (i > 0) pos += i;
			return i;
		}

		@Override
		public synchronized long skip(long n) throws IOException {
			long i = in.skip(n);
			if (i > 0) {
				pos += i;
			}
			return i;
		}

		@Override
		public synchronized void mark(int readlimit) {
			in.mark(readlimit);
			mark = pos;
		}

		@Override
		public synchronized void reset() throws IOException {
			in.reset();
			pos = mark;
		}

		private byte[] skipHeader() throws IOException {
			byte[] buffer = new byte[sdLogFormat.headerSize];
			in.read(buffer);
			return buffer;
		}

		/**
		 * @return the number of bytes of the embedded payload data
		 */
		public long getPayloadSize() {
			return this.payloadSize;
		}

	}

	/**
	 * Copy the buffer array into buf* arrays based on the data block number.
	 * @author Thomas Eickert (USER)
	 */
	public static class BufCopier {

		private final byte[]		buf;
		private final byte[]		buf0;
		private final byte[]		buf1;
		private final byte[]		buf2;
		private final byte[]		buf3;
		private final byte[]		buf4;
		private final boolean[]	isBufReady;

		/**
		 * @param buf is the source buffer array
		 * @param buf0 to buf* are the target data blocks
		 */
		public BufCopier(byte[] buf, byte[] buf0, byte[] buf1, byte[] buf2, byte[] buf3, byte[] buf4) {
			this.buf = buf;
			this.buf0 = buf0;
			this.buf1 = buf1;
			this.buf2 = buf2;
			this.buf3 = buf3;
			this.buf4 = buf4;
			this.isBufReady = new boolean[5];
		}

		public void clearBuffers() {
			isBufReady[1] = isBufReady[2] = isBufReady[3] = isBufReady[4] = false;
		}

		public boolean is4BuffersFull() {
			return isBufReady[1] && isBufReady[2] && isBufReady[3] && isBufReady[4];
		}

		/**
		 * Fill data block 0 to 4 whether it is clear or not.
		 */
		public void copyToBuffer() {
			switch (buf[33]) { // data block number
			case 0:
				// fill data block 0 receiver voltage and temperature
				if (DataParser.parse2Short(buf, 0) != 0) {
					System.arraycopy(buf, 34, buf0, 0, buf0.length);
				}
				break;
			case 1:
				System.arraycopy(buf, 34, buf1, 0, buf1.length);
				break;
			case 2:
				System.arraycopy(buf, 34, buf2, 0, buf2.length);
				break;
			case 3:
				System.arraycopy(buf, 34, buf3, 0, buf3.length);
				break;
			case 4:
				System.arraycopy(buf, 34, buf4, 0, buf4.length);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		/**
		 * Fill data block 0 to 4 if it is clear.
		 */
		public void copyToFreeBuffer() {
			switch (buf[33]) { // data block number
			case 0:
				break;
			case 1:
				if (!isBufReady[1]) {
					isBufReady[1] = true;
					System.arraycopy(buf, 34, buf1, 0, buf1.length);
				}
				break;
			case 2:
				if (!isBufReady[2]) {
					isBufReady[2] = true;
					System.arraycopy(buf, 34, buf2, 0, buf2.length);
				}
				break;
			case 3:
				if (!isBufReady[3]) {
					isBufReady[3] = true;
					System.arraycopy(buf, 34, buf3, 0, buf3.length);
				}
				break;
			case 4:
				if (!isBufReady[4]) {
					isBufReady[4] = true;
					System.arraycopy(buf, 34, buf4, 0, buf4.length);
				}
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		/**
		 * Fill data block 0 to 2 whether it is clear or not.
		 * fill data block 3 and 4 if HoTTAdapterD is involved
		 */
		public void copyToVarioBuffer() {
			switch (buf[33]) { // data block number
			case 0:
				break;
			case 1:
				isBufReady[1] = true;
				System.arraycopy(buf, 34, buf1, 0, buf1.length);
				break;
			case 2:
				isBufReady[2] = true;
				System.arraycopy(buf, 34, buf2, 0, buf2.length);
				break;
			case 3:
				isBufReady[3] = true;
				System.arraycopy(buf, 34, buf3, 0, buf3.length);
				break;
			case 4:
				isBufReady[4] = true;
				System.arraycopy(buf, 34, buf4, 0, buf4.length);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * get data file info data
	 *
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException
	 * @throws DataTypeException
	 */
	public static HashMap<String, String> getFileInfo(File file, PickerParameters newPickerParameters) throws IOException, DataTypeException {
		HashMap<String, String> fileInfo = null;
		HoTTbinReader.pickerParameters = newPickerParameters;
		try (FilterInputStream data_in = new BufferedInputStream(new FileInputStream(file))) {
			Consumer<String> messageProvider = (s) -> DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(s));
			fileInfo = new InfoParser(messageProvider).getFileInfo(data_in, file.getPath(), file.length());
		}
		return fileInfo;
	}

	/**
	 * Parse the native file header information.
	 * @author Thomas Eickert (USER)
	 */
	public static class InfoParser {

		private final Consumer<String>	messageProvider;

		/**
		 * @param messageProvider takes a message ID and produces a UI message box
		 */
		public InfoParser(Consumer<String> messageProvider) {
			this.messageProvider = messageProvider;
		}

		/**
		 * @param data_in is the input stream which is consumed but not closed
		 * @param filePath
		 * @param fileLength
		 * @return a hash map containing header data as string accessible by public header keys
		 */
		@Nullable
		private HashMap<String, String> getFileInfoLog(FilterInputStream data_in, String filePath, long fileLength)
				throws UnsupportedEncodingException, IOException {
			HashMap<String, String> fileInfo = new HashMap<String, String>();
			fileInfo.put(HoTTAdapter.FILE_PATH, filePath);
			EnumSet<Sensor> sensors = EnumSet.of(Sensor.RECEIVER);

			data_in.reset();
			data_in.mark(999);
			byte[] buffer = new byte[64];
			data_in.read(buffer);

			// read header size
			String preHeader = new String(buffer);
			int indexOf = preHeader.indexOf("LOG DATA OFFSET : ");
			if (indexOf > 10) {
				int logDataOffset = Integer.valueOf(preHeader.substring(indexOf + 18, indexOf + 18 + 8));

				data_in.reset();
				StringBuilder sb = new StringBuilder();
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(data_in, "ISO-8859-1")); //$NON-NLS-1$
				while ((line = reader.readLine()) != null && sb.append(line).append(GDE.STRING_NEW_LINE).length() < logDataOffset) {
					if (line.contains(": ") && line.indexOf(GDE.CHAR_COLON) > 5) {
						String key = line.split(": ")[0].trim();
						String value = null;
						try {
							value = line.split(": ")[1].trim();
						}
						catch (Exception e) {
							// ignore and skip this entry
						}
						if (value != null) {
							fileInfo.put(key, value);
							if (log.isLoggable(Level.FINE))
								log.log(Level.FINE, String.format("%16s : %s", line.split(": ")[0].trim(), fileInfo.get(line.split(": ")[0].trim())));
						}
					}
				}
				reader.close();

				// now the input stream is closed as well
				try (FilterInputStream data_in1 = new BufferedInputStream(new FileInputStream(filePath))) {
					int numberLogChannels = Integer.valueOf(fileInfo.get("LOG NOB CHANNEL"));
					boolean isASCII = fileInfo.get("LOG TYPE").contains("ASCII");
					int rawDataBlockSize = 66 + numberLogChannels * 2;
					fileInfo.put(HoTTAdapter.RAW_LOG_SIZE, GDE.STRING_EMPTY + rawDataBlockSize);
					int asciiDataBlockSize = 202 + numberLogChannels * 5;
					fileInfo.put(HoTTAdapter.ASCII_LOG_SIZE, GDE.STRING_EMPTY + asciiDataBlockSize);
					int dataBlockSize = isASCII ? asciiDataBlockSize : rawDataBlockSize;
					fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (((fileLength - logDataOffset) / dataBlockSize) - 1));
					fileInfo.put(HoTTAdapter.DATA_BLOCK_SIZE, GDE.STRING_EMPTY + dataBlockSize);
					
					//TODO scan sensors since value of detected sensor may be wrong
					buffer = new byte[dataBlockSize];
					data_in1.skip(logDataOffset);
	
					for (int i = 0; i < NUMBER_LOG_RECORDS_TO_SCAN/10 && data_in1.available() >= dataBlockSize; i++) {
						data_in1.read(buffer);
						
						if (isASCII) { //convert ASCII log data to hex
							HoTTlogReader.convertAscii2Raw(rawDataBlockSize, buffer);
						}
						
						if (buffer[24] != 0x1F) {
							Sensor tmpSensor = Sensor.fromSensorByte(buffer[26]);
							if (tmpSensor != null) {
								sensors.add(tmpSensor);
								if (HoTTbinReader.log.isLoggable(Level.FINE)) {
									HoTTbinReader.log.log(Level.FINE, StringHelper.byte2Hex4CharString(buffer, buffer.length));
									HoTTbinReader.log.log(Level.FINE, String.format("SensorByte  %02X", buffer[26]));
								}
							}
						}
					}
					data_in1.close();
				}
				fileInfo.put(HoTTAdapter.DETECTED_SENSOR, Sensor.getSetAsDetected(sensors));
			}
			return fileInfo;
		}

	/**
	 * Get data file info data.
	 * @param data_in is the input stream which is consumed but not closed.
	 * @return a hash map containing header data as string accessible by public header keys
	 */
	public HashMap<String, String> getFileInfo(FilterInputStream data_in, String filePath, long fileLength) throws IOException, DataTypeException {
		final HashMap<String, String> fileInfo;
		data_in.mark(64000);
		byte[] buffer = new byte[64];
		data_in.read(buffer);
		if (filePath.endsWith(GDE.FILE_ENDING_BIN) && new String(buffer).startsWith("GRAUPNER SD LOG")) {
			// begin evaluate for HoTTAdapterX files containing normal HoTT V4 sensor data
			try {
				data_in.reset();
				SdLogFormat sdLogFormat = new SdLogFormat(HoTTbinReaderX.headerSize, HoTTbinReaderX.footerSize, 64);
				SdLogInputStream sdLogInputStream = new SdLogInputStream(data_in, fileLength, sdLogFormat);
				fileInfo = getFileInfo(sdLogInputStream, filePath, fileLength);
				return fileInfo;
			} catch (DataTypeException e) {
				// includes diverse format exceptions
			}
			this.messageProvider.accept(gde.device.graupner.hott.MessageIds.GDE_MSGW2410);
			throw new DataTypeException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410));
		}
		else if (filePath.endsWith(GDE.FILE_ENDING_LOG) && new String(buffer).startsWith("FILE TAG IDVER")) {
			fileInfo = getFileInfoLog(data_in, filePath, fileLength);
		}
		else if (filePath.endsWith(GDE.FILE_ENDING_LOG)) {
			this.messageProvider.accept(MessageIds.GDE_MSGW0021);
			throw new DataTypeException(Messages.getString(MessageIds.GDE_MSGW0021));
		} else { // *.log do not need a sensor scan.
			long numberLogs = (fileLength / 64);
			if (numberLogs < NUMBER_LOG_RECORDS_MIN) {
				this.messageProvider.accept(gde.device.graupner.hott.MessageIds.GDE_MSGW2406);
			}
			else if (numberLogs < LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) {
				this.messageProvider.accept(gde.device.graupner.hott.MessageIds.GDE_MSGW2407);
				throw new IOException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407));
			}
			fileInfo = getFileInfoBin(data_in, filePath, fileLength);
		}
		return fileInfo;
	}

	private static HashMap<String, String> getFileInfoBin(FilterInputStream data_in, String filePath, long fileLength) throws IOException {
		HashMap<String, String> fileInfo = new HashMap<String, String>();

		fileInfo.put(HoTTAdapter.SD_FORMAT, Boolean.toString(data_in instanceof SdLogInputStream));
		fileInfo.put(HoTTAdapter.FILE_PATH, filePath);
		EnumSet<Sensor> sensors = EnumSet.of(Sensor.RECEIVER);

		data_in.reset();
		byte[] buffer = new byte[64];
		data_in.read(buffer);
		long position = (fileLength / 2) - ((NUMBER_LOG_RECORDS_TO_SCAN * 64) / 2);
		if (data_in instanceof SdLogInputStream) 
			position = ((fileLength - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize) / 2) - ((NUMBER_LOG_RECORDS_TO_SCAN * 64) / 2);
		else 	
			position = position - position % 64;
		if (position > 0) {
			position = position - 64;
			position = position <= 64 ? 64 : position;
			
			long skipped = 0, iterationCount = -1;
			while ((skipped = data_in.skip(position)) < position && iterationCount < 8) { //check skipped bytes while sensor detection problems
				position -= skipped;
				++iterationCount;
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "position " + position + " skipped " + skipped);
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "position " + position + " skipped " + skipped);
			
			for (int i = 0; i < NUMBER_LOG_RECORDS_TO_SCAN && data_in.available() >= 64; i++) {
				data_in.read(buffer);
				Sensor tmpSensor = Sensor.fromSensorByte(buffer[7]);
				if (tmpSensor != null) sensors.add(tmpSensor);
				if (HoTTbinReader.log.isLoggable(Level.FINER)) {
					HoTTbinReader.log.log(Level.FINER, StringHelper.byte2Hex4CharString(buffer, buffer.length));
					HoTTbinReader.log.log(Level.FINER, String.format("SensorByte  %02X", buffer[7]));
				}
			}
		}
		if (data_in instanceof SdLogInputStream) 
			fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + ((fileLength - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize) / HoTTbinReader.dataBlockSize));
		else 	
			fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (fileLength / HoTTbinReader.dataBlockSize));
		fileInfo.put(HoTTAdapter.DETECTED_SENSOR, Sensor.getSetAsDetected(sensors));
		if (HoTTbinReader.log.isLoggable(Level.FINE))
			for (Entry<String, String> entry : fileInfo.entrySet()) {
				HoTTbinReader.log.log(Level.FINE, entry.getKey() + " = " + entry.getValue());
				HoTTbinReader.log.log(Level.FINE, Paths.get(filePath).getFileName().toString() + " - " + "sensor count = " + sensors.size());
			}
		return fileInfo;
	}
}

	// 0=Rx dbm to Strength lookup table
	static int[] lookup = new int[] { 95, 95, 95, 95, 95, 95, 95, 95, 95, 95, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 85, 85, 85, 85, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 30, 25, 25, 20, 20,
			20, 15, 15, 10, 10, 5, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0 };
	/**
	 * convert from Rx dbm to strength using lookup table
	 *
	 * @param inValue
	 * @return
	 */
	static int convertRxDbm2Strength(int inValue) {
		if (inValue >= 40 && inValue < HoTTbinReader.lookup.length + 40) {
			return HoTTbinReader.lookup[inValue - 40];
		}
		else if (inValue < 40)
			return 100;
		else
			return 0;
	}

	/**
	 * set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
	 */
	protected static void setAltitudeClimbPickeParameter(PickerParameters pickerParameters, EnumSet<Sensor>	detectedSensors) {
		if (pickerParameters.altitudeClimbSensorSelection == 0) {
			boolean isSensorDetected = false;
			for (Sensor sensor : detectedSensors) {
				switch (sensor) {
				case VARIO:
					pickerParameters.altitudeClimbSensorSelection = sensor.ordinal();
					isSensorDetected = true;
					break;
				case GPS:
					pickerParameters.altitudeClimbSensorSelection = sensor.ordinal();
					isSensorDetected = true;
					break;
				case GAM:
					pickerParameters.altitudeClimbSensorSelection = sensor.ordinal();
					isSensorDetected = true;
					break;
				case EAM:
					pickerParameters.altitudeClimbSensorSelection = sensor.ordinal();
					isSensorDetected = true;
					break;
				default: //sensor does not provide altitude and climb values
					break;
				}
				if (isSensorDetected)
					break;
			}
		}
		else { //sensor already selected by user, check if part of detected sensors
			boolean isSensorContained = false;
			for (Sensor sensor : detectedSensors) {
				if (pickerParameters.altitudeClimbSensorSelection == sensor.ordinal()) {
					isSensorContained = true;
					break;
				}
			}
			if (!isSensorContained) { //sensor selected which is not part of detected sensors
				pickerParameters.altitudeClimbSensorSelection = 0; //auto
				//reverse call set picker parameter for altitude/climb sensor selection
				HoTTbinReader.setAltitudeClimbPickeParameter(pickerParameters, detectedSensors);
			}
		}
		log.log(Level.INFO, String.format("pickerParameters.altitudeClimbSensorSelection = %s", Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection).name()));
	}

	/**
	 * read complete file data and display the first found record set
	 *
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		HoTTbinReader.pickerParameters = newPickerParameters;
		HashMap<String, String> header = getFileInfo(new File(filePath), newPickerParameters);
		HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		//set fix detected sensors if sensor detection fails!
		//HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected("RECEIVER,GPS,AIR_ESC");

		if (HoTTbinReader2.detectedSensors.size() <= 2) {
			HoTTbinReader.isReceiverOnly = HoTTbinReader2.detectedSensors.size() == 1;
			readSingle(new File(filePath), header);
		} else {
			readMultiple(new File(filePath), header);
		}
	}

	/**
	 * read log data according to version 0
	 *
	 * @param file
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx 9=Event Rx
		HoTTbinReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event 8=accX 9=accY 10=accZ 11=reserved 12=version
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS 15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc_ESC_15 29=VersionESC
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] {0};
		HoTTbinReader.rcvBinParser = Sensor.RECEIVER.createBinParser(HoTTbinReader.pickerParameters, new int[10], timeSteps_ms, new byte[][] { buf });
		HoTTbinReader.varBinParser = Sensor.VARIO.createBinParser(HoTTbinReader.pickerParameters, new int[13], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gpsBinParser = Sensor.GPS.createBinParser(HoTTbinReader.pickerParameters, new int[24], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gamBinParser = Sensor.GAM.createBinParser(HoTTbinReader.pickerParameters, new int[26], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.eamBinParser = Sensor.EAM.createBinParser(HoTTbinReader.pickerParameters, new int[31], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.escBinParser = Sensor.ESC.createBinParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		int version = -1;
		HoTTbinReader.isJustParsed = false;
		HoTTbinReader.isTextModusSignaled = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean isWrongDataBlockNummerSignaled = false;
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		numberDatablocks = HoTTbinReader.isReceiverOnly && !HoTTbinReader.pickerParameters.isChannelsChannelEnabled ? numberDatablocks / 10 : numberDatablocks;
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			HoTTbinReader.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			String newFileDescription = HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReader.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReader.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data

			if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
				// channel data are always contained
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				HoTTbinReader.chnBinParser = Sensor.CHANNEL.createBinParser(HoTTbinReader.pickerParameters, new int[HoTTbinReader.recordSetChannel.size()], timeSteps_ms, new byte[][] { buf });
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (GDE.isWithUi()) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTbinReader.pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader.log.isLoggable(Level.INFO)) 
							HoTTbinReader.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINER)) HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}
						if (HoTTbinReader.isReceiverOnly && !HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
							for (int j = 0; j < 9; j++) { //skip 9 data packets
								data_in.read(HoTTbinReader.buf);
								timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
							}
						}
						// fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0) {
							bufCopier.copyToBuffer();
						}
						
						// log.log(Level.INFO, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinReader.buf[7] & 0xFF)}, 1));
						if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4) { // expected data block number
							switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (detectedSensors.contains(Sensor.VARIO)) {
									// check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetVario == null) {
										channel = HoTTbinReader.channels.get(2);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
												? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
												: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
										HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetVario);
										HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetVario initialized and ready to add data
									bufCopier.copyToVarioBuffer();
									if (bufCopier.is4BuffersFull()) {
										version = parseAddVario(version, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										bufCopier.clearBuffers();
									}
									if (!isVarioDetected) {
										HoTTAdapter.updateVarioTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetVario);
										isVarioDetected = true;								
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (detectedSensors.contains(Sensor.GPS)) {
									// check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate as signals
									if (HoTTbinReader.recordSetGPS == null) {
										channel = HoTTbinReader.channels.get(3);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
												? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
												: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGPS);
										HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetGPS initialized and ready to add data
									bufCopier.copyToFreeBuffer();
									if (bufCopier.is4BuffersFull()) {
										parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										bufCopier.clearBuffers();
									}
									if (!isGPSdetected) {
										if (isReasonableData(buf4) && HoTTbinReader.recordSetGPS.get(19).size() > 0 && HoTTbinReader.recordSetGPS.get(19).get(HoTTbinReader.recordSetGPS.get(19).size()-1) != 0) {
											startTimeStamp_ms = HoTTAdapter.updateGpsTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetGPS, startTimeStamp_ms);
											isGPSdetected = true;
										}
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (detectedSensors.contains(Sensor.GAM)) {
									// check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetGAM == null) {
										channel = HoTTbinReader.channels.get(4);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
												? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
												: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGAM = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGAM);
										HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGAM);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetGeneral initialized and ready to add data
									bufCopier.copyToFreeBuffer();
									if (bufCopier.is4BuffersFull()) {
										parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										bufCopier.clearBuffers();
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (detectedSensors.contains(Sensor.EAM)) {
									// check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetEAM == null) {
										channel = HoTTbinReader.channels.get(5);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
												? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
												: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
										HoTTbinReader.recordSetEAM = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetEAM);
										HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetEAM);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetElectric initialized and ready to add data
									bufCopier.copyToFreeBuffer();
									if (bufCopier.is4BuffersFull()) {
										parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										bufCopier.clearBuffers();
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (detectedSensors.contains(Sensor.ESC)) {
									// check if recordSetMotorDriver initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetESC == null) {
										channel = HoTTbinReader.channels.get(7);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
												? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
												: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
										HoTTbinReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetESC);
										HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetESC);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetMotorDriver initialized and ready to add data
									bufCopier.copyToFreeBuffer();
									if (bufCopier.is4BuffersFull()) {
										parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										bufCopier.clearBuffers();
									}
									
									if (!isESCdetected) {
										HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetESC);
										isESCdetected = true;
									}
								}
								break;
							}
						}
						else {
							if (!isWrongDataBlockNummerSignaled) {
								application.openMessageDialogAsync(String.format("Datenblocknummer ausserhalb des Bereichs von 0 bis 5 (%d)", buf[33]));
								isWrongDataBlockNummerSignaled = true;
							}
							HoTTbinReader.isJustParsed = true;
						}
						
						if (HoTTbinReader.isJustParsed) {
							HoTTbinReader.isJustParsed = !((RcvBinParser) HoTTbinReader.rcvBinParser).updateLossStatistics();
						}
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from device of 10 msec
						
						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					}
					else { // skip empty block, but add time step
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
						
						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);
						if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			((RcvBinParser) HoTTbinReader.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().percentage)
					: "100";
			if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled)
				HoTTbinReader.detectedSensors.add(Sensor.CHANNEL);
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription() 
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReader.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTbinReader2.detectedSensors.toString()));
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (GDE.isWithUi()) {
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		return getRecordSetExtend(file.getName());
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param fileName
	 * @return
	 */
	protected static String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.indexOf(GDE.CHAR_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.indexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * read log data according to version 0
	 *
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readMultiple(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event 8=accX 9=accY 10=accZ 11=reserved 12=version
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS 15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
		HoTTbinReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc ESC_15 29=VersionESC
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] {0};
		HoTTbinReader.rcvBinParser = Sensor.RECEIVER.createBinParser(HoTTbinReader.pickerParameters, new int[10], timeSteps_ms, new byte[][] { buf });
		HoTTbinReader.varBinParser = Sensor.VARIO.createBinParser(HoTTbinReader.pickerParameters, new int[13], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gpsBinParser = Sensor.GPS.createBinParser(HoTTbinReader.pickerParameters, new int[24], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gamBinParser = Sensor.GAM.createBinParser(HoTTbinReader.pickerParameters, new int[26], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.eamBinParser = Sensor.EAM.createBinParser(HoTTbinReader.pickerParameters, new int[31], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.escBinParser = Sensor.ESC.createBinParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		HoTTbinReader.isJustParsed = false;
		HoTTbinReader.isTextModusSignaled = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			HoTTbinReader.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			String newFileDescription = HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReader.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReader.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data
			// channel data are always contained
			if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
						? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				HoTTbinReader.chnBinParser = Sensor.CHANNEL.createBinParser(HoTTbinReader.pickerParameters, new int[HoTTbinReader.recordSetChannel.size()], timeSteps_ms, new byte[][] { buf });
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (GDE.isWithUi()) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTbinReader.pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader.log.isLoggable(Level.INFO)) 
							HoTTbinReader.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
						
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINEST)) HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						// detect sensor switch
						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							// write data just after sensor switch
							if (logCountVario >= 5 || logCountGPS >= 5 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (detectedSensors.contains(Sensor.VARIO)) {
										// check if recordSetVario initialized, transmitter and receiver data always
										// present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetVario == null) {
											channel = HoTTbinReader.channels.get(2);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
											HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetVario);
											HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetVario initialized and ready to add data
										parseAddVario(1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										if (!isVarioDetected) {
											HoTTAdapter.updateVarioTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetVario);
											isVarioDetected = true;								
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (detectedSensors.contains(Sensor.GPS)) {
										// check if recordSetReceiver initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetGPS == null) {
											channel = HoTTbinReader.channels.get(3);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGPS);
											HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGPS initialized and ready to add data
										parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);

										if (!isGPSdetected) {
											if (isReasonableData(buf4) && HoTTbinReader.recordSetGPS.get(19).size() > 0 && HoTTbinReader.recordSetGPS.get(19).get(HoTTbinReader.recordSetGPS.get(19).size()-1) != 0) {
												startTimeStamp_ms = HoTTAdapter.updateGpsTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetGPS, startTimeStamp_ms);
												isGPSdetected = true;
											}
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (detectedSensors.contains(Sensor.GAM)) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetGAM == null) {
											channel = HoTTbinReader.channels.get(4);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGAM = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGAM);
											HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGAM);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGeneral initialized and ready to add data
										parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (detectedSensors.contains(Sensor.EAM)) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetEAM == null) {
											channel = HoTTbinReader.channels.get(5);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetEAM = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetEAM);
											HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetEAM);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and ready to add data
										parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (detectedSensors.contains(Sensor.ESC)) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetESC == null) {
											channel = HoTTbinReader.channels.get(7);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
											HoTTbinReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetESC);
											HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetESC);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and ready to add data
										parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);

										if (!isESCdetected) {
											HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf4[9] & 0xFF), device, HoTTbinReader.recordSetESC);
											isESCdetected = true;
										}
									}
									break;
								}
							}

							if (HoTTbinReader.log.isLoggable(Level.FINE))
								HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = "
										+ logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
						}

						switch (lastSensor) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							++logCountVario;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							++logCountGPS;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							++logCountGeneral;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountElectric;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountSpeedControl;
							break;
						}

						bufCopier.copyToBuffer();

						if (HoTTbinReader.isJustParsed) {
							HoTTbinReader.isJustParsed = !((RcvBinParser) HoTTbinReader.rcvBinParser).updateLossStatistics();
						}

						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec

						if (i % progressIndicator == 0)
							GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					}
					else { // tx,rx == 0
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
						
						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);
						if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReader.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405,
			// new Object[] { HoTTbinReader.oldProtocolCount }));
			// }
			((RcvBinParser) HoTTbinReader.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().percentage)
					: "100";
			if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled)
				HoTTbinReader.detectedSensors.add(Sensor.CHANNEL);
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription() 
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReader.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTbinReader2.detectedSensors.toString()));
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (GDE.isWithUi()) {
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddReceiver(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.rcvBinParser.parse();
		HoTTbinReader.recordSetReceiver.addPoints(HoTTbinReader.rcvBinParser.getPoints(), HoTTbinReader.rcvBinParser.getTimeStep_ms());
	}

	public static class RcvBinParser extends BinParser {
		private int																	tmpVoltageRx			= 0;
		private int																	tmpTemperatureRx	= 0;

		/**
		 * the number of lost packages since the last valid package
		 */
		private int																	consecutiveLossCounter	= 0;

		private PackageLoss	lostPackages			= new PackageLoss();

		protected final byte[]											_buf;

		protected RcvBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.RECEIVER);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx,
			// 6=VoltageRx, 7=TemperatureRx, 8=VoltageRx_min
			this.tmpVoltageRx = (_buf[35] & 0xFF);
			this.tmpTemperatureRx = (_buf[36] & 0xFF);
			this.points[1] = (_buf[38] & 0xFF) * 1000;
			this.points[3] = DataParser.parse2Short(_buf, 40) * 1000;
			if (isPointsValid()) {
				this.points[2] = (convertRxDbm2Strength(_buf[4] & 0xFF)) * 1000;
				this.points[4] = (_buf[3] & 0xFF) * -1000;
				this.points[5] = (_buf[4] & 0xFF) * -1000;
				this.points[6] = (_buf[35] & 0xFF) * 1000;
				this.points[7] = ((_buf[36] & 0xFF) - 20) * 1000;
				this.points[8] = (_buf[39] & 0xFF) * 1000;
				if ((_buf[32] & 0x40) > 0 || (_buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
					this.points[9] = (_buf[32] & 0x65) * 1000; // warning E,V,T only
				else
					this.points[9] = 0;
				return true;
			}
			if ((_buf[32] & 0x40) > 0 || (_buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
				this.points[9] = (_buf[32] & 0x65) * 1000; // warning E,V,T only
			else
				this.points[9] = 0;
			return false;
		}

		/**
		 * @param isAvailable true if the package is not lost
		 */
		public void trackPackageLoss(boolean isAvailable) {
			if (isAvailable) {
				this.pickerParameters.reverseChannelPackageLossCounter.add(1);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;
			} else {
				this.pickerParameters.reverseChannelPackageLossCounter.add(0);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

				++this.consecutiveLossCounter;
			}
			++this.lostPackages.numberTrackedSamples;
		}

		/**
		 * @return true if the lost packages count is transferred into the loss statistics
		 */
		public boolean updateLossStatistics() {
			if (this.consecutiveLossCounter > 0) {
				this.lostPackages.add(this.consecutiveLossCounter);
				this.consecutiveLossCounter = 0;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * update packets loss statistics before reading statistics values
		 */
		public void finalUpdateLossStatistics() {
			this.lostPackages.percentage = this.lostPackages.lossTotal * 100. / (this.lostPackages.numberTrackedSamples - this.consecutiveLossCounter);
			log.log(Level.INFO, String.format("lostPackages = (%d) %d of %d percentage = %3.1f", this.lostPackages.lossTotal + this.consecutiveLossCounter,
					this.lostPackages.lossTotal, this.lostPackages.numberTrackedSamples, this.lostPackages.percentage ));
		}

		/**
		 * @return the total number of lost packages (is summed up while reading the log)
		 */
		public int getLossTotal() {
			return this.lostPackages.lossTotal + this.consecutiveLossCounter;
		}

		public PackageLoss getLostPackages() {
			return this.lostPackages;
		}

		private boolean isPointsValid() {
			return !pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 120;
		}

		@Override
		public String toString() {
			return super.toString() + "  [lossTotal=" + this.lostPackages.lossTotal + ", consecutiveLossCounter=" + this.consecutiveLossCounter + "]";
		}

	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.chnBinParser.parse();
		HoTTbinReader.recordSetChannel.addPoints(HoTTbinReader.chnBinParser.getPoints(), HoTTbinReader.chnBinParser.getTimeStep_ms());
	}

	public static class ChnBinParser extends BinParser {
		protected final byte[] _buf;

		protected ChnBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.CHANNEL);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=FreCh, 1=Tx, 2=Rx, 3=Ch01, 4=Ch02 .. 18=Ch16, 19=Ch17 .. 34=Ch32 35=PowerOff, 36=BattLow, 37=Reset, 38=warning
			this.points[0] = (_buf[1] & 0xFF) * 1000;
			this.points[1] = (_buf[3] & 0xFF) * -1000;
			this.points[2] = (_buf[4] & 0xFF) * -1000;
			this.points[3] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000;
			this.points[4] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000;
			this.points[5] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
			this.points[6] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
			this.points[7] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
			this.points[8] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
			this.points[9] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
			this.points[10] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
			this.points[this.points.length-4] = (_buf[50] & 0x01) * 100000;
			this.points[this.points.length-3] = (_buf[50] & 0x02) * 50000;
			this.points[this.points.length-2] = (_buf[50] & 0x04) * 25000;
			if (_buf[32] > 0 && _buf[32] < 27) {
				this.points[this.points.length-1] = _buf[32] * 1000; // warning
				log.log(Level.FINE, String.format("Warning %d occured at %s", this.points[22] / 1000, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS)));
			}
			else
				this.points[this.points.length-1] = 0;

			if (_buf[5] == 0x00) { // channel 9-12
				this.points[11] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
				this.points[12] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
				this.points[13] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
				this.points[14] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
				if (this.points[15] == 0) {
					this.points[15] = 1500 * 1000;
					this.points[16] = 1500 * 1000;
					this.points[17] = 1500 * 1000;
					this.points[18] = 1500 * 1000;
				}
			}
			else { // channel 13-16
				this.points[15] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
				this.points[16] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
				this.points[17] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
				this.points[18] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
				if (this.points[11] == 0) {
					this.points[11] = 1500 * 1000;
					this.points[12] = 1500 * 1000;
					this.points[13] = 1500 * 1000;
					this.points[14] = 1500 * 1000;
				}
			}
			return true;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 *
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static int parseAddVario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		if (HoTTbinReader.varBinParser.parse()) {
			HoTTbinReader.recordSetVario.addPoints(HoTTbinReader.varBinParser.getPoints(), HoTTbinReader.varBinParser.getTimeStep_ms());
		}
		HoTTbinReader.isJustParsed = true;
		return sdLogVersion;
	}

	public static class VarBinParser extends BinParser {
		private int		tmpHeight		= 0;
		private int		tmpClimb10	= 0;

		protected VarBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.VARIO);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			points[2] = 100000;
		}

		@Override
		protected boolean parse() {
			// 0=RXSQ, 1=Altitude, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=EventVario
			this.points[0] = (_buf0[4] & 0xFF) * 1000;
			this.tmpHeight = DataParser.parse2Short(_buf1, 2);
			this.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2);
			if (isPointsValid()) {
				this.points[1] = this.tmpHeight * 1000;
				// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
				// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
				this.points[2] = (DataParser.parse2UnsignedShort(_buf1, 8) - 30000) * 10;
				this.points[3] = DataParser.parse2UnsignedShort(_buf2, 0) * 1000;
				this.points[4] = this.tmpClimb10 * 1000;
				this.points[5] = (_buf0[1] & 0xFF) * 1000;
				this.points[6] = ((_buf0[2] & 0xFF) - 20) * 1000;
				this.points[7] = (_buf1[1] & 0x3F) * 1000; // inverse event

				if ((_buf4[9] & 0xFF) > 100 && (_buf4[9] & 0xFF) < 120) { //SM MicroVario starts with FW version 1.00 -> 100
					try {
						this.points[8] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf2[4], _buf2[5], _buf2[6], _buf2[8], _buf2[9]).trim());
						this.points[9] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[1], _buf3[2], _buf3[3], _buf3[5], _buf3[6]).trim());
						this.points[10] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[8], _buf3[9], _buf4[0], _buf4[2], _buf4[3]).trim());
					}
					catch (NumberFormatException e) {
						byte[] tmpArray = new byte[21];
						System.arraycopy(_buf2, 4, tmpArray, 0, 6);
						System.arraycopy(_buf3, 0, tmpArray, 6, 10);
						System.arraycopy(_buf4, 0, tmpArray, 16, 5);
						log.log(Level.WARNING, "'" + new String(tmpArray) + "'");
					}
					this.points[11] = (_buf4[8] & 0xFF) * 1000; //AirSpeed/2
					this.points[12] = (_buf4[9] & 0xFF) * 1000; //SM MicroVario starts with FW version 1.00 -> 100
				}
				return true;
			}
			this.points[7] = (_buf1[1] & 0x3F) * 1000; // inverse event
			return false;
		}

		private  boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (tmpHeight > 10 && tmpHeight < 5000);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		if (HoTTbinReader.gpsBinParser.parse()) {
			HoTTbinReader.recordSetGPS.addPoints(HoTTbinReader.gpsBinParser.getPoints(), HoTTbinReader.gpsBinParser.getTimeStep_ms());
		}
		HoTTbinReader.isJustParsed = true;
	}

	public static class GpsBinParser extends BinParser {
		private int			tmpDate								= 0;
		private int			tmpTime								= 0;
		private int			tmpHeight							= 0;
		private int			tmpClimb1							= 0;
		private int			tmpClimb3							= 0;
		private int			tmpVelocity						= 0;
		private int			tmpLatitude						= 0;
		private int			tmpLatitudeDelta			= 0;
		private int			tmpLongitude					= 0;
		private int			tmpLongitudeDelta			= 0;
		private double	latitudeTolerance			= 1;
		private long		lastLatitudeTimeStep	= 0;
		private double	longitudeTolerance		= 1;
		private long		lastLongitudeTimeStep	= 0;

		protected GpsBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GPS);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			this.tmpHeight = DataParser.parse2Short(_buf2, 8);
			this.tmpClimb1 = DataParser.parse2UnsignedShort(_buf3, 0);
			this.tmpClimb3 = (_buf3[2] & 0xFF);
			this.tmpVelocity = DataParser.parse2UnsignedShort(_buf1, 4) * 1000;
			this.points[0] = (_buf0[4] & 0xFF) * 1000;
			if (isPointsValid()) {
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
				//15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
				this.points[6] = this.pickerParameters.isFilterEnabled && this.tmpVelocity > 2000000 ? this.points[6] : this.tmpVelocity;

				this.tmpLatitude = DataParser.parse2UnsignedShort(_buf1, 7) * 10000 + DataParser.parse2UnsignedShort(_buf1[9], _buf2[0]);
				this.tmpLatitude = _buf1[6] == 1 ? -1 * this.tmpLatitude : this.tmpLatitude;
				this.tmpLatitudeDelta = Math.abs(this.tmpLatitude - this.points[1]);
				this.tmpLatitudeDelta = this.tmpLatitudeDelta > 400000 ? this.tmpLatitudeDelta - 400000 : this.tmpLatitudeDelta;
				this.latitudeTolerance = this.points[6] / 1000.0 * (this.getTimeStep_ms() - this.lastLatitudeTimeStep) / this.pickerParameters.latitudeToleranceFactor;
				this.latitudeTolerance = this.latitudeTolerance > 0 ? this.latitudeTolerance : 25;

				if (!this.pickerParameters.isFilterEnabled || this.points[1] == 0 || this.tmpLatitudeDelta <= this.latitudeTolerance) {
					this.lastLatitudeTimeStep = this.getTimeStep_ms();
					this.points[1] = this.tmpLatitude;
				}
				else {
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO,
							StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Lat " + this.tmpLatitude + " - " + this.tmpLatitudeDelta);
				}

				this.tmpLongitude = DataParser.parse2UnsignedShort(_buf2, 2) * 10000 + DataParser.parse2UnsignedShort(_buf2, 4);
				this.tmpLongitude = _buf2[1] == 1 ? -1 * this.tmpLongitude : this.tmpLongitude;
				this.tmpLongitudeDelta = Math.abs(this.tmpLongitude - this.points[2]);
				this.tmpLongitudeDelta = this.tmpLongitudeDelta > 400000 ? this.tmpLongitudeDelta - 400000 : this.tmpLongitudeDelta;
				this.longitudeTolerance = this.points[6] / 1000.0 * (this.getTimeStep_ms() - this.lastLongitudeTimeStep) / this.pickerParameters.longitudeToleranceFactor;
				this.longitudeTolerance = this.longitudeTolerance > 0 ? this.longitudeTolerance : 45;

				if (!this.pickerParameters.isFilterEnabled || this.points[2] == 0 || this.tmpLongitudeDelta <= this.longitudeTolerance) {
					this.lastLongitudeTimeStep = this.getTimeStep_ms();
					this.points[2] = this.tmpLongitude;
				}
				else {
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Long "
							+ this.tmpLongitude + " - " + this.tmpLongitudeDelta + " - " + this.longitudeTolerance);
				}

				this.points[3] = this.tmpHeight * 1000;
				this.points[4] = this.tmpClimb1 * 1000;
				this.points[5] = this.tmpClimb3 * 1000;
				this.points[7] = DataParser.parse2UnsignedShort(_buf2, 6) * 1000;
				this.points[8] = (_buf1[3] & 0xFF) * 1000;
				this.points[9] = 0;
				this.points[10] = (_buf0[1] & 0xFF) * 1000;
				this.points[11] = ((_buf0[2] & 0xFF) - 20) * 1000;
				this.points[12] = (_buf3[3] & 0xFF) * 1000;
				
				switch (_buf3[4]) { //sat-fix
				case '-':
					this.points[13] = 0;
					break;
				case '2':
					this.points[13] = 2000;
					break;
				case '3':
					this.points[13] = 3000;
					break;
				case 'D':
					this.points[13] = 4000;
					break;
				default:
					try {
						this.points[13] = Integer.valueOf(String.format("%c", 0xFF & _buf3[4])) * 1000;
					}
					catch (NumberFormatException e1) {
						this.points[13] = 1000;
					}
					break;
				}
				this.points[14] = (_buf1[1] & 0x0F) * 1000; //14=inverse event
				this.points[15] = (_buf3[5] & 0xFF) * 1000; //15=HomeDirection
				if ((_buf4[9] & 0xFF) > 100) { //SM GPS-Logger
					//16=servoPulse 17=AirSpeed 18=n/a 19=GyroX 20=GyroY 21=GyroZ 22=ENL 23=Version	
					this.points[16] = _buf3[6] * 1000; 
					this.points[17] = DataParser.parse2UnsignedShort(_buf3, 7) * 1000;
					this.points[19] = DataParser.parse2Short(_buf3[9], _buf4[0]) * 1000;
					this.points[20] = DataParser.parse2Short(_buf4, 1) * 1000;
					this.points[21] = DataParser.parse2Short(_buf4, 3) * 1000;
					this.points[22] = (_buf4[5] & 0xFF) * 1000;
				}
				else if ((_buf4[9] & 0xFF) == 4) { //RCE Electronics Sparrow
					//16=servoPulse 17=? 18=Voltage 19=GPS time 20=GPS date 21=MSL Altitude 22=ENL 23=Version	
					this.points[16] = _buf4[4] * 1000; 
					this.points[17] = 0; 
					this.points[18] = _buf3[8] * 100; 
					if (this.points[13] > 0) { //Sat-Fix
						tmpTime = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2] * 10;
						if (tmpTime < this.points[19])
							log.log(Level.WARNING, String.format("near time: %s %s", StringHelper.getFormatedTime("HH:mm:ss.SSS", this.timeSteps_ms[TIMESTEP_INDEX] - GDE.ONE_HOUR_MS), HoTTAdapter.getFormattedTime(tmpTime)));
						this.points[19] = tmpTime;
						
						tmpDate = ((_buf4[5] - 48) * 1000000 + (_buf4[7] - 48) * 10000 + (_buf4[6] - 48) * 100) * 10;
						if (tmpDate < 0)
							log.log(Level.WARNING, String.format("near time: %s Sat-Fix %d #Sats %d %s - %c %c %c", StringHelper.getFormatedTime("HH:mm:ss.SSS", this.timeSteps_ms[TIMESTEP_INDEX] - GDE.ONE_HOUR_MS), this.points[13]/1000,  this.points[12]/1000, HoTTAdapter.getFormattedTime(this.points[19]), _buf4[5]&0xff, _buf4[7]&0xff, _buf4[6]&0xff));
						this.points[20] = tmpDate;

						this.points[21] = (DataParser.parse2Short(_buf3, 6) - 500) * 1000; //TODO remove offset 500 after correction
					}
					this.points[22] = (_buf4[3] & 0xFF) * 1000;
				}
				else if ((_buf4[9] & 0xFF) == 0 || (_buf4[9] & 0xFF) == 1) { //Graupner GPS need workaround to distinguish between different Graupner GPS version #0
					int version = this.points[23] == 1000 || (_buf3[6] != 0 && _buf3[7] != 0 && _buf3[8] != 0) ? 1 : 0;
						
					if (version == 0) { //#0=GPS 33600
						//16=Roll 17=Pitch 18=Yaw 19=GPS time 20=? 21=MSL Altitude 22=Vibration
						this.points[16] = _buf3[6] * 1000; 
						this.points[17] = _buf3[7] * 1000; 
						this.points[18] = _buf3[8] * 1000; 
						this.points[19] = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2]*10;
						this.points[20] = 0;
						this.points[21] = DataParser.parse2Short(_buf4, 3) * 1000;
						this.points[22] = (_buf4[5] & 0xFF) * 1000;
					}
					else { //#1= 33602/S8437
						//16=velN NED north velocity mm/s 17=n/a 18=sAcc Speed accuracy estimate cm/s
						this.points[16] = DataParser.parse2Short(_buf3, 6) * 1000;
						this.points[17] = 0;
						this.points[18] = _buf3[8] * 1000; 
						//19=GPS time 20=? 21=velE NED east velocity mm/s
						this.points[19] = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2]*10;
						this.points[20] = 0;
						this.points[21] = DataParser.parse2Short(_buf4, 3) * 1000;
						//22=hAcc Horizontal accuracy estimate HDOP 			
						this.points[22] = (_buf4[5] & 0xFF) * 1000;
					}
				}
				else { //unknown GPS
					//16=Roll 17=Pitch 18=Yaw 19=GPS time1 20=GPS time2 21=AltitudeMSL 22=Vibration
					this.points[16] = _buf3[6] * 1000; 
					this.points[17] = _buf3[7] * 1000; 
					this.points[18] = _buf3[8] * 1000; 
					this.points[19] = DataParser.parse2Short(_buf3[9], _buf4[0]) * 1000;
					this.points[20] = DataParser.parse2Short(_buf4, 1) * 1000;
					this.points[21] = DataParser.parse2Short(_buf4, 3) * 1000;
					this.points[22] = (_buf4[5] & 0xFF) * 1000;
				}
				//three char 23=Version		
				this.points[23] = (_buf4[9] & 0xFF) * 1000;
				return true;
			}
			this.points[14] = (_buf1[1] & 0x0F) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb1 > 10000 && this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 4500);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		if (HoTTbinReader.gamBinParser.parse()) {
			HoTTbinReader.recordSetGAM.addPoints(HoTTbinReader.gamBinParser.getPoints(), HoTTbinReader.gamBinParser.getTimeStep_ms());
		}
		HoTTbinReader.isJustParsed = true;
	}

	public static class GamBinParser extends BinParser {
		private int		tmpHeight		= 0;
		private int		tmpClimb3		= 0;
		private int		tmpVoltage1	= 0;
		private int		tmpVoltage2	= 0;
		private int		tmpCapacity	= 0;

		private int		parseCount	= 0;

		protected GamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			this.tmpHeight = DataParser.parse2Short(_buf3, 0);
			this.tmpClimb3 = (_buf3[4] & 0xFF);
			this.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
			this.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
			// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
			// 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
			// 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel,
			// 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
			// 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
			this.points[0] = (_buf0[4] & 0xFF) * 1000;
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[1] = DataParser.parse2Short(_buf3, 7) * 1000;
				this.points[2] = DataParser.parse2Short(_buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[3] / 1000 + this.points[1] / 1000 * this.points[2] / 1000 / 2500 + 2))) {
					this.points[3] = this.tmpCapacity * 1000;
				}
				else {
					log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[3] / 1000)
							+ " + " + (this.points[1] / 1000 * this.points[2] / 1000 / 2500 + 2));
				}
				this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue();
				this.points[5] = 0;
				for (int j = 0; j < 6; j++) {
					this.points[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 6] > 0) {
						maxVotage = this.points[j + 6] > maxVotage ? this.points[j + 6] : maxVotage;
						minVotage = this.points[j + 6] < minVotage ? this.points[j + 6] : minVotage;
					}
				}
				this.points[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				this.points[12] = DataParser.parse2UnsignedShort(_buf2, 8) * 1000;
				this.points[13] = this.tmpHeight * 1000;
				this.points[14] = DataParser.parse2UnsignedShort(_buf3, 2) * 1000;
				this.points[15] = this.tmpClimb3 * 1000;
				this.points[16] = DataParser.parse2Short(_buf2, 6) * 1000;
				this.points[17] = this.tmpVoltage1 * 1000;
				this.points[18] = this.tmpVoltage2 * 1000;
				this.points[19] = ((_buf2[3] & 0xFF) - 20) * 1000;
				this.points[20] = ((_buf2[4] & 0xFF) - 20) * 1000;
				// 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
				this.points[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000; // Speed [km/h
				this.points[22] = (_buf4[3] & 0xFF) * 1000; // lowest cell voltage 124 = 2.48 V
				this.points[23] = (_buf4[4] & 0xFF) * 1000; // cell number lowest cell voltage
				this.points[24] = (_buf4[8] & 0xFF) * 1000; // Pressure
				if ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8) != 0)
					this.points[25] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; //inverse event
				++this.parseCount;
				return true;
			}
			if ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8) != 0)
				this.points[25] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; //inverse event
			++this.parseCount;
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddEAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		if (HoTTbinReader.eamBinParser.parse()) {
			HoTTbinReader.recordSetEAM.addPoints(HoTTbinReader.eamBinParser.getPoints(), HoTTbinReader.eamBinParser.getTimeStep_ms());
		}
		HoTTbinReader.isJustParsed = true;
	}

	public static class EamBinParser extends BinParser {
		private int		tmpHeight		= 0;
		private int		tmpClimb3		= 0;
		private int		tmpVoltage1	= 0;
		private int		tmpVoltage2	= 0;
		private int		tmpCapacity	= 0;

		private int		parseCount	= 0;

		protected EamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.EAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			this.tmpHeight = DataParser.parse2Short(_buf3, 3);
			this.tmpClimb3 = (_buf4[3] & 0xFF);
			this.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
			this.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
			this.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
			// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
			// 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude,
			// 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1,
			// 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
			this.points[0] = (_buf0[4] & 0xFF) * 1000;
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[1] = DataParser.parse2Short(_buf3, 7) * 1000;
				this.points[2] = DataParser.parse2Short(_buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| Math.abs(this.tmpCapacity) <= (this.points[3] / 1000 + this.points[1] / 1000 * this.points[2] / 1000 / 2500 + 2)) {
					this.points[3] = this.tmpCapacity * 1000;
				}
				else {
					log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[3] / 1000)
							+ " + " + (this.points[1] / 1000 * this.points[2] / 1000 / 2500 + 2));
				}
				this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue(); // power U*I [W];
				this.points[5] = 0; // 5=Balance
				for (int j = 0; j < 7; j++) {
					this.points[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 6] > 0) {
						maxVotage = this.points[j + 6] > maxVotage ? this.points[j + 6] : maxVotage;
						minVotage = this.points[j + 6] < minVotage ? this.points[j + 6] : minVotage;
					}
				}
				for (int j = 0; j < 7; j++) {
					this.points[j + 13] = (_buf2[j] & 0xFF) * 1000;
					if (this.points[j + 13] > 0) {
						maxVotage = this.points[j + 13] > maxVotage ? this.points[j + 13] : maxVotage;
						minVotage = this.points[j + 13] < minVotage ? this.points[j + 13] : minVotage;
					}
				}
				// calculate balance on the fly
				this.points[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				this.points[20] = this.tmpHeight * 1000;
				this.points[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;
				this.points[22] = this.tmpClimb3 * 1000;
				this.points[23] = this.tmpVoltage1 * 1000;
				this.points[24] = this.tmpVoltage2 * 1000;
				this.points[25] = ((_buf3[1] & 0xFF) - 20) * 1000;
				this.points[26] = ((_buf3[2] & 0xFF) - 20) * 1000;
				this.points[27] = DataParser.parse2UnsignedShort(_buf4, 4) * 1000; // revolution
				this.points[28] = ((_buf4[6] & 0xFF) * 60 + (_buf4[7] & 0xFF)) * 1000; // motor time
				this.points[29] = DataParser.parse2Short(_buf4, 8) * 1000; // speed
				this.points[30] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; // inverse event
				++this.parseCount;
				return true;
			}
			this.points[30] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; // inverse event
			++this.parseCount;
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		if (HoTTbinReader.escBinParser.parse()) {
			HoTTbinReader.recordSetESC.addPoints(HoTTbinReader.escBinParser.getPoints(), HoTTbinReader.escBinParser.getTimeStep_ms());
		}
		HoTTbinReader.isJustParsed = true;
	}

	public static class EscBinParser extends BinParser {
		private int		tmpTemperatureFet	= 0;
		private int		tmpVoltage				= 0;
		private int		tmpCurrent				= 0;
		private int		tmpRevolution			= 0;
		private int		tmpCapacity				= 0;

		private int		parseCount				= 0;

		protected EscBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.ESC);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
			// 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
			// 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 
			// 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc_ESC_15 29=VersionESC
			this.tmpVoltage = DataParser.parse2Short(_buf1, 3);
			this.tmpCurrent = DataParser.parse2Short(_buf2, 1);
			this.points[0] = (_buf0[4] & 0xFF) * 1000;
			this.tmpCapacity = DataParser.parse2Short(_buf1, 7);
			this.tmpRevolution = DataParser.parse2UnsignedShort(_buf2, 5);
			this.tmpTemperatureFet = (_buf1[9] & 0xFF) - 20;
			if (isPointsValid()) {
				this.points[1] = this.tmpVoltage * 1000;
				this.points[2] = this.tmpCurrent * 1000;
				this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue();
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[3] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
					this.points[3] = this.tmpCapacity * 1000;
				}
				else {
					if (this.tmpCapacity != 0) log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - "
							+ (this.points[3] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
				}
				this.points[5] = this.tmpRevolution * 1000;
				this.points[6] = this.tmpTemperatureFet * 1000;
				this.points[7] = ((_buf2[9] & 0xFF) - 20) * 1000;    //Temperature Motor

				// 8=Voltage_min, 9=Current_max, 10=Revolution_max,
				// 11=Temperature1_max, 12=Temperature2_max
				this.points[8] = DataParser.parse2Short(_buf1, 5) * 1000;
				this.points[9] = DataParser.parse2Short(_buf2, 3) * 1000;
				this.points[10] = DataParser.parse2UnsignedShort(_buf2, 7) * 1000;
				this.points[11] = ((_buf2[0] & 0xFF) - 20) * 1000;
				this.points[12] = ((_buf3[0] & 0xFF) - 20) * 1000; 	//Temperature Motor_max
				
				if ((_buf1[1] & 0xFF) != 0)
					this.points[13] = (_buf1[1] & 0xFF) * 1000; //inverse event
				
				if ((_buf4[9] & 0xFF) == 3) { //Extended YGE protocol 				
					//14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 
					//23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc_ESC_15 29=VersionESC
					this.points[14] = DataParser.parse2Short(_buf3, 1) * 1000; 	//Speed
					this.points[15] = DataParser.parse2Short(_buf3, 3) * 1000; 	//Speed max
					this.points[16] = (_buf3[5] & 0xFF) * 1000; 								//PWM
					this.points[17] = (_buf3[6] & 0xFF) * 1000; 								//Throttle
					this.points[18] = (_buf3[7] & 0xFF) * 1000; 								//BEC Voltage
					this.points[19] = (_buf3[8] & 0xFF) * 1000; 								//BEC Voltage min
					this.points[20] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000; 	//BEC Current
					this.points[21] = ((_buf4[1] & 0xFF) - 20) * 1000; 					//BEC Temperature
					this.points[22] = ((_buf4[2] & 0xFF) - 20) * 1000; 					//Capacity Temperature
					this.points[23] = (_buf4[3] & 0xFF) * 1000; 								//Timing
					this.points[24] = ((_buf4[4] & 0xFF) - 20) * 1000; 					//Aux Temperature
					this.points[25] = DataParser.parse2Short(_buf4, 5) * 1000; 	//Gear
					this.points[26] = (_buf4[7] & 0xFF) * 1000; 								//YGEGenExt
					this.points[27] = (_buf4[8] & 0xFF) * 1000; 								//MotStatEscNr
					this.points[28] = 0; 																				//spare
					this.points[29] = (_buf4[9] & 0xFF) * 1000; 								//Version ESC
				}
				else if ((_buf4[9] & 0xFF) >= 128) { //Extended CS-Electronics
					//14=AirSpeed 15=AirSpeed_max 16=PWM 17=Throttle 18=VoltagePump 19=VoltagePump_min 20=Flow 21=Fuel 22=Power 
					//23=Thrust 24=TemperaturePump 25=EngineStat 26=spare 27=spare 28=spare 29=version
					this.points[14] = DataParser.parse2Short(_buf3, 1) * 1000; 	//AirSpeed
					this.points[15] = DataParser.parse2Short(_buf3, 3) * 1000; 	//AirSpeed max
					this.points[16] = (_buf3[5] & 0xFF) * 1000; 								//PWM
					this.points[17] = (_buf3[6] & 0xFF) * 1000; 								//Throttle
					this.points[18] = (_buf3[7] & 0xFF) * 1000; 								//Pump Voltage
					this.points[19] = (_buf3[8] & 0xFF) * 1000; 								//Pump Voltage min
					this.points[20] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000;	//Flow
					this.points[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;						//Fuel ml
					this.points[22] = DataParser.parse2UnsignedShort(_buf4, 3) * 1000; 						//Power Wh
					this.points[23] = DataParser.parse2UnsignedShort(_buf4, 5) * 1000; 						//Thrust
					this.points[24] = ((_buf4[7] & 0xFF) - 20) * 1000; 					//Pump Temperature
					this.points[25] = (_buf4[8] & 0xFF) * 1000; 								//Engine run
					this.points[26] = 0; 																				//spare
					this.points[27] = 0; 																				//spare
					this.points[28] = 0; 																				//spare
					this.points[29] = (_buf4[9] & 0xFF) * 1000; 								//Version ESC			

				}
				++this.parseCount;
				return true;
			}
			if ((_buf1[1] & 0xFF) != 0)
				this.points[13] = (_buf1[1] & 0xFF) * 1000; //inverse event
			++this.parseCount;
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled
					|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
					&& this.tmpRevolution < 20000 && !(this.points[6] != 0 && this.points[6] / 1000 - this.tmpTemperatureFet > 20);
		}
	}

	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(buffer[i]).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	/**
	 * @return get a rectified start timestamp if the files's last modified timestamp does not correspond with the filename
	 */
	protected static long getStartTimeStamp(String fileName, long fileLastModified, long numberDatablocks) {
		final long startTimeStamp;
		log.log(Level.FINE, "name=", fileName); //$NON-NLS-1$
		Pattern hoTTNamePattern = Pattern.compile("\\d{4}\\_\\d{4}-\\d{1,2}-\\d{1,2}"); //$NON-NLS-1$
		Matcher hoTTMatcher = hoTTNamePattern.matcher(fileName);
		if (hoTTMatcher.find()) {
			String logName = hoTTMatcher.group();

			String[] strValueLogName = logName.split(GDE.STRING_UNDER_BAR);
			int logCounter = Integer.parseInt(strValueLogName[0]);

			String[] strValueDate = strValueLogName[1].split(GDE.STRING_DASH);
			int year = Integer.parseInt(strValueDate[0]);
			int month = Integer.parseInt(strValueDate[1]);
			int day = Integer.parseInt(strValueDate[2]);

			// check if the zoned date is equal
			GregorianCalendar lastModifiedDate = new GregorianCalendar();
			lastModifiedDate.setTimeInMillis(fileLastModified - numberDatablocks * 10);
			if (year == lastModifiedDate.get(Calendar.YEAR) && month == lastModifiedDate.get(Calendar.MONTH) + 1 && day == lastModifiedDate.get(Calendar.DAY_OF_MONTH)) {
				startTimeStamp = lastModifiedDate.getTimeInMillis();
				log.log(Level.FINE, "creation=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", lastModifiedDate.getTimeInMillis()) + " lastModified=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", fileLastModified)); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			else {
				// spread the logCounter into steps of 10 minutes
				GregorianCalendar fileNameDate = new GregorianCalendar(year, month - 1, day, 0, 0, 0);
				fileNameDate.add(Calendar.MINUTE, 10 * (logCounter % (24 * 6)));
				startTimeStamp = fileNameDate.getTimeInMillis();
				log.log(Level.FINE, "fileNameDate=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileNameDate.getTimeInMillis())); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			startTimeStamp = fileLastModified;
		}
		return startTimeStamp;
	}

	/**
	 * check if file time stamp match log internal recorded start time and replace if required
	 * @param formattedLogStartTime
	 * @param fileStartTimeStamp_ms
	 * @return
	 */
	protected static long getStartTimeStamp(String formattedLogStartTime, long fileStartTimeStamp_ms) {
		long startTimeStamp_ms = fileStartTimeStamp_ms;
		String formattedFileStartTimeStamp = StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", fileStartTimeStamp_ms);

		if (!formattedFileStartTimeStamp.contains(formattedLogStartTime)) { // LOG START TIME : 15:08:28

			int year = Integer.parseInt(formattedFileStartTimeStamp.substring(0, 4));
			int month = Integer.parseInt(formattedFileStartTimeStamp.substring(5, 7));
			int day = Integer.parseInt(formattedFileStartTimeStamp.substring(8, 10));

			int hour = Integer.parseInt(formattedLogStartTime.substring(0, 2));
			int minute = Integer.parseInt(formattedLogStartTime.substring(3, 5));
			int second = Integer.parseInt(formattedLogStartTime.substring(6, 8));

			GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
			fileStartTimeStamp_ms = calendar.getTimeInMillis(); 
		}
		return startTimeStamp_ms;
	}

	/**
	 * check if file time stamp match log internal recorded start time and replace if required
	 * @param fileStartTimeStamp_ms file based time stamp
	 * @param timeStampGPS_ms integer formatted time stamp received from satellite
	 * @param numberDataBlocks_base10_ms number of passed data blocks until first valid time stamp received from GPS
	 * @return time stamp corrected 
	 */
	protected static long getStartTimeStamp(final long fileStartTimeStamp_ms, final int timeStampGPS_ms, final int numberDataBlocks_base10_ms) {
		try {
			String formattedFileStartTimeStamp = StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", fileStartTimeStamp_ms);

			int year = Integer.parseInt(formattedFileStartTimeStamp.substring(0, 4));
			int month = Integer.parseInt(formattedFileStartTimeStamp.substring(5, 7));
			int day = Integer.parseInt(formattedFileStartTimeStamp.substring(8, 10));
			int hour = Integer.parseInt(formattedFileStartTimeStamp.substring(11, 13));
			
			int tmpHH = timeStampGPS_ms/10000000;
			int minute = timeStampGPS_ms/100000 - tmpHH*100;
			int second = timeStampGPS_ms/1000 - minute*100 - tmpHH*10000;
			int milliseconds = timeStampGPS_ms - second*1000 - minute*100000 - tmpHH*10000000; 
			milliseconds -= numberDataBlocks_base10_ms == 0 ? 100 : numberDataBlocks_base10_ms * 10; //use 100 ms default correction factor for HoTTAdapter

			GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
			return calendar.getTimeInMillis() + milliseconds;
		}
		catch (Exception e) {
			return fileStartTimeStamp_ms;
		} 
 	}

	/**
	 * Use for HoTTbinReader and HoTTbinHistoReader only (not for HoTTbinReaderD / X and derivates).
	 * @author Thomas Eickert (USER)
	 */
	public abstract static class BinParser {

		public static final int						TIMESTEP_INDEX	= 0;

		@SuppressWarnings("hiding")
		protected final PickerParameters	pickerParameters;
		protected final int[]							points;
		protected final long[]						timeSteps_ms;
		protected final Sensor						sensor;

		protected byte[]									_buf0, _buf1, _buf2, _buf3, _buf4;

		/**
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param pickerParameters is the parameter object for the current thread
		 * @param points parsed from the input buffers
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 * @param sensor associated with this parser (pls note that the receiver / channel is also a sensor)
		 */
		protected BinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers, Sensor sensor) {
			this.pickerParameters = pickerParameters;
			this.points = points;
			this.timeSteps_ms = timeSteps_ms;
			this.sensor = sensor;
			switch (buffers.length) {
			case 5:
				this._buf4 = buffers[4];
			case 4:
				this._buf3 = buffers[3];
			case 3:
				this._buf2 = buffers[2];
			case 2:
				this._buf1 = buffers[1];
			case 1:
				this._buf0 = buffers[0];
				break;

			default:
				throw new IllegalArgumentException("buffers length mismatch " + buffers.length);
			}
		}

		/**
		 * Parse the buffered data.
		 * Use an individual subset of buffer 0 to 4.
		 * @return true if the core points are valid
		 */
		protected abstract boolean parse();

		protected long getTimeStep_ms() {
			return this.timeSteps_ms[TIMESTEP_INDEX];
		}

		/**
		 * @return the sensor associated with this parser (pls note that the receiver / channel is also a sensor)
		 */
		public Sensor getSensor() {
			return this.sensor;
		}

		public int[] getPoints() {
			return this.points;
		}

		public void migratePoints(int[] targetPoints) {
			throw new UnsupportedOperationException("required for HoTTbinReader2 only");
		}

		@Override
		public String toString() {
			final int maxLen = 11;
			return "BinParser [sensor=" + this.sensor + ", timeStep_ms=" + this.getTimeStep_ms() + ", points=" + (this.points != null
					? Arrays.toString(Arrays.copyOf(this.points, Math.min(this.points.length, maxLen)))
					: null) + "]";
		}
	}
	
	/**
	 * @param buffer
	 * @return true if other data than 0 contained
	 */
	public static boolean isReasonableData(byte[] buffer) {
		int sum = 0;
		for (byte b : buffer) {
			sum += (b & 0xFF);
		}
		return sum > 10;
	}
}
