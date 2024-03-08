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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.StatisticsType;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTlogReader.LogParser;
import gde.device.graupner.hott.MessageIds;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;
import gde.histo.device.IHistoDevice;
import gde.histo.device.UniversalSampler;
import gde.histo.utils.PathUtils;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CalculationThread;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.LinearRegression;
import gde.utils.ObjectKeyCompliance;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Graupner HoTT device base class
 * @author Winfried Brügmann
 */
public class HoTTAdapter extends DeviceConfiguration implements IDevice, IHistoDevice {
	final static Logger		log																= Logger.getLogger(HoTTAdapter.class.getName());

	final static String		LOG_COUNT													= "LogCount";						//number of log entries
	final static String		FILE_PATH													= "FilePath";						//file path
	final static String		SD_FORMAT													= "SD_FORMAT";					//special SD log format car radio
	final static String		DETECTED_SENSOR										= "DETECTED SENSOR";		//detected sensor set
	final static String		ASCII_LOG_SIZE										= "ASCII_LOG_SIZE";			//size of log entry in ASCII format
	final static String		RAW_LOG_SIZE											= "RAW_LOG_SIZE";				//size of log entry in RAW format
	final static String		DATA_BLOCK_SIZE										= "DATA_BLOCK_SIZE";		//resulting size of log entry 		
	final static String		LOG_DATA_OFFSET										= "LOG DATA OFFSET"; 		//log header length

	// HoTT sensor bytes 19200 Baud protocol
	static boolean				IS_SLAVE_MODE											= false;
	final static byte			SENSOR_TYPE_RECEIVER_19200				= (byte) (0x80 & 0xFF);
	final static byte			SENSOR_TYPE_VARIO_19200						= (byte) (0x89 & 0xFF);
	final static byte			SENSOR_TYPE_GPS_19200							= (byte) (0x8A & 0xFF);
	final static byte			SENSOR_TYPE_GENERAL_19200					= (byte) (0x8D & 0xFF);
	final static byte			SENSOR_TYPE_ELECTRIC_19200				= (byte) (0x8E & 0xFF);
	final static byte			SENSOR_TYPE_SPEED_CONTROL_19200		= (byte) (0x8C & 0xFF);
	final static byte			ANSWER_SENSOR_VARIO_19200					= (byte) (0x90 & 0xFF);
	final static byte			ANSWER_SENSOR_GPS_19200						= (byte) (0xA0 & 0xFF);
	final static byte			ANSWER_SENSOR_GENERAL_19200				= (byte) (0xD0 & 0xFF);
	final static byte			ANSWER_SENSOR_ELECTRIC_19200			= (byte) (0xE0 & 0xFF);
	final static byte			ANSWER_SENSOR_MOTOR_DRIVER_19200	= (byte) (0xC0 & 0xFF);
	final static byte			ANSWER_SENSOR_ESC2_19200					= (byte) (0x50 & 0xFF);
	final static byte			ANSWER_SENSOR_ESC3_19200					= (byte) (0x60 & 0xFF);
	final static byte			ANSWER_SENSOR_ESC4_19200					= (byte) (0x70 & 0xFF);

	// HoTT sensor bytes 115200 Baud protocol (actual no slave mode)
	// there is no real slave mode for this protocol
	final static byte			SENSOR_TYPE_RECEIVER_115200				= 0x34;
	final static byte			SENSOR_TYPE_VARIO_115200					= 0x37;
	final static byte			SENSOR_TYPE_GPS_115200						= 0x38;
	final static byte			SENSOR_TYPE_GENERAL_115200				= 0x35;
	final static byte			SENSOR_TYPE_ELECTRIC_115200				= 0x36;
	final static byte			SENSOR_TYPE_SPEED_CONTROL_115200	= 0x39;
	final static byte			SENSOR_TYPE_SERVO_POSITION_115200	= 0x40;
	final static byte			SENSOR_TYPE_PURPIL_POSITION_115200= 0x41;
	final static byte			SENSOR_TYPE_CONTROL_1_115200			= 0x42;
	final static byte			SENSOR_TYPE_CONTROL_2_115200			= 0x43;

	final static boolean	isSwitchS[]												= { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false };
	final static boolean	isSwitchG[]												= { false, false, false, false, false, false, false, false };
	final static boolean	isSwitchL[]												= { false, false, false, false, false, false, false, false };

	final static int			QUERY_GAP_MS											= 30;

	public enum Sensor {
		RECEIVER(1, "Receiver", "RECEIVER") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.RcvBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.RcvLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.RcvBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.RcvBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.RcvBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.RcvBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.RcvLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		VARIO(2, "Vario", "VARIO") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.VarBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.VarLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.VarBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.VarBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.VarBinParserD(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.VarBinParserD(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.VarLogParserD(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		GPS(3, "GPS", "GPS") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.GpsBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.GpsLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GpsBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GpsBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.GpsBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.GpsBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.GpsLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		GAM(4, "GAM", "GENERAL") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.GamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.GamLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.GamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.GamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.GamLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		EAM(5, "EAM", "ELECTRIC") {//$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.EamLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.EamLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		ESC(7, "ESC", "AIR_ESC") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.EscLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.EscLogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		ESC2(8, "ESC2", "AIR_ESC2") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.Esc2LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.Esc2LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		ESC3(9, "ESC3", "AIR_ESC3") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.Esc3LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.Esc3LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		ESC4(10, "ESC4", "AIR_ESC4") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.Esc4LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.Esc4LogParser(pickerParameters, points, timeSteps_ms, buffer);
			}
		},
		CHANNEL(6, "Channel", "N/A") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.ChnBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReader.ChnLogParser(pickerParameters, points, timeSteps_ms, buffer, numberUsedChannels);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.ChnBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.ChnBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.ChnBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReaderD.ChnBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
			
			@Override
			public LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
				return new HoTTlogReaderD.ChnLogParser(pickerParameters, points, timeSteps_ms, buffer, numberUsedChannels);
			}
		};

		private final String				value;
		private final String				detectedName;
		private final int						channelNumber;
		public static final Sensor	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private Sensor(int channelNumber, String value, String detectedName) {
			this.value = value;
			this.detectedName = detectedName;
			this.channelNumber = channelNumber;
		}

		public String value() {
			return this.value;
		}

		/**
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param points is the output object
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param points is the output object
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract LogParser createLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels);

		/**
		 * Parse for HoTTAdapter2.
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse in situ for HoTTAdapter2 without the need to migrate the points.
		 * Takes the parsing input objects as well as the parsing output object.
		 * @param points is the output object which might be on single commonly used object for all parser subclasses
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse for HoTTAdapterD.
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse in situ for HoTTAdapterD without the need to migrate the points.
		 * Takes the parsing input objects as well as the parsing output object.
		 * @param points is the output object which might be on single commonly used object for all parser subclasses
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse in situ for HoTTAdapterD without the need to migrate the points.
		 * Takes the parsing input objects as well as the parsing output object.
		 * @param points is the output object which might be on single commonly used object for all parser subclasses
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract LogParser createLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffers, int numberUsedChannels);

		public static Sensor fromOrdinal(int ordinal) {
			return Sensor.VALUES[ordinal];
		}

		@Nullable
		public static Sensor fromSensorByte(byte sensorByte) {
			Sensor sensor = null;
			switch (sensorByte) {
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
			case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
				sensor = Sensor.VARIO;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
			case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
				sensor = Sensor.GPS;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
			case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
				sensor = Sensor.GAM;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
			case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
				sensor = Sensor.EAM;
				break;
			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
			case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
				sensor = Sensor.ESC;
				break;
			default:
				//
				break;
			}
			return sensor;
		}

		@Nullable
		public static Sensor fromChannelNumber(int channelNumber) {
			for (Sensor sensor : Sensor.VALUES) {
				if (channelNumber == sensor.channelNumber) {
					return sensor;
				}
			}
			return null;
		}

		/**
		 * @param value is the name in the sensor signature
		 * @return the sensor found (ignoring casing)
		 */
		@Nullable
		public static Sensor fromValue(String value) {
			for (Sensor sensor : Sensor.VALUES) {
				if (value.equalsIgnoreCase(sensor.value)) {
					return sensor;
				}
			}
			return null;
		}

		/**
		 * @param detectedName is the name in the same pattern as the DETECTED SENSOR entry in a *.log file (e.g. RECEIVER,VARIO,GPS,GENERAL,)
		 * @return the sensor found (ignoring casing)
		 */
		@Nullable
		public static Sensor fromDetectedName(String detectedName) {
			for (Sensor sensor : Sensor.VALUES) {
				if (detectedName.equalsIgnoreCase(sensor.detectedName)) {
					return sensor;
				}
			}
			return null;
		}

		public static EnumSet<Sensor> getSetFromSignature(String sensorSignature) {
			EnumSet<Sensor> sensors = sensorSignature.isEmpty() ? EnumSet.noneOf(Sensor.class)
					: Arrays.stream(sensorSignature.split(GDE.STRING_COMMA)).map(Sensor::fromValue).filter(Objects::nonNull) //
							.collect(Collectors.toCollection(() -> EnumSet.noneOf(Sensor.class)));
			sensors.add(RECEIVER); // always present
			sensors.remove(CHANNEL); // not a real sensor
			return sensors;
		}

		public static EnumSet<Sensor> getSetFromDetected(String detectedSensors) {
			EnumSet<Sensor> sensors = detectedSensors.isEmpty() ? EnumSet.noneOf(Sensor.class)
					: Arrays.stream(detectedSensors.split(GDE.STRING_COMMA)).map(Sensor::fromDetectedName).filter(Objects::nonNull) //
							.collect(Collectors.toCollection(() -> EnumSet.noneOf(Sensor.class)));
			sensors.add(RECEIVER); // always present
			sensors.remove(CHANNEL); // not a real sensor
			return sensors;
		}

		public static StringBuilder getSetAsSignature(EnumSet<Sensor> sensors) {
			EnumSet<Sensor> tmpSensors = sensors.clone();
			tmpSensors.add(RECEIVER);
			return new StringBuilder(tmpSensors.stream().map(s -> s.value).collect(Collectors.joining(GDE.STRING_COMMA)));
		}

		public static String getSetAsDetected(EnumSet<Sensor> sensors) {
			EnumSet<Sensor> tmpSensors = sensors.clone();
			tmpSensors.add(RECEIVER);
			return tmpSensors.stream().map(s -> s.detectedName).collect(Collectors.joining(GDE.STRING_COMMA));
		}

		/**
		 * @param sensors is a subset of the sensor entries
		 * @return a boolean array with a length of all sensor entries. It holds true values for the sensors in the {@code sensors} set.
		 */
		public static boolean[] getActiveSensors(EnumSet<Sensor> sensors) {
			boolean[] activeSensors = new boolean[VALUES.length];
			for (int i = 0; i < VALUES.length; i++) {
				Sensor sensor = VALUES[i];
				activeSensors[i] = sensors.contains(sensor);
			}
			return activeSensors;
		}

		/**
		 * @param sensors is a subset of the sensor entries
		 * @return a set with bits 0 to 31 representing the sensor type ordinal numbers (true if the sensor type is active)
		 */
		public static BitSet getSensors(EnumSet<Sensor> sensors) {
			BitSet sensorBitSet = new BitSet();
			for (Sensor sensor : sensors) {
				sensorBitSet.set(sensor.ordinal());
			}
			return sensorBitSet;
		}

		/**
		 * @return channel numbers of the sensors including the receiver and 'channels'
		 */
		public static Set<Integer> getChannelNumbers(EnumSet<Sensor> sensors) {
			Set<Integer> channelNumbers = new HashSet<Integer>();
			channelNumbers.add(RECEIVER.channelNumber); // always present
			channelNumbers.add(CHANNEL.channelNumber); // always present
			for (Sensor sensor : sensors) {
				channelNumbers.add(sensor.channelNumber);
			}
			return channelNumbers;
		}

		public int getChannelNumber() {
			return this.channelNumber;
		}

	};

	// protocol definitions
	public enum Protocol {
		TYPE_19200_V3("19200 V3"), TYPE_19200_V4("19200 V4"), TYPE_115200("115200"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		private final String value;

		private Protocol(String v) {
			this.value = v;
		}

		public String value() {
			return this.value;
		}

		public static Protocol fromValue(String v) {
			for (Protocol c : Protocol.values()) {
				if (c.value.equals(v)) {
					return c;
				}
			}
			throw new IllegalArgumentException(v);
		}

		public static String[] valuesAsStingArray() {
			StringBuilder sb = new StringBuilder();
			for (Protocol protocol : Protocol.values()) {
				sb.append(protocol.value).append(GDE.STRING_SEMICOLON);
			}
			return sb.toString().split(GDE.STRING_SEMICOLON);
		}
	}

	/**
	 * Parameter object replacing some HoTTAdapter* fields used by other classes.
	 * Supports threadsafe access to these parameters.
	 * todo check if this class is required after making the readers threadsafe
	 * @author Thomas Eickert (USER)
	 */
	public static final class PickerParameters {
		final Analyzer					analyzer;

		final PackageLossDeque	reverseChannelPackageLossCounter;

		boolean									isChannelsChannelEnabled			= false;
		boolean									isFilterEnabled								= true;
		boolean									isFilterTextModus							= true;
		boolean									isChannelPercentEnabled				= true;
		int											altitudeClimbSensorSelection	= 0;
		
		double									latitudeToleranceFactor				= 50.0;
		double									longitudeToleranceFactor			= 15.0;

		public PickerParameters(Analyzer analyzer) {
			this.analyzer = analyzer;
			this.reverseChannelPackageLossCounter = new PackageLossDeque(100);
		}

		/**
		 * Shallow copy constructor.
		 * New package loss counter!
		 */
		PickerParameters(PickerParameters that) {
			this.analyzer = that.analyzer;
			this.reverseChannelPackageLossCounter = new PackageLossDeque(100);

			this.isChannelsChannelEnabled = that.isChannelsChannelEnabled;
			this.isFilterEnabled = that.isFilterEnabled;
			this.isFilterTextModus = that.isFilterTextModus;
			this.isChannelPercentEnabled = that.isChannelPercentEnabled;
			this.altitudeClimbSensorSelection = that.altitudeClimbSensorSelection;
			
//			this.isTolerateSignChangeLatitude = that.isTolerateSignChangeLatitude;
//			this.isTolerateSignChangeLongitude = that.isTolerateSignChangeLongitude;
			this.latitudeToleranceFactor = that.latitudeToleranceFactor;
			this.longitudeToleranceFactor = that.longitudeToleranceFactor;
		}

		/**
		 * Collect the settings relevant for the values inserted in the histo vault.
		 * @return the settings which determine the measurement values returned by the reader
		 */
		public String getReaderSettingsCsv() {
			final String d = GDE.STRING_CSV_SEPARATOR;
			return isFilterEnabled + d + altitudeClimbSensorSelection;
			//return isFilterEnabled + d + isTolerateSignChangeLatitude + d + isTolerateSignChangeLongitude + d + latitudeToleranceFactor + d + longitudeToleranceFactor;
		}

		@Override
		public String toString() {
			return "PickerParameters [analyzer.channels=" + this.analyzer.getChannels() + ", isChannelsChannelEnabled=" + this.isChannelsChannelEnabled + ", isFilterEnabled=" + this.isFilterEnabled + ", isFilterTextModus=" + this.isFilterTextModus + ", altitudeClimbSensorSelection=" + this.altitudeClimbSensorSelection + "]";
		}


	}

	final DataExplorer								application;
	final Channels										channels;
	final Settings										settings;
	final HoTTAdapterDialog						dialog;
	final HoTTAdapterSerialPort				serialPort;

	protected final PickerParameters	pickerParameters	= new PickerParameters(Analyzer.getInstance());

	protected List<String>						importExtentions	= null;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public HoTTAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	private void setPickerParameters() {
		this.pickerParameters.isChannelsChannelEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue()) : false;
		this.pickerParameters.isFilterEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		this.pickerParameters.isFilterTextModus = this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE) != null && this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue()) : false;
		try {
			this.pickerParameters.altitudeClimbSensorSelection = this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB) != null && this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() != null //$NON-NLS-1$
					? Integer.parseInt(this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue()) : 0;
		}
		catch (NumberFormatException e) {
			this.pickerParameters.altitudeClimbSensorSelection = 0;
		}
		try {
			this.pickerParameters.isChannelPercentEnabled = this.getChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE) != null && this.getChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE).getValue() != null //$NON-NLS-1$
					? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE).getValue()) : true;
		}
		catch (NumberFormatException e) {
			this.pickerParameters.isChannelPercentEnabled = true;
		}		
		this.pickerParameters.latitudeToleranceFactor = this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 50.0;
		this.pickerParameters.longitudeToleranceFactor = this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 15.0;
	}

	/**
	 * Collect the settings relevant for the values inserted in the histo vault.
	 * @return the settings which determine the measurement values returned by the reader
	 */
	@Override
	public String getReaderSettingsCsv() {
		return this.pickerParameters.getReaderSettingsCsv();
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public HoTTAdapterSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
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
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
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
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// LogView doesn't support HoTT sensor logfiles
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int tmpHeight, tmpClimb3, tmpClimb10, tmpCapacity, tmpRevolution, tmpTemperatureFet, tmpVoltage, tmpCurrent, tmpCellVoltage, tmpVoltage1, tmpVoltage2, tmpLatitude, tmpLongitude, tmpPackageLoss, tmpVoltageRx,
				tmpTemperatureRx;

		switch (this.serialPort.protocolType) {
		case TYPE_19200_V3:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
					points[1] = (dataBuffer[9] & 0xFF) * 1000;
					points[2] = (dataBuffer[5] & 0xFF) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 11) * 1000;
					points[4] = (dataBuffer[13] & 0xFF) * -1000;
					points[5] = (dataBuffer[9] & 0xFF) * -1000;
					points[6] = (dataBuffer[6] & 0xFF) * 1000;
					points[7] = (dataBuffer[7] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 31) {
					// 0=RXSQ, 1=Altitude, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 16) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 26) * 1000;
					points[5] = (dataBuffer[8] & 0xFF) * 1000;
					points[6] = (dataBuffer[5] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 40) {
					// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 
					//12=satellites 13=GPS-fix 14=EventGPS 15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
					points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
					points[2] = DataParser.parse2Short(dataBuffer, 25) * 10000 + DataParser.parse2Short(dataBuffer, 27);
					points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
					points[3] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[5] = (dataBuffer[35] & 0xFF) * 1000;
					points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
					points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[8] = (dataBuffer[16] & 0xFF) * 1000;
					points[9] = 0;
					points[10] = (dataBuffer[8] & 0xFF) * 1000;
					points[11] = (dataBuffer[5] & 0xFF) * 1000;
			}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 48) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; // 5=Balance
					for (int j = 0; j < 6; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					// calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[13] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
					points[15] = (dataBuffer[37] & 0xFF) * 1000;
					points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[17] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[18] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[19] = (dataBuffer[26] & 0xFF) * 1000;
					points[20] = (dataBuffer[27] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 51) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; // 5=Balance
					for (int j = 0; j < 14; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					// calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[20] = DataParser.parse2Short(dataBuffer, 36) * 1000;
					points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
					points[22] = (dataBuffer[46] & 0xFF) * 1000;
					points[23] = DataParser.parse2Short(dataBuffer, 30) * 1000;
					points[24] = DataParser.parse2Short(dataBuffer, 32) * 1000;
					points[25] = (dataBuffer[34] & 0xFF) * 1000;
					points[26] = (dataBuffer[35] & 0xFF) * 1000;
				}
				break;
			}
			break;

		case TYPE_19200_V4:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length >= 17) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx, 8=VoltageRxMin
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 11);
					tmpVoltageRx = (dataBuffer[6] & 0xFF);
					tmpTemperatureRx = (dataBuffer[7] & 0xFF);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 120) {
						points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
						points[1] = (dataBuffer[9] & 0xFF) * 1000;
						points[2] = (dataBuffer[5] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[13] & 0xFF) * -1000;
						points[5] = (dataBuffer[8] & 0xFF) * -1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = (tmpTemperatureRx - 20) * 1000;
						points[8] = (dataBuffer[10] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 16);
					if (!this.pickerParameters.isFilterEnabled || tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = (DataParser.parse2Short(dataBuffer, 22) - 30000) * 10;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 24);
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 26);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = (dataBuffer[6] & 0xFF) * 1000;
					points[6] = (dataBuffer[7] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					tmpLatitude = DataParser.parse2Short(dataBuffer, 20);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 25);
					tmpHeight = DataParser.parse2Short(dataBuffer, 31);
					tmpClimb3 = dataBuffer[35] & 0xFF;
					if (!this.pickerParameters.isFilterEnabled || (tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 27);
						points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[8] = (dataBuffer[38] & 0xFF) * 1000;
						points[9] = 0;
						points[10] = (dataBuffer[6] & 0xFF) * 1000;
						points[11] = (dataBuffer[7] & 0xFF) * 1000;
						//12=satellites 13=GPS-fix 14=EventGPS 
						points[12] = (dataBuffer[36] & 0xFF) * 1000;
						switch (dataBuffer[37]) { //sat-fix
						case '-':
							points[13] = 0;
							break;
						case '2':
							points[13] = 2000;
							break;
						case '3':
							points[13] = 3000;
							break;
						case 'D':
							points[13] = 4000;
							break;
						default:
							try {
								points[13] = Integer.valueOf(String.format("%c",dataBuffer[37])) * 1000;
							}
							catch (NumberFormatException e1) {
								points[13] = 1000;
							}
							break;
						}
						points[14] = (dataBuffer[14] & 0xFF) * 1000; //14=EventGPS
						//15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
						points[15] = (dataBuffer[38] & 0xFF) * 1000; //Home direction
						
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, 39, dataBuffer.length));
							if (dataBuffer[41] > 40 && dataBuffer[41] <= 100 && dataBuffer[47] > 0 && dataBuffer[47] <= 100)
								log.log(Level.INFO, String.format("Sparrow: Voltage GU = %d; servo pulse = %d", 
										dataBuffer[41] * 100, //voltage GPS 4-10
										dataBuffer[47] * 1000));//servo pulse 0-100
							if (dataBuffer[41] == 0 && dataBuffer[39] > -120 && dataBuffer[39] <= 120) 
								log.log(Level.INFO, String.format("SM GPS-Logger: servo pulse = %d; not used = %d", 
										dataBuffer[39] * 1000, //servoPulse 0-100
										dataBuffer[41]));//not used
						}
						if (dataBuffer[41] > 40 && dataBuffer[41] <= 100 && dataBuffer[47] > 0 && dataBuffer[47] <= 100) { //RCE Sparrow
							//30=servoPulse 31=n/a 32=voltage GU 33=HH:mm:ss.SSS 34=yy-dd-mm 35=Altitude MSL 36=ENL 37=Version
							points[16] = dataBuffer[47] * 1000; //servo pulse
							points[17] = 0;
							points[18] = dataBuffer[41] * 100; //voltage GPS
							points[19] = dataBuffer[42] * 10000000 + dataBuffer[43] * 100000 + dataBuffer[44] * 1000 + dataBuffer[45]*10;//HH:mm:ss.SSS
							points[20] = ((dataBuffer[48]-48) * 1000000 + (dataBuffer[50]-48) * 10000 + (dataBuffer[49]-48) * 100) * 10;//yy-dd-mm
							points[21] = DataParser.parse2Short(dataBuffer, 39) * 1000;; //Altitude MSL
							points[22] = (dataBuffer[46] & 0xFF) * 1000; //ENL
							//three char
							points[23] = 4 * 1000; //Version
						}
						else if (dataBuffer[41] == 0 && dataBuffer[39] > -120 && dataBuffer[39] <= 120) { //SM GPS-Logger				
							//30=servoPulse 31=airSpeed 32=n/a 33=GyroX 34=GyroY 35=GyroZ 36=ENL 37=Version	
							points[16] = dataBuffer[39] * 1000; //servoPulse
							points[17] = dataBuffer[40] * 1000; //airSpeed
							points[18] = dataBuffer[41] * 1000; //n/a
							points[19] = DataParser.parse2Short(dataBuffer, 42) * 1000; //Acc x
							points[20] = DataParser.parse2Short(dataBuffer, 44) * 1000; //Acc y
							points[21] = DataParser.parse2Short(dataBuffer, 46) * 1000; //Acc z
							points[22] = (dataBuffer[48] & 0xFF) * 1000; //ENL
							//three char
							points[23] = 125 * 1000; //Version
						}
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 33);
					tmpClimb3 = dataBuffer[37] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					if (!this.pickerParameters.isFilterEnabled || (tmpClimb3 > 30 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600)) {
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 6; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = ((dataBuffer[26] & 0xFF) - 20) * 1000;
						points[20] = ((dataBuffer[27] & 0xFF) - 20) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 36);
					tmpClimb3 = dataBuffer[46] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 30);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 32);
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					if (!this.pickerParameters.isFilterEnabled || (tmpClimb3 > 30 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600)) {
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 14; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
						points[22] = tmpClimb3 * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = ((dataBuffer[34] & 0xFF) - 20) * 1000;
						points[26] = ((dataBuffer[35] & 0xFF) - 20)  * 1000;
						points[27] = DataParser.parse2Short(dataBuffer, 47) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
				if (dataBuffer.length == 57) {
					// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					tmpVoltage = DataParser.parse2Short(dataBuffer, 16);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 24);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 20);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 28);
					tmpTemperatureFet = (dataBuffer[35] & 0xFF) + 20;
					if (!this.pickerParameters.isFilterEnabled
							|| tmpVoltage > 0 && tmpVoltage < 1000 && tmpCurrent < 4000 && tmpCurrent > -10 && tmpRevolution > -1
							&& tmpRevolution < 20000 && !(points[6] != 0 && points[6] / 1000 - tmpTemperatureFet > 20)) {
						points[1] = tmpVoltage * 1000;
						points[2] = tmpCurrent * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						points[5] = tmpRevolution * 1000;
						points[6] = tmpTemperatureFet * 1000;
					}
				}
				break;
			}
			break;

		case TYPE_115200:
			switch (dataBuffer[0]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_115200:
				if (dataBuffer.length >= 21) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 12);
					tmpVoltageRx = dataBuffer[15] & 0xFF;
					tmpTemperatureRx = DataParser.parse2Short(dataBuffer, 10);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 100) {
						this.pickerParameters.reverseChannelPackageLossCounter.add((dataBuffer[5] & 0xFF) == 0 && (dataBuffer[4] & 0xFF) == 0 ? 0 : 1);
						points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;// (dataBuffer[16] & 0xFF) * 1000;
						points[1] = (dataBuffer[17] & 0xFF) * 1000;
						points[2] = (dataBuffer[14] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[5] & 0xFF) * -1000;
						points[5] = (dataBuffer[4] & 0xFF) * -1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[18] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
				if (dataBuffer.length >= 25) {
					// 0=RXSQ, 1=Altitude, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 10) + 500;
					if (!this.pickerParameters.isFilterEnabled || tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 16) * 1000;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 18) + 30000;
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 20) + 30000;
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = dataBuffer[4] * 1000;
					points[6] = (dataBuffer[5] + 20) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_115200:
				if (dataBuffer.length >= 46) {
					//0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
					//15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
					tmpLatitude = DataParser.parse2Short(dataBuffer, 16);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 20);
					tmpHeight = DataParser.parse2Short(dataBuffer, 14) + 500;
					tmpClimb3 = dataBuffer[30] + 120;
					if (!this.pickerParameters.isFilterEnabled || (tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = tmpLatitude * 10000 + DataParser.parse2Short(dataBuffer, 18);
						points[1] = dataBuffer[26] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[2] = dataBuffer[27] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = (DataParser.parse2Short(dataBuffer, 28) + 30000) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 10) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[8] = DataParser.parse2Short(dataBuffer, 24) * 500;
						points[9] = 0;
						points[10] = dataBuffer[4] * 1000;
						points[11] = (dataBuffer[5] + 20) * 1000;
						//18=Satellites 19=Fix
						points[12] = (dataBuffer[32] & 0xFF) * 1000;
						switch (dataBuffer[33]) { //sat-fix
						case '-':
							points[13] = 0;
							break;
						case '2':
							points[13] = 2000;
							break;
						case '3':
							points[13] = 3000;
							break;
						case 'D':
							points[13] = 4000;
							break;
						default:
							try {
								points[13] = Integer.valueOf(String.format("%c",dataBuffer[33])) * 1000;
							}
							catch (NumberFormatException e1) {
								points[13] = 1000;
							}
							break;
						}
						points[14] = (dataBuffer[1] & 0xFF) * 1000; //GPS inverse Event
						//15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
						points[15] = DataParser.parse2Short(dataBuffer, 34) * 1000; //Home direction
						
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, 36, dataBuffer.length));
							if (dataBuffer[38] > 40 && dataBuffer[38] <= 100 && dataBuffer[45] > 0 && dataBuffer[45] <= 100)
								log.log(Level.INFO, String.format("Sparrow: Voltage GU = %d; servo pulse = %d", 
										dataBuffer[38] * 100, //voltage GPS
										dataBuffer[45] * 1000));//servo pulse
							if (dataBuffer[38] == 0 && dataBuffer[36] > -120 && dataBuffer[36] <= 120) 
								log.log(Level.INFO, String.format("SM GPS-Logger: servo pulse = %d; not used = %d", 
										dataBuffer[36] * 1000, //servoPulse
										dataBuffer[38]));//not used
						}
						if (dataBuffer[38] > 40 && dataBuffer[38] <= 100 && dataBuffer[45] > 0 && dataBuffer[45] <= 100) { //RCE Sparrow
							//16=servoPulse 17=fixed 18=Voltage 19=GPS hh:mm 20=GPS sss.SSS 21=MSL Altitude 22=ENL 23=Version	
							points[16] = dataBuffer[45] * 1000; //servo pulse
							points[17] = 0; //(dataBuffer[46] & 0xFF) * 1000; //0xDF
							points[18] = dataBuffer[38] * 100; //voltage GPS
							points[19] = dataBuffer[40] * 10000000 + dataBuffer[41] * 100000 + dataBuffer[42] * 1000 + dataBuffer[43]*10;//HH:mm:ss.SSS
							points[20] = ((dataBuffer[46]-48) * 1000000 + (dataBuffer[48]-48) * 10000 + (dataBuffer[47]-48) * 100) * 10;//yy-dd-mm
							points[21] = DataParser.parse2Short(dataBuffer, 39) * 1000;; //Altitude MSL
							points[22] = (dataBuffer[44] & 0xFF) * 1000; //ENL
							//three char
							points[23] = 4 * 1000; //Version
						}
						else if (dataBuffer[38] == 0 && dataBuffer[36] > -120 && dataBuffer[36] <= 120) { //SM GPS-Logger				
							//16=servoPulse 17=airSpeed 18=n/a 19=GyroX 20=GyroY 21=GyroZ 22=ENL 23=Version	
							points[16] = dataBuffer[36] * 1000; //servoPulse
							points[17] = dataBuffer[37] * 1000; //airSpeed
							points[18] = dataBuffer[38] * 1000; //n/a
							points[19] = DataParser.parse2Short(dataBuffer, 40) * 1000; //Acc x 
							points[20] = DataParser.parse2Short(dataBuffer, 42) * 1000; //Acc y
							points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000; //Acc z
							points[22] = (dataBuffer[46] & 0xFF) * 1000; //ENL
							//three char
							points[23] = 125 * 1000; //Version
						}
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
				if (dataBuffer.length >= 49) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 36);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 38);
					tmpHeight = DataParser.parse2Short(dataBuffer, 32) + 500;
					tmpClimb3 = dataBuffer[44] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					if (!this.pickerParameters.isFilterEnabled || (tmpClimb3 > 30 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600)) {
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 34) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 6; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 30) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = (DataParser.parse2Short(dataBuffer, 42) + 30000) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 40) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = DataParser.parse2Short(dataBuffer, 26) * 1000;
						points[20] = DataParser.parse2Short(dataBuffer, 28) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
				if (dataBuffer.length >= 60) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 50);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 52);
					tmpHeight = DataParser.parse2Short(dataBuffer, 46) + 500;
					tmpClimb3 = dataBuffer[56] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 38);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 40);
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					if (!this.pickerParameters.isFilterEnabled || (tmpClimb3 > 30 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600)) {
						points[1] = DataParser.parse2Short(dataBuffer, 50) * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 48) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 14; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = (DataParser.parse2Short(dataBuffer, 54) + 30000) * 1000;
						points[22] = (dataBuffer[46] + 120) * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = DataParser.parse2Short(dataBuffer, 42) * 1000;
						points[26] = DataParser.parse2Short(dataBuffer, 44) * 1000;
						points[27] = DataParser.parse2Short(dataBuffer, 58) * 1000;
					}
				}
				break;
			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
				if (dataBuffer.length >= 34) {
					// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					tmpVoltage = DataParser.parse2Short(dataBuffer, 10);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 14);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 22);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 18);
					tmpTemperatureFet = DataParser.parse2Short(dataBuffer, 24);
					if (!this.pickerParameters.isFilterEnabled
							|| tmpVoltage > 0 && tmpVoltage < 1000 && tmpCurrent < 4000 && tmpCurrent > -10 && tmpRevolution > -1
							&& tmpRevolution < 20000 && !(points[6] != 0 && points[6] / 1000 - tmpTemperatureFet > 20)) {
						points[1] = tmpVoltage * 1000;
						points[2] = tmpCurrent * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						points[5] = tmpRevolution * 1000;
						points[6] = tmpTemperatureFet * 1000;
					}
				}
				break;
			case HoTTAdapter.SENSOR_TYPE_SERVO_POSITION_115200:
				if (dataBuffer.length >= 74 && this.pickerParameters.isChannelsChannelEnabled) {
					//log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
					// 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
					//update number of Tx Channels in dependency of used XML
					for (int i = 0, j = 0; i < 32; i++, j+=2) {
						points[3 + i] = (DataParser.parse2Short(dataBuffer, 8 + j) / 2 + 1500) * 1000;
					}
					if (log.isLoggable(Level.FINE)) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0, j = 0; i < 32; i++, j += 2) {
							sb.append(String.format("%2d = %4d; ", i + 1, DataParser.parse2Short(dataBuffer, 8 + j) / 16 + 50));
						}
						log.log(Level.FINE, sb.toString());
					}
				}
				break;
			}
			break;
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
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINER)) HoTTAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * Add record data points from file stream to each measurement.
	 * It is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * Do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * Reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param recordSet is the target object holding the records (curves) which include measurement curves and calculated curves
	 * @param dataBuffer holds rows for each time step (i = recordDataSize) with measurement data (j = recordNamesLength equals the number of measurements)
	 * @param recordDataSize is the number of time steps
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, int[] maxPoints, int[] minPoints, Analyzer analyzer) throws DataInconsitsentException {
		if (maxPoints.length != minPoints.length || maxPoints.length == 0) throw new DataInconsitsentException("number of max/min points differs: " + maxPoints.length + "/" + minPoints.length); //$NON-NLS-1$

		int recordTimespan_ms = 10;
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(recordSet.getChannelConfigNumber(), maxPoints, minPoints, recordTimespan_ms, analyzer);
		int[] points = histoRandomSample.getPoints();
		IntBuffer intBuffer = ByteBuffer.wrap(dataBuffer).asIntBuffer(); // no performance penalty compared to familiar bit shifting solution
		for (int i = 0, pointsLength = points.length; i < recordDataSize; i++) {
			for (int j = 0, iOffset = i * pointsLength + recordDataSize; j < pointsLength; j++) {
				points[j] = intBuffer.get(j + iOffset);
			}
			int timeStep_ms = intBuffer.get(i) / 10;
			if (histoRandomSample.capturePoints(timeStep_ms)) recordSet.addPoints(points, timeStep_ms);
		}
		recordSet.syncScaleOfSyncableRecords();
		if (log.isLoggable(Level.FINE)) log.log(Level.INFO, String.format("%s processed: %,9d", recordSet.getChannelConfigName(), recordDataSize)); //$NON-NLS-1$
	}

	/**
	 * @return true if the device supports a native file import for histo purposes
	 */
	@Override
	public boolean isHistoImportSupported() {
		return this.getClass().equals(HoTTAdapter.class) && !this.getClass().equals(HoTTAdapterD.class) && !this.getClass().equals(HoTTAdapterM.class) && !this.getClass().equals(HoTTAdapterX.class)
				&& !this.getClass().equals(HoTTViewer.class);
	}

	private void setSupportedImportExtentions() {
		if (isHistoImportSupported()) {
			this.importExtentions = Arrays.stream(getDataBlockPreferredFileExtention().split(GDE.REGEX_FILE_EXTENTION_SEPARATION)) //
					.map(s -> s.substring(s.lastIndexOf(GDE.CHAR_DOT))).map(e -> e.toLowerCase()) //
					.collect(Collectors.toList());
		} else {
			this.importExtentions = new ArrayList<>();
		}
	}

	/**
	 * @return the device's native file extentions if the device supports histo imports (e.g. '.bin' or '.log')
	 */
	@Override
	public List<String> getSupportedImportExtentions() {
		if (this.importExtentions == null) setSupportedImportExtentions();
		return this.importExtentions;
	}

	/**
	 * Extended consumer supporting exceptions.
	 */
	@FunctionalInterface
	interface CheckedConsumer<T> {
		void accept(T t) throws DataInconsitsentException, IOException, DataTypeException;
	}

	/**
	 * create recordSet and add record data size points from binary file to each measurement.
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * collects life data if device setting |isLiveDataActive| is true.
	 * reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param inputStream for loading the log data
	 * @param truss references the requested vault for feeding with the results (vault might be without measurements, settlements and scores)
	 */
	@Override
	public void getRecordSetFromImportFile(Supplier<InputStream> inputStream, VaultCollector truss, Analyzer analyzer)
			throws DataInconsitsentException, IOException, DataTypeException {
		String fileEnding = PathUtils.getFileExtention(truss.getVault().getLoadFilePath());
		if (GDE.FILE_ENDING_DOT_BIN.equals(fileEnding)) {
			HoTTbinHistoReader histoReader = new HoTTbinHistoReader(new PickerParameters(analyzer));
			histoReader.read(inputStream, truss);
		}
		else if (GDE.FILE_ENDING_DOT_LOG.equals(fileEnding)) {
			HashMap<String, String> infoHeader = null;
			try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
				infoHeader = new InfoParser((s) -> {
				}).getFileInfo(info_in, truss.getVault().getLoadFilePath(), truss.getVault().getLogFileLength());
				if (infoHeader == null || infoHeader.isEmpty()) return;

				EnumSet<Sensor> detectedSensors = Sensor.getSetFromDetected(infoHeader.get(HoTTAdapter.DETECTED_SENSOR));
				if (!Sensor.getChannelNumbers(detectedSensors).contains(truss.getVault().getVaultChannelNumber())) return;

				if (Integer.parseInt(infoHeader.get(HoTTAdapter.LOG_COUNT)) <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN / 5) return;

				HoTTlogHistoReader histoReader = new HoTTlogHistoReader(new PickerParameters(analyzer), infoHeader);
				histoReader.read(inputStream, truss);
			}
		}
		else {
			throw new UnsupportedOperationException(truss.getVault().getLoadFilePath());
		}
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int channel = recordSet.getChannelConfigNumber();
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb, 5=Velocity, 6=Distance, 7=Direction, 8=TripDistance, 9=VoltageRx, 10=TemperatureRx
				if (channel == 1 && ordinal >= 0 && ordinal <= 5) { // Receiver
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else if (channel == 6 && ordinal == 38) { //Channels warning
					// 0=FreCh, 1=Tx, 2=Rx, 3=Ch01, 4=Ch02 .. 18=Ch16, 19=Ch17 .. 34=Ch32 35=PowerOff, 36=BattLow, 37=Reset, 38=warning
					dataTableRow[index + 1] = record.realGet(rowIndex) == 0
							? GDE.STRING_EMPTY
							: String.format("'%c'", ((record.realGet(rowIndex) / 1000) + 64));
				}
				else if (channel == 3) {
					//GPS: 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS 15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version
					if (ordinal == 19 && record.getUnit().endsWith("HH:mm:ss.SSS")) { //hhmmssSSSOrdinal = 19
						dataTableRow[index + 1] = HoTTAdapter.getFormattedTime(record.realGet(rowIndex));
					}
					else if (ordinal == 20 && record.getUnit().endsWith("yy-MM-dd")) { 
						dataTableRow[index + 1] = HoTTAdapter.getFormattedDate(record.realGet(rowIndex)/10);
					}
					else {
						dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
					}
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		} catch (RuntimeException e) {
			HoTTAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * @param timeValue
	 * @return formatted time HH:mm:ss.SSS
	 */
	public static String getFormattedTime(final int timeValue) {
		int tmpHH = timeValue/10000000;
		int tmpMM = timeValue/100000 - tmpHH*100;
		int tmpSS = timeValue/1000 - tmpMM*100 - tmpHH*10000;
		int tmpSSS = timeValue - tmpSS*1000 - tmpMM*100000 - tmpHH*10000000;
		return String.format("%02d:%02d:%02d.%03d", tmpHH, tmpMM, tmpSS, tmpSSS); //$NON-NLS-1$;
	}

	/**
	 * @param dateValue
	 * @return formatted date yy-MM-dd
	 */
	public static String getFormattedDate(int dateValue) {
		int tmpYY = dateValue/1000000;
		int tmpMM = dateValue/10000 - tmpYY*100;
		int tmpDD = dateValue/100 - tmpMM*100 - tmpYY*10000;
		return String.format("%02d-%02d-%02d", tmpYY, tmpMM, tmpDD); //$NON-NLS-1$
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getAbstractParent().getChannelConfigNumber() == 3 && (record.getOrdinal() == 1 || record.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				int grad = ((int) (value / 1000));
				double minuten = (value - (grad * 1000.0)) / 10.0;
				newValue = grad + minuten / 60.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 34) && value != 0.) {
			//channel recordSet, channel values
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor + 0.001;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor + offset;
			}
		}
		// ET logic differs compared to prepareDataTableRow for getChannelConfigNumber() == 1 (Receiver)
		else {
			newValue = (value - reduction) * factor + offset;
		}

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
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if ((record.getOrdinal() == 1 || record.getOrdinal() == 2) && record.getAbstractParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude )
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				int grad = (int) value;
				double minuten = (value - grad * 1.0) * 60.0;
				newValue = (grad + minuten / 100.0) * 1000.0;
		} 
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 34) && value != 0.) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = value / factor + reduction - 0.001;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

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
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() && record.isActive() != measurement.isActive()) { //corrected values from older OSD might be overwritten p.e. VoltageRx_min
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(measurement.isActive() && record.hasReasonableData());
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); // reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		calculateInactiveRecords(recordSet);
		recordSet.syncScaleOfSyncableRecords();
		this.updateVisibilityStatus(recordSet, true);
		this.application.updateStatisticsData();
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 */
	@Override
	public void calculateInactiveRecords(RecordSet recordSet) {
		if (recordSet.getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude 3=Altitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			Record recordLatitude = recordSet.get(1);
			Record recordLongitude = recordSet.get(2);
			Record recordAlitude = recordSet.get(3);
			if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) {
				int recordSize = recordLatitude.realSize();
				int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix
				// check GPS latitude and longitude
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
				}
				startAltitude = recordAlitude.get(indexGPS); // set initial altitude to enable absolute altitude calculation

				GPSHelper.calculateTripLength(this, recordSet, 1, 2, 3, startAltitude, 9);
				// GPSHelper.calculateLabs(this, recordSet, 1, 2, 7, 9, 6);
			}
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public HoTTAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		switch (this.application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			importDeviceData();
			break;

		case DeviceCommPort.ICON_SET_START_STOP:
			this.serialPort.isInterruptedByUser = true;
			break;
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2400), "LogData"); //$NON-NLS-1$

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					HoTTAdapter.this.application.setPortConnected(true);
					boolean isInitialSwitched = false;

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						HoTTAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							// String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								// use a copy of the picker parameters to avoid changes by the reader
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
									HoTTbinReader.read(selectedImportFile, new PickerParameters(HoTTAdapter.this.pickerParameters));
								}
								else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									HoTTlogReader.read(selectedImportFile, new PickerParameters(HoTTAdapter.this.pickerParameters));
								}
								if (!isInitialSwitched) {
									if (HoTTAdapter.this.application.getActiveChannel().getActiveRecordSet() == null) {
										Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();
										HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									} else {
										String recordSetType = HoTTAdapter.this.application.getActiveChannel().getActiveRecordSet().getName().split(Pattern.quote("["))[0].split(Pattern.quote(")"))[1];
										Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();

										// find the latest recordSet just imported by this method
										if (Settings.getInstance().isFirstRecordSetChoice()) {
											String lastCurrentNumber = "";
											String lastNameMatch = null;
											for (String tmpName : selectedChannel.getRecordSetNames()) {
												String currentNumber = tmpName.split(Pattern.quote(")"))[0];
												if (!currentNumber.equals(lastCurrentNumber)) {
													lastCurrentNumber = currentNumber;
													lastNameMatch = tmpName;
												}
											}
											HoTTbinReader.channels.switchChannel(selectedChannel.getName());
											selectedChannel.switchRecordSet(lastNameMatch);
										} else {
											String lastNameMatch = null;
											for (String tmpName : selectedChannel.getRecordSetNames()) {
												if (tmpName.contains(recordSetType)) lastNameMatch = tmpName;
											}
											HoTTbinReader.channels.switchChannel(selectedChannel.getName());
											selectedChannel.switchRecordSet(lastNameMatch);
										}
									}
									isInitialSwitched = true;
								}
								WaitTimer.delay(500);
							} catch (Exception e) {
								HoTTAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				} finally {
					HoTTAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * import device specific *.bin data files
	 * @param filePath
	 */
	@Override
	public void importDeviceData(Path filePath) {
		if (!this.application.getDeviceSelectionDialog().checkDataSaved()) return;

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					boolean isInitialSwitched = false;
					HoTTAdapter.this.application.setPortConnected(true);

					if (filePath.getFileName().toString().length() > MIN_FILENAME_LENGTH) {
						for (Channel channel : HoTTAdapter.this.channels.values()) {
							channel.clear();
						}

						try {
							// use a copy of the picker parameters to avoid changes by the reader
							if (HoTTAdapter.this.getClass().equals(HoTTAdapter.class)) {
								if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
									HoTTbinReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
								else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
									HoTTlogReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2.class)) {
								if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
									HoTTbinReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
								else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
									HoTTlogReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2M.class)) {
								if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
									HoTTbinReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
								else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
									HoTTlogReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterD.class)) {
								if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
									HoTTbinReaderD.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
								else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
									HoTTlogReaderD.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterM.class)) {
								if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
									HoTTbinReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
								else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
									HoTTlogReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterX.class)) {
								HoTTbinReaderX.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							}
							else
								throw new UnsupportedOperationException();

							if (!isInitialSwitched) {
								Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();
								if (HoTTAdapter.this.getClass().equals(HoTTAdapter.class) || HoTTAdapter.this.getClass().equals(HoTTAdapterM.class) || HoTTAdapter.this.getClass().equals(HoTTAdapterX.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
								}
								else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2.class) || HoTTAdapter.this.getClass().equals(HoTTAdapter2M.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
										selectedChannel.switchRecordSet(HoTTbinReader2.recordSet.getName());
									else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
										selectedChannel.switchRecordSet(HoTTlogReader2.recordSet.getName());						
								}
								else if (HoTTAdapter.this.getClass().equals(HoTTAdapterD.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_BIN))
										selectedChannel.switchRecordSet(HoTTbinReaderD.recordSet.getName());
									else if (filePath.getFileName().toString().endsWith(GDE.FILE_ENDING_LOG)) 
										selectedChannel.switchRecordSet(HoTTlogReaderD.recordSet.getName());						
								}
								else
									throw new UnsupportedOperationException();
								isInitialSwitched = true;
							}

							WaitTimer.delay(500);
						} catch (Exception e) {
							HoTTAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				} finally {
					HoTTAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		// MenuItem convertGPXItem;
		// MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2405));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2406));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2407));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			// convertGPXItem = new MenuItem(exportMenue, SWT.PUSH);
			// convertGPXItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0728));
			// convertGPXItem.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(java.util.logging.Level.FINEST, "convertGPXItem action performed! " + e); //$NON-NLS-1$
			// export2GPX(false);
			// }
			// });
			//
			// convertGPXGarminItem = new MenuItem(exportMenue, SWT.PUSH);
			// convertGPXGarminItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0729));
			// convertGPXGarminItem.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(java.util.logging.Level.FINEST, "convertGPXGarminItem action performed! " + e); //$NON-NLS-1$
			// export2GPX(true);
			// }
			// });
		}
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
			String[] messageParams = new String[GDE.MOD1.length + 1];
			System.arraycopy(GDE.MOD1, 0, messageParams, 1, GDE.MOD1.length);
			messageParams[0] = this.getDeviceConfiguration().getDataBlockPreferredFileExtention();
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2416, messageParams));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2416));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), 2, 1, 3, 6, 5, 9, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param isGarminExtension
	 */
	public void export2GPX(final boolean isGarminExtension) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		if (isGarminExtension)
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 1, 2, 3, 6, -1, -1, -1, -1, new int[] { -1, -1, -1 });
		else
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 1, 2, 3, 6, -1, -1, -1, -1, new int[0]);
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return true if the record is a latitude or longitude record
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		if (this.application.getActiveChannelNumber() == 3) {
			// 0=RXSQ, 1=Latitude, 2=Longitude
			return record.getOrdinal() == 1 || record.getOrdinal() == 2;
		}
		return false;
	}

	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		Record recordLatitude = recordSet.get(1);
		Record recordLongitude = recordSet.get(2);
		Record gpsAlitude = recordSet.get(3);

		return String.format("%02d%05d%s%03d%05d%s%c%05.0f%05.0f", //$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S", //$NON-NLS-1$ //$NON-NLS-2$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W", //$NON-NLS-1$ //$NON-NLS-2$
				fixValidity, (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude), (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude));
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && activeChannel.getNumber() == 3) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				containsGPSdata = activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(2).hasReasonableData();
			}
		}
		return containsGPSdata;
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
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(2, 1, 3, additionalMeasurementOrdinal, 5, 9, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 6;

		return this.kmzMeasurementOrdinal;
	}

	/**
	 * @param isFilterEnabled the isFilterEnabled to set
	 */
	public synchronized void setFilterProperties(boolean isFilterEnabled) {
		this.pickerParameters.isFilterEnabled = isFilterEnabled;
	}

	/**
	 * @param isTextModusFilterEnabled the isTextModusFilterEnabled to set
	 */
	public synchronized void setTextModusFilter(boolean isTextModusFilterEnabled) {
		this.pickerParameters.isFilterTextModus = isTextModusFilterEnabled;
	}

	/**
	 * @param isChannelPercentEnabled the isChannelPercentEnabled to set
	 */
	public synchronized void setChannelPercent(boolean isChannelPercentEnabled) {
		this.pickerParameters.isChannelPercentEnabled = isChannelPercentEnabled;
	}
	
	/**
	 * @param altitudeClimbComboSelectionIndex the altitudeClimbSensorSelection index to set
	 */
	public synchronized void setAltitudeClimbSelectionProperties(int altitudeClimbComboSelectionIndex) {
		this.pickerParameters.altitudeClimbSensorSelection = altitudeClimbComboSelectionIndex;
	}

	/**
	 * @return the tooltip text for the import menu bar button
	 */
	public static String getImportToolTip() {
		DeviceConfiguration hoTTConfiguration = Analyzer.getInstance().getDeviceConfigurations().get("HoTTAdapter");
		String fileExtentions = hoTTConfiguration != null ? hoTTConfiguration.getDataBlockPreferredFileExtention() : GDE.STRING_QUESTION_MARK;
		return Messages.getString(MessageIds.GDE_MSGT2404, new Object[] { fileExtentions });
	}

	/**
	 * @param isChannelEnabled the isChannelEnabled to set
	 */
	public synchronized void setChannelEnabledProperty(boolean isChannelEnabled) {
		this.pickerParameters.isChannelsChannelEnabled = isChannelEnabled;
	}

	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.
	 */
	@Override
	public CTabItem getUtilityDeviceTabItem() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (HoTTAdapter.this.isMdlTabRequested()) {
					CTabItem mdlTabItem = HoTTAdapter.this.getMdlTabItem();
					if (mdlTabItem != null) {
						DataExplorer.getInstance().registerCustomTabItem(mdlTabItem);
					}
				}
			}
		});

		return new FileTransferTabItem(this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount(), this, this.serialPort);
	}

	/**
	 * query if the MDL decoder tab item can be displayed
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isMdlTabRequested() {
		boolean rc = true;
		try {
			String className = "de.treichels.hott.mdlviewer.swt.Launcher";//$NON-NLS-1$
			// log.log(Level.OFF, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			// Class c = Class.forName(className);
			Constructor<?> constructor = c.getDeclaredConstructor();
			if (constructor != null) {
				constructor.newInstance();
			} else {
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "de.treichels.hott.mdlviewer.swt.Launcher can not be loaded"); //$NON-NLS-1$
				rc = false;
			}
		} catch (final Throwable t) {
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "de.treichels.hott.mdlviewer.swt.Launcher can not be loaded"); //$NON-NLS-1$
			rc = false;
		}
		return rc;
	}

	/**
	 * This function allows to register a CTabItem to to display MDL content converted to HTML
	 */
	public CTabItem getMdlTabItem() {
		Object inst = null;
		try {
			String className = "de.treichels.hott.mdlviewer.swt.MdlTabItem";//$NON-NLS-1$
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { CTabFolder.class, int.class, int.class });
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {

				// set directory where to start search for mdl files
				System.setProperty("log.dir", Settings.getLogFilePath().substring(0, Settings.getLogFilePath().lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX))); //$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "log.dir =  " + System.getProperty("log.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty("mdl.dir", Settings.getInstance().getDataFilePath());//$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "mdl.dir =  " + System.getProperty("mdl.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
				System.setProperty("program.dir", url.getFile().substring(0, url.getPath().lastIndexOf(DataExplorer.class.getSimpleName())));//$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "program.dir =  " + System.getProperty("program.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty("template.dir", "");// load from classpath //$NON-NLS-1$ //$NON-NLS-2$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "template.dir =  " + System.getProperty("template.dir")); //$NON-NLS-1$ //$NON-NLS-2$

				inst = constructor.newInstance(new Object[] { this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount() });
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}
		if (HoTTAdapter.log.isLoggable(java.util.logging.Level.OFF) && inst != null) HoTTAdapter.log.log(java.util.logging.Level.OFF, "loading TabItem " + ((CTabItem) inst).getText()); //$NON-NLS-1$

		if (inst != null) ((CTabItem) inst).setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		return (CTabItem) inst;
	}

	/**
	 * calculate labs based on Rx dbm and based on distance from start point
	 * HoTTAdapterD
	 * //5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
	 * //15=DistanceStart, 112=DiffDistance, 113=LapsDistance
	 * @param recordSet
	 * @param channelNumber
	 * @param ordinalSourceRx_dbm
	 * @param ordinalSmoothRx_dbm
	 * @param ordinalDiffRx_dbm
	 * @param ordinalLabsRx_dbm
	 * @param ordinalSourceDist
	 * @param ordinalDiffDist
	 * @param ordinalLapsDistance
	 */
	protected void runLabsCalculation(final RecordSet recordSet, final int channelNumber, final int ordinalSourceRx_dbm, final int ordinalSmoothRx_dbm, final int ordinalDiffRx_dbm,
			final int ordinalLabsRx_dbm, final int ordinalSourceDist, final int ordinalDiffDist, final int ordinalLapsDistance) {
		// laps calculation init begin
		Record recordSourceRx_dbm = recordSet.get(ordinalSourceRx_dbm);
		Record recordSmoothRx_dbm = recordSet.get(ordinalSmoothRx_dbm);
		Record recordDiffRx_dbm = recordSet.get(ordinalDiffRx_dbm);
		Record recordLapsRx_dbm = recordSet.get(ordinalLabsRx_dbm);
		Record recordDistanceStart = recordSet.get(ordinalSourceDist);
		Record recordDiffDistance = recordSet.get(ordinalDiffDist);
		Record recordLapsDistance = recordSet.get(ordinalLapsDistance);
		// adjustable variables
		int absorptionLevel = 70;
		long filterStartTime = 0;// wait 15 seconds before starting lab counting
		long filterMaxTime = 300000;// 300 seconds = 5 min window for lab counting
		long filterLapMinTime_ms = 5000; // 5 seconds time minimum time space between laps
		int filterMinDeltaRxDbm = 3;
		int filterMinDeltaDist = 20;
		if (this.getMeasurementPropertyValue(channelNumber, ordinalLabsRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0) {
			try {
				absorptionLevel = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalSmoothRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterStartTime = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMaxTime = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalSourceRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterLapMinTime_ms = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalLabsRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMinDeltaRxDbm = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffRx_dbm, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMinDeltaDist = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffDist, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
		}
		if (recordSourceRx_dbm != null && recordSmoothRx_dbm != null && recordDiffRx_dbm != null && recordLapsRx_dbm != null) {
			// temporary variables
			double lastLapTimeStamp_ms = 0;
			int lapTime = 0;
			int lastRxDbmValue = 0;
			int lapCount = 0;
			int lastRxdbm = 0;
			boolean isLapEvent = false;
			int localRxDbmMin = 0;

			// prepare smoothed Rx dbm
			for (int i = 0; i < recordSourceRx_dbm.realSize(); ++i) {
				if (recordSourceRx_dbm.get(i) == 0)
					recordSmoothRx_dbm.set(i, lastRxdbm);
				else
					recordSmoothRx_dbm.set(i, (lastRxdbm * absorptionLevel + recordSourceRx_dbm.get(i)) / (absorptionLevel + 1));
				lastRxdbm = recordSmoothRx_dbm.get(i);

			}
			// smooth and calculate differentiation
			CalculationThread thread = new LinearRegression(recordSet, recordSmoothRx_dbm.getName(), recordDiffRx_dbm.getName(), 2);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

			for (int i = 0; i < recordDiffRx_dbm.realSize(); ++i) {
				if (recordDiffRx_dbm.getTime_ms(i) > filterStartTime && recordDiffRx_dbm.getTime_ms(i) < (filterStartTime + filterMaxTime)) { //check start time before starting lab counting

					if ((recordDiffRx_dbm.getTime_ms(i) - lastLapTimeStamp_ms) > filterLapMinTime_ms) { // check minimal time between lap events

						if ((recordSmoothRx_dbm.get(i) / 1000 - localRxDbmMin) > filterMinDeltaRxDbm) { // check minimal Rx dbm difference

							if (lastRxDbmValue > 0 && recordDiffRx_dbm.get(i) <= 0) { // lap event detected
								isLapEvent = true;
								if (lastLapTimeStamp_ms != 0) {
									log.log(Level.FINE, String.format("Lap time in sec %03.1f", (recordSet.getTime_ms(i) - lastLapTimeStamp_ms) / 1000.0)); //$NON-NLS-1$
									lapTime = (int) (recordSet.getTime_ms(i) - lastLapTimeStamp_ms);
								}
								lastLapTimeStamp_ms = recordSet.getTime_ms(i);
								recordLapsRx_dbm.set(i, lapTime);
								if (lapTime != 0) {
									if (lapCount % 2 == 0) {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "\n%02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									} else {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "  -   %02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									}
								}
								if (isLapEvent && lapTime == 0) { // first lap start
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
								}

								localRxDbmMin = 0; // reset local min value of Rx dbm
							} // end lap event detected
							else if (lapTime == 0)
								if (isLapEvent)
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
								else
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
							else
								recordLapsRx_dbm.set(i, lapTime);
						} // end check minimal Rx dbm difference
						else if (lapTime == 0)
							if (isLapEvent)
								recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
							else
								recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
						else
							recordLapsRx_dbm.set(i, lapTime);
					} // end check minimal time between lap events
					else if (lapTime == 0) {
						if (isLapEvent)
							recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
						else
							recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
					}
					else {
						try {
							recordLapsRx_dbm.set(i, lapTime);
						}
						catch (ArrayIndexOutOfBoundsException e) {
							log.log(Level.SEVERE, String.format("index %d out of range in recordLapsRx_dbm with length %d", i, recordLapsRx_dbm.realSize()));
						}
					}

					// find a local minimal value of Rx dbm
					if (lastRxDbmValue < 0 && recordDiffRx_dbm.get(i) >= 0) { // local minimum Rx dbm detected
						if (recordSmoothRx_dbm.get(i) / 1000 < localRxDbmMin) localRxDbmMin = recordSmoothRx_dbm.get(i) / 1000;
					}
				} // end check start time before starting lab counting
				else if (recordDiffRx_dbm.getTime_ms(i) > (filterStartTime + filterMaxTime))
					recordLapsRx_dbm.set(i, 0);
				else
					recordLapsRx_dbm.set(i, lapTime);

				lastRxDbmValue = recordDiffRx_dbm.get(i);
			}
			// labs calculation end
		}
		if (recordDistanceStart != null && recordDistanceStart.hasReasonableData() && recordDiffDistance != null && recordLapsDistance != null) {
			// temporary variables
			double lastLapTimeStamp_ms = 0;
			int lapTime = 0;
			int lastDistanceValue = 0;
			int lapCount = 0;
			boolean isLapEvent = false;
			int localDistMax = 0;

			// smooth and calculate differentiation
			CalculationThread thread = new LinearRegression(recordSet, recordDistanceStart.getName(), recordDiffDistance.getName(), 4);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

			for (int i = 0; i < recordDiffDistance.realSize(); ++i) {
				if (recordDiffDistance.getTime_ms(i) > filterStartTime && recordDiffDistance.getTime_ms(i) < (filterStartTime + filterMaxTime)) { //check start time before starting lab counting

					if ((recordDiffDistance.getTime_ms(i) - lastLapTimeStamp_ms) > filterLapMinTime_ms) { // check minimal time between lap events

						if ((localDistMax - recordDistanceStart.get(i) / 1000) > filterMinDeltaDist) { // check minimal distance difference

							if (lastDistanceValue < 0 && recordDiffDistance.get(i) >= 0) { // lap event detected
								isLapEvent = true;
								if (lastLapTimeStamp_ms != 0) {
									log.log(Level.FINE, String.format("Lap time in sec %03.1f", (recordSet.getTime_ms(i) - lastLapTimeStamp_ms) / 1000.0)); //$NON-NLS-1$
									lapTime = (int) (recordSet.getTime_ms(i) - lastLapTimeStamp_ms);
								}
								lastLapTimeStamp_ms = recordSet.getTime_ms(i);
								recordLapsDistance.set(i, lapTime);
								if (lapTime != 0) {
									if (lapCount % 2 == 0) {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "\n%02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									} else {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "  -   %02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									}
								}
								if (isLapEvent && lapTime == 0) // first lap start
									recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);

								localDistMax = 0; // reset local distance maximum
							} // end lap event detected
							else if (lapTime == 0)
								if (isLapEvent)
									recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
								else
									recordLapsDistance.set(i, (int) filterLapMinTime_ms);
							else
								recordLapsDistance.set(i, lapTime);
						} // end check minimal distance difference
						else if (lapTime == 0)
							if (isLapEvent)
								recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
							else
								recordLapsDistance.set(i, (int) filterLapMinTime_ms);
						else
							recordLapsDistance.set(i, lapTime);
					} // end check minimal time between lap events
					else if (lapTime == 0)
						if (isLapEvent)
							recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
						else
							recordLapsDistance.set(i, (int) filterLapMinTime_ms);
					else
						recordLapsDistance.set(i, lapTime);

					// find local distance maximum
					if (lastDistanceValue > 0 && recordDiffDistance.get(i) <= 0) { // local maximum distance detected
						if (recordDistanceStart.get(i) / 1000 > localDistMax) localDistMax = recordDistanceStart.get(i) / 1000;
					}
				} // end check start time before starting lab counting
				else if (recordDiffDistance.getTime_ms(i) > (filterStartTime + filterMaxTime))
					recordLapsDistance.set(i, 0);
				else
					recordLapsDistance.set(i, lapTime);

				lastDistanceValue = recordDiffDistance.get(i);
			}
		}
		recordSet.setSaved(true); // adding description will set unsaved reason
	}

	public PickerParameters getPickerParameters() {
		return this.pickerParameters;
	}

	/**
	 * @param sensorSignature is a csv list of valid sensor values (i.e. sensor names)
	 * @return an integer value with bits 0 to 31 representing the sensor type ordinal numbers (true if the sensor type is active)
	 */
	@Override
	public BitSet getActiveSensors(String sensorSignature) {
		EnumSet<Sensor> sensors = Sensor.getSetFromSignature(sensorSignature);
		return Sensor.getSensors(sensors);
	}
	
	/**
	 * get the measurement ordinal of altitude, speed and trip length
	 * @return empty integer array if device does not fulfill complete requirement
	 */
	@Override
	public int[] getAtlitudeTripSpeedOrdinals() { 
		switch (this.application.getActiveChannelNumber()) {
		case 3: //GPS = 3; 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			return new int[] { 3, 9, 6 };
		default:
			return new int[0];
		}
	}  
	
	/**
	 * check and adapt stored measurement specialties properties against actual record set records which gets created by device properties XML
	 * - like GPS type dependent properties
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 */
	@Override
	public void applyMeasurementSpecialties(String[] fileRecordsProperties, RecordSet recordSet) {
		
		if (recordSet.getChannelConfigNumber() == 2 && recordSet.getChannelConfigName().equals("Vario")) {
			for (int i = 8; i < recordSet.size()-1; ++i) {
				Record record = recordSet.get(i);
				if (record != null && !record.getName().startsWith("vari")) {
					if (fileRecordsProperties[i].contains("factor_DOUBLE=")) {
						int startIndex = fileRecordsProperties[i].indexOf("factor_DOUBLE=") + "factor_DOUBLE=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - factor_DOUBLE " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.setFactor(Double.parseDouble(fileRecordsProperties[i].substring(startIndex, endIndex)));
					}
					if (fileRecordsProperties[i].contains("scale_sync_ref_ordinal_INTEGER=")) {
						int startIndex = fileRecordsProperties[i].indexOf("scale_sync_ref_ordinal_INTEGER=") + "scale_sync_ref_ordinal_INTEGER=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - scale_sync_ref_ordinal_INTEGER " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, Integer.parseInt(fileRecordsProperties[i].substring(startIndex, endIndex))); //$NON-NLS-1$
					}
				}
			} 
		}
		else if (recordSet.getChannelConfigNumber() == 3 && recordSet.getChannelConfigName().equals("GPS")) {
			for (int i = 19; i < recordSet.size()-1; ++i) {
				Record record = recordSet.get(i);
				if (record != null && !record.getName().startsWith("vari")) {
					if (fileRecordsProperties[i].contains("factor_DOUBLE=")) {
						int startIndex = fileRecordsProperties[i].indexOf("factor_DOUBLE=") + "factor_DOUBLE=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - factor_DOUBLE " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.setFactor(Double.parseDouble(fileRecordsProperties[i].substring(startIndex, endIndex)));
					}
					if (fileRecordsProperties[i].contains("scale_sync_ref_ordinal_INTEGER=")) {
						int startIndex = fileRecordsProperties[i].indexOf("scale_sync_ref_ordinal_INTEGER=") + "scale_sync_ref_ordinal_INTEGER=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - scale_sync_ref_ordinal_INTEGER " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, Integer.parseInt(fileRecordsProperties[i].substring(startIndex, endIndex))); //$NON-NLS-1$
					}
				}
			} 
		}
		else if (recordSet.getChannelConfigNumber() == 7 && recordSet.getChannelConfigName().equals("ESC")) {
			for (int i = 14; i < recordSet.size()-1; ++i) {
				Record record = recordSet.get(i);
				if (record != null && !record.getName().startsWith("vari")) {
					if (fileRecordsProperties[i].contains("factor_DOUBLE=")) {
						int startIndex = fileRecordsProperties[i].indexOf("factor_DOUBLE=") + "factor_DOUBLE=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - factor_DOUBLE " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.setFactor(Double.parseDouble(fileRecordsProperties[i].substring(startIndex, endIndex)));
					}
					if (fileRecordsProperties[i].contains("scale_sync_ref_ordinal_INTEGER=")) {
						int startIndex = fileRecordsProperties[i].indexOf("scale_sync_ref_ordinal_INTEGER=") + "scale_sync_ref_ordinal_INTEGER=".length();
						int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - scale_sync_ref_ordinal_INTEGER " + fileRecordsProperties[i].substring(startIndex, endIndex));
						record.createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, Integer.parseInt(fileRecordsProperties[i].substring(startIndex, endIndex))); //$NON-NLS-1$
					}
				}
			} 
		}
		return;
	}
	
	/**
	 * update the record set Vario dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter
	 * @param tmpRecordSet the record set to be updated
	 */
	protected static void updateVarioTypeDependent(int version, IDevice device, RecordSet tmpRecordSet) {
		if (version > 100 && version < 120) { //SM MicroVario
			//8=accX 9=accY 10=accZ 11=air-speed 12=version
			tmpRecordSet.get(8).setName(device.getMeasurementReplacement("acceleration") + " X");
			tmpRecordSet.get(8).setUnit("g");
			tmpRecordSet.get(9).setName(device.getMeasurementReplacement("acceleration") + " Y");
			tmpRecordSet.get(9).setUnit("g");
			tmpRecordSet.get(9).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 8); //$NON-NLS-1$
			tmpRecordSet.get(10).setName(device.getMeasurementReplacement("acceleration") + " Z");
			tmpRecordSet.get(10).setUnit("g");
			tmpRecordSet.get(10).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 8); //$NON-NLS-1$
			tmpRecordSet.get(11).setName(device.getMeasurementReplacement("air_speed"));
			tmpRecordSet.get(11).setUnit("km/h");
			tmpRecordSet.get(11).setFactor(2.0);
			tmpRecordSet.get(12).setName("Version");
			tmpRecordSet.get(12).setUnit("");
		}
	}

	/**
	 * update the record set GPS dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter
	 * @param tmpRecordSet the record set to be updated
	 * @param startTimeStamp_ms to be used for synchronization of other contained record sets
	 * @return
	 */
	protected static long updateGpsTypeDependent(int version, IDevice device, RecordSet tmpRecordSet, long startTimeStamp_ms) {
		if (version > 100) { //SM GPS-Logger
			//15=HomeDirection 16=ServoPulse 17=AirSpeed 18=n/a 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version		
			tmpRecordSet.get(16).setName(device.getMeasurementReplacement("servo_impulse"));
			tmpRecordSet.get(16).setUnit("%");
			tmpRecordSet.get(17).setName(device.getMeasurementReplacement("air_speed"));
			tmpRecordSet.get(17).setUnit("km/h");
			tmpRecordSet.get(17).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("acceleration") + " X");
			tmpRecordSet.get(19).setUnit("g");
			tmpRecordSet.get(19).setFactor(0.01);
			tmpRecordSet.get(20).setName(device.getMeasurementReplacement("acceleration") + " Y");
			tmpRecordSet.get(20).setUnit("g");
			tmpRecordSet.get(20).setFactor(0.01);
			tmpRecordSet.get(20).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 19); //$NON-NLS-1$
			tmpRecordSet.get(21).setName(device.getMeasurementReplacement("acceleration") + " Z");
			tmpRecordSet.get(21).setUnit("g");
			tmpRecordSet.get(21).setFactor(0.01);
			tmpRecordSet.get(21).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 19); //$NON-NLS-1$
			tmpRecordSet.get(22).setName("ENL");
			tmpRecordSet.get(22).setUnit("");
		}
		else if (version == 4) { //RC Electronics Sparrow
			tmpRecordSet.get(16).setName(device.getMeasurementReplacement("servo_impulse") + " GPS");
			tmpRecordSet.get(16).setUnit("%");
			tmpRecordSet.get(18).setName(device.getMeasurementReplacement("voltage") + " GPS");
			tmpRecordSet.get(18).setUnit("V");
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("time") + " GPS");//214:74:83.647
			tmpRecordSet.get(19).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(19).setFactor(1.0);
			tmpRecordSet.get(20).setName(device.getMeasurementReplacement("date") + " GPS");
			tmpRecordSet.get(20).setUnit("yy-MM-dd");
			tmpRecordSet.get(20).setFactor(1.0);
			tmpRecordSet.get(21).setName(device.getMeasurementReplacement("altitude") + " MSL");
			tmpRecordSet.get(21).setUnit("m");
			tmpRecordSet.get(21).setFactor(1.0);
			tmpRecordSet.get(22).setName("ENL");
			tmpRecordSet.get(22).setUnit("%");
			if (startTimeStamp_ms >= 0) {
				startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(19).lastElement(), 0);
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					recordSet.setStartTimeStamp(startTimeStamp_ms);
				} 
			}
		}
		else if (version == 1) { //Graupner GPS #1= 33602/S8437,
			tmpRecordSet.get(16).setName("velNorth");
			tmpRecordSet.get(16).setUnit("mm/s");
			tmpRecordSet.get(18).setName("speedAcc");
			tmpRecordSet.get(18).setUnit("cm/s");
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("time") + " GPS");
			tmpRecordSet.get(19).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(19).setFactor(1.0);
//		tmpRecordSet.get(20).setName("GPS ss.SSS");
//		tmpRecordSet.get(20).setUnit("ss.SSS");
//		tmpRecordSet.get(20).setFactor(1.0);
			tmpRecordSet.get(21).setName("velEast");
			tmpRecordSet.get(21).setUnit("mm/s");
			tmpRecordSet.get(21).setFactor(1.0);
			tmpRecordSet.get(22).setName("HDOP");
			tmpRecordSet.get(22).setUnit("dm");
			if (startTimeStamp_ms >= 0) {
				startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(19).lastElement(), 0);
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					recordSet.setStartTimeStamp(startTimeStamp_ms);
				} 
			}
		}
		else if (version == 0) { //Graupner GPS #0=GPS #33600
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("time") + " GPS");
			tmpRecordSet.get(19).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(19).setFactor(1.0);
//		tmpRecordSet.get(20).setName("GPS ss.SSS");
//		tmpRecordSet.get(20).setUnit("ss.SSS");
//		tmpRecordSet.get(20).setFactor(1.0);
			tmpRecordSet.get(21).setName(device.getMeasurementReplacement("altitude") + " MSL");
			tmpRecordSet.get(21).setUnit("m");
			tmpRecordSet.get(21).setFactor(1.0);
			if (startTimeStamp_ms >= 0) {
				startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(19).lastElement(), 0);
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					recordSet.setStartTimeStamp(startTimeStamp_ms);
				} 
			}
		}
		else {
			tmpRecordSet.get(16).setName("Byte GPS_1");
			tmpRecordSet.get(17).setName("Byte GPS_2");
			tmpRecordSet.get(18).setName("Byte GPS_3");

			tmpRecordSet.get(19).setName("Short GPS_4");
			tmpRecordSet.get(20).setName("Short GPS_5");
			tmpRecordSet.get(21).setName("Short GPS_6");

			tmpRecordSet.get(22).setName("Byte GPS_7");
		}
		return startTimeStamp_ms;
	}

	/**
	 * update the record set ESC dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter
	 * @param tmpRecordSet the record set to be updated
	 */
	protected static void updateEscTypeDependent(int version, IDevice device, RecordSet tmpRecordSet) {
		if (version == 3) {
			// 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 
			// 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc_ESC_15 29=VersionESC
			tmpRecordSet.get(14).setName(device.getMeasurementReplacement("speed"));
			tmpRecordSet.get(14).setUnit("km/h");
			device.getMeasurement(7, 14).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(15).setName(device.getMeasurementReplacement("speed") + " max");
			tmpRecordSet.get(15).setUnit("km/h");
			tmpRecordSet.get(16).setName("PWM");
			tmpRecordSet.get(16).setUnit("%");
			tmpRecordSet.get(17).setName(device.getMeasurementReplacement("throttle"));
			tmpRecordSet.get(17).setUnit("%");
			tmpRecordSet.get(18).setName(device.getMeasurementReplacement("voltage_bec"));
			tmpRecordSet.get(18).setUnit("V");
			tmpRecordSet.get(18).setFactor(0.1);
			device.getMeasurement(7, 18).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("voltage_bec_min"));
			tmpRecordSet.get(19).setUnit("V");
			tmpRecordSet.get(19).setFactor(0.1);
			tmpRecordSet.get(19).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 18); //$NON-NLS-1$
			tmpRecordSet.get(20).setName(device.getMeasurementReplacement("current_bec"));
			tmpRecordSet.get(20).setUnit("A");
			tmpRecordSet.get(20).setFactor(0.1);
			device.getMeasurement(7, 20).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(21).setName(device.getMeasurementReplacement("temperature_bec"));
			tmpRecordSet.get(21).setUnit("°C");
			tmpRecordSet.get(21).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			device.getMeasurement(7, 21).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(22).setName(device.getMeasurementReplacement("temperature_capacitor"));
			tmpRecordSet.get(22).setUnit("°C");
			device.getMeasurement(7, 22).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(22).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(23).setName(device.getMeasurementReplacement("timing"));
			tmpRecordSet.get(23).setUnit("°");
			tmpRecordSet.get(24).setName(device.getMeasurementReplacement("temperature") + " 3");
			tmpRecordSet.get(24).setUnit("°C");
			device.getMeasurement(7, 24).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(24).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(25).setName(device.getMeasurementReplacement("gear"));
			tmpRecordSet.get(25).setUnit("");
			tmpRecordSet.get(26).setName("YGEGenExt");
			tmpRecordSet.get(26).setUnit("");
			tmpRecordSet.get(27).setName("MotStatEscNr");
			tmpRecordSet.get(27).setUnit("#");
		}
		else if (version >= 128) {
			//1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
			//8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
			tmpRecordSet.get(1).setName(device.getMeasurementReplacement("voltage") + " ECU");
			tmpRecordSet.get(2).setName(device.getMeasurementReplacement("current") + " ECU");
			tmpRecordSet.get(6).setName(device.getMeasurementReplacement("temperature") + " EGT 1");
			tmpRecordSet.get(7).setName(device.getMeasurementReplacement("temperature") + " EGT 2");
			tmpRecordSet.get(8).setName(device.getMeasurementReplacement("voltage") + " ECU_min");
			tmpRecordSet.get(9).setName(device.getMeasurementReplacement("current") + " ECU_max");
			tmpRecordSet.get(11).setName(device.getMeasurementReplacement("temperature") + " EGT 1_max");
			tmpRecordSet.get(12).setName(device.getMeasurementReplacement("temperature") + " EGT 2_max");
			
			//14=AirSpeed 15=AirSpeed_max 16=PWM 17=Throttle 18=VoltagePump 19=VoltagePump_min 20=Flow 21=Fuel 22=Power 
			//23=Thrust 24=TemperaturePump 25=EngineStat 26=spare 27=spare 28=spare 29=version
			tmpRecordSet.get(14).setName(device.getMeasurementReplacement("air_speed"));
			tmpRecordSet.get(14).setUnit("km/h");
			device.getMeasurement(7, 14).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(15).setName(device.getMeasurementReplacement("air_speed") + "_max");
			tmpRecordSet.get(15).setUnit("km/h");
			tmpRecordSet.get(16).setName("PWM");
			tmpRecordSet.get(16).setUnit("%");
			tmpRecordSet.get(17).setName(device.getMeasurementReplacement("throttle"));
			tmpRecordSet.get(17).setUnit("%");
			tmpRecordSet.get(18).setName(device.getMeasurementReplacement("voltage_pump"));
			tmpRecordSet.get(18).setUnit("V");
			tmpRecordSet.get(18).setFactor(0.1);
			device.getMeasurement(7, 18).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(19).setName(device.getMeasurementReplacement("voltage_pump_min"));
			tmpRecordSet.get(19).setUnit("V");
			tmpRecordSet.get(19).setFactor(0.1);
			tmpRecordSet.get(20).setName(device.getMeasurementReplacement("flow"));
			tmpRecordSet.get(20).setUnit("ml/min");
			tmpRecordSet.get(20).setFactor(0.1);
			device.getMeasurement(7, 20).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(21).setName(device.getMeasurementReplacement("fuel"));
			tmpRecordSet.get(21).setUnit("ml");
			//tmpRecordSet.get(21).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			device.getMeasurement(7, 21).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(22).setName(device.getMeasurementReplacement("power"));
			tmpRecordSet.get(22).setUnit("W");
			device.getMeasurement(7, 22).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			//tmpRecordSet.get(22).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(23).setName(device.getMeasurementReplacement("thrust"));
			tmpRecordSet.get(23).setUnit("N");
			tmpRecordSet.get(24).setName(device.getMeasurementReplacement("temperature_pump"));
			tmpRecordSet.get(24).setUnit("°C");
			device.getMeasurement(7, 24).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			//tmpRecordSet.get(24).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(25).setName(device.getMeasurementReplacement("engine"));
			tmpRecordSet.get(25).setUnit("#");
		}
	}

	/**
	 * query if the channel in use has dependency with each other to initiate file description synchronization 
	 * @return true for devices with one source of data distributed over channel record sets (HoTTAdapter, Av4ms_FV_762)
	 */
	public boolean useChannelWithSyncedDescription() { return true; }
}
