package gde.device.weatronic;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.StatisticsType;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

public class LogReader {
	final static String								$CLASS_NAME					= LogReader.class.getName();
	final static Logger								log									= Logger.getLogger(LogReader.$CLASS_NAME);

	final static DataExplorer					application					= DataExplorer.getInstance();
	final static Channels							channels						= Channels.getInstance();

	static long												startTimeStamp_ms;
	static int[]											points;
	static Vector<Integer>						pointsVector				= new Vector<Integer>();
	static RecordSet									recordSet;
	static WeatronicAdapter						device;
	static byte[]											buf_length					= new byte[4];
	static byte[]											buf_log_record_tmp;
	static byte[]											buf_log_record;
	static byte[]											buf_header;
	static byte[]											buf_data;
	static long												timeStep_ms;
	static Map<Integer, Measurement>	measurements				= new HashMap<Integer, Measurement>();
	static Vector<Integer>						usedMeasurementIds	= new Vector<Integer>();
	static Map<Integer, DataItem>			receivedDataItems		= new HashMap<Integer, DataItem>();
	static Vector<Integer>						unknownIds					= new Vector<Integer>();

	public LogReader(int activeChannelNumber) {
		LogReader.channels.setActiveChannelNumber(activeChannelNumber);
	}

	public enum RecordType {
		UNKNOWN, DATA, HEADER, EVENT;
	}

	public class Record {
		protected int	offset;

		public Record() {
			this.offset = 13;
		}
	}

	public long getTimeOffset(final int interval) {
		switch (interval) {
		//Smaller ID for shorter intervals
		case 0: // "1000Hz";
			return 1;
		case 1: // "500Hz";
			return 2;
		case 2: // "200Hz";
			return 5;
		case 3: // "100Hz";
			return 10;
		case 4: // "50Hz";
			return 20;
		case 5: // "20Hz";
			return 50;
		case 6: // "10Hz";
			return 100;
		case 7: // "5Hz";
			return 200;
		case 8: // "4Hz";
			return 250;
		case 9: // "2Hz";
			return 500;
		case 10: // "1Hz";
			return 1000;
		case 11: // "0.5Hz";
			return 2000;
		case 12: // "0.2Hz";
			return 2500;
		case 13: // "0.1Hz";
			return 10000;
		default:
			return 0;
		}
	}

	private boolean isChannelFilter(int measurementId) {
		return measurementId == 0xFFFF
				|| (measurementId & 0xFF00) == 0x1000
				|| (measurementId & 0xFF00) == 0x1100
				|| ((measurementId >= 0x1200 && measurementId <= 0x125E)
						&& (LogReader.measurements.get(measurementId) != null ? LogReader.measurements.get(measurementId).getName().contains("Function") : true)
				|| LogReader.measurements.get(measurementId).getName().contains("ControlID"));
	}

	private boolean isStatusFilter(int measurementId) {
		return measurementId == 0x0C0A
				|| measurementId == 0x2c09
				|| measurementId == 0x2c86
				|| measurementId == 0x2c8c
				|| measurementId == 0x0c1a
				|| measurementId == 0x0c2a
				|| measurementId == 0x2c19
				|| measurementId == 0x2c29;
	}

	private boolean isUTCFilter(Measurement measurement) {
		if (measurement == null || measurement.getName().contains("UTC")) return true;
		return false;
	}

	public enum DataType {
		SignedByte(43), UnsignedByte(52), SignedShort(44), UnsignedShort(53), SignedWord(45), UnsignedWord(54), Float(6), PaketGPS(20), PaketControlData(5), PaketServoData(4), PowerSupply(34), ControlIDPaket(
				17), SignedShort2(2), SignedShort3(3), UnknownSize(0);

		private final int	value;

		private DataType(int v) {
			this.value = v;
		}

		public int value() {
			return this.value;
		}

		public static DataType valueOf(int i) {
			for (DataType ds : DataType.values()) {
				if (ds.value == i) return ds;
			}
			return DataType.UnknownSize;
		}
	}

	public class Measurement {
		public static final double	DBL_EPSILON	= 2.220446049250313E-16d;
		String											name;
		int													id;
		DataType										dataType;
		double											factor;
		double											offset;
		String											unit;

		public Measurement(final byte[] buffer) {
			this.name = new String(buffer, 0, 64).trim();
			this.id = DataParser.parse2UnsignedShort(buffer, 64);
			this.dataType = DataType.valueOf(buffer[66]);
			byte[] bytes = new byte[8];
			System.arraycopy(buffer, 67, bytes, 0, 8);
			this.offset = DataParser.byte2Double(bytes, true)[0];
			System.arraycopy(buffer, 75, bytes, 0, 8);
			this.factor = DataParser.byte2Double(bytes, true)[0];
			this.factor = (this.factor - 0.0) < Measurement.DBL_EPSILON ? 1.0 : this.factor;
			this.unit = new String(buffer, 83, 8).trim();
			if (LogReader.log.isLoggable(Level.FINER))
				System.out.println(String.format(Locale.ENGLISH, "%s[%s] factor=%f offset=%f Id=%d type=%d;", this.name, this.unit, this.factor, this.offset, this.id, this.dataType.value));
		}

		public String getName() {
			return this.name;
		}

		public void setName(final String newName) {
			this.name = newName;
		}

		public int getId() {
			return this.id;
		}

		public DataType getDataType() {
			return this.dataType;
		}

		public int getDataSize() {
			return getSizeByType(this.dataType);
		}

		/**
		 * @param type
		 * @return the size in bytes to be used for a data type
		 */
		public int getSizeByType(DataType type) {
			switch (type) {
			case SignedByte: 			/* 8Bit signed */
				return 1;
			case UnsignedByte: 		/* 8Bit unsigned */
				return 1;
			case SignedShort: 		/* 16Bit signed */
				return 2;
			case UnsignedShort: 	/* 16Bit unsigned */
				return 2;
			case SignedWord: 			/* 32Bit signed */
				return 4;
			case UnsignedWord: 		/* 32Bit unsigned */
				return 4;
			case Float: 					/* single float according IEEE754 */
				return 6;
			case PaketGPS: 				/* 20 Bytes */
				return 20;
			case PaketControlData: /* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -100% ... +100% */
				return 24;
			case PaketServoData: 	/* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -200% ... +200% */
				return 24;
			case ControlIDPaket: 	/* 16 16Bit values */
				return 32;
			case PowerSupply: 		/* 64 Bytes */
				return 64;
			default:							//empty fields 2 bytes long
			case SignedShort2:
			case SignedShort3:
				return 2;
			}
		}

		public double getFactor() {
			return this.factor;
		}

		public double getOffset() {
			return this.offset;
		}

		public String getUnit() {
			return this.unit;
		}
	}

	public class RecordHeader extends Record {

		String	modellName;
		int			measurementCount;
		int 		realUsedMeasurementCount;

		public RecordHeader(final byte[] buffer) {
			super(); //constructor will set offset
			this.modellName = new String(buffer, 0 + this.offset, 256);
			this.measurementCount = DataParser.parse2UnsignedShort(buffer, 256 + this.offset);
			this.realUsedMeasurementCount = 0;

			int activeChannelConfigNumber = LogReader.application.getActiveChannelNumber();
			int ordinal = 0;
			int existingNumberMeasurements = LogReader.device.getDeviceConfiguration().getMeasurementNames(activeChannelConfigNumber).length;
			for (int i = this.measurementCount; i < existingNumberMeasurements; i++) {
				LogReader.device.removeMeasurementFromChannel(activeChannelConfigNumber, LogReader.device.getMeasurement(activeChannelConfigNumber, this.measurementCount));
			}
			if (this.measurementCount != existingNumberMeasurements)
				System.out.println();
//			existingNumberMeasurements = LogReader.device.getDeviceConfiguration().getMeasurementNames(activeChannelConfigNumber).length;
//			for (int i = 0; i < existingNumberMeasurements; i++) {
//				LogReader.device.getMeasurement(activeChannelConfigNumber, i).setName(i + GDE.STRING_DOLLAR);
//			}
			//device.getDeviceConfiguration().storeDeviceProperties();

			for (int i = 0; i < this.measurementCount; i++) {
				byte[] measurementBuffer = new byte[91];
				System.arraycopy(buffer, 258 + this.offset + i * measurementBuffer.length, measurementBuffer, 0, measurementBuffer.length);

				//due to special measurement blocks we need to add a second loop here
				DataType dataSize = DataType.valueOf(measurementBuffer[66]);
				final String measurementName = new String(measurementBuffer, 0, 64).trim();
				switch (dataSize) {
				case PaketGPS://GPS packet
					String[] packetGpsNames = { "_Latitude", "_Longitude", "_Speed", "_Altitude", "_Course", "_isValid", "_UTC" };
					String[] packetGpsUnits = { "°", "°", "kn", "m", "°", "-", "-" };
					double[] packetGpsFactors = { 1 / 6000000.0, 1 / 6000000.0, 1 / 10.0, 1 / 10.0, 1 / 100.0, 1.0, 1.0 };
					//"%.6f°;%.6f°;%.1fkn;%.1fm;%.2f°;%d;%d;", latitude / 6000000.0f, longitude / 6000000.0f, speed / 10.0f, altitude / 10.0f, course / 100.0f, isValid, timeStam_ms_utc));

					for (int j = 0; j < packetGpsNames.length; j++) {
						String packetGpsName = measurementName + packetGpsNames[j];
						byte[] tempMeasurementBuffer = new byte[64];
						System.arraycopy(packetGpsName.getBytes(), 0, tempMeasurementBuffer, 0, packetGpsName.getBytes().length);
						System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
						switch (j) {
						case 0://Latitude
							setupMeasurement(gde.data.Record.DataType.GPS_LATITUDE, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, true);
							break;
						case 1://Longitude
							setupMeasurement(gde.data.Record.DataType.GPS_LONGITUDE, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, true);
							break;
						case 2://Speed
							setupMeasurement(gde.data.Record.DataType.GPS_SPEED, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, false);
							break;
						case 3://Altitude
							setupMeasurement(gde.data.Record.DataType.GPS_ALTITUDE, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, false);
							break;
						case 4://Course
							setupMeasurement(gde.data.Record.DataType.GPS_AZIMUTH, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, false);
							break;
						case 5://isValid
							setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, true);
							break;
						case 6://UTC
							if (!WeatronicAdapter.isUtcFilter) //skip UTC since time is not really display able
								setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, packetGpsName, packetGpsUnits[j], true, packetGpsFactors[j], 0.0, true);
							break;
						default:
							break;
						}
					}
					Measurement measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);
					LogReader.usedMeasurementIds.add(measurement.getId());
					break;

				case PowerSupply://Power Supply packet
					String[] powerSupplyNames = { "_Status Main", "_Voltage Main", "_Current Main", "_InputVoltage Main", "_InputCurrent Main", "_MainVoltage Main", "_ReserveVoltage Main",
							"_Main_InputTemperature" };
					String[] powerSupplyUnits = { "-", "V", "A", "V", "A", "V", "V", "°C" };
					double[] powerSupplyFactors = { 1.0, 1 / 1000.0, 1 / 1000.0, 1 / 1000.0, 1 / 1000.0, 1 / 1000.0, 1 / 1000.0, 1.0 };
					//"%d;%.3fV;%.3fA;%.3fV;%.3fA;%.3fV;%.3fV;%d°C;", status, voltage / 1000.0f, current / 1000.0f, inVoltage / 1000.0f, inCurrent / 1000.0f, mainVoltage / 1000.0f, reserveVoltage / 1000.0f, inTemperature));
					for (int j = 0; j < powerSupplyNames.length; j++) {
						if (j == 0 && WeatronicAdapter.isStatusFilter) continue;

						String powerSupplyName = measurementName + powerSupplyNames[j];
						byte[] tempMeasurementBuffer = new byte[64];
						System.arraycopy(powerSupplyName.getBytes(), 0, tempMeasurementBuffer, 0, powerSupplyName.getBytes().length);
						System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
						setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, powerSupplyName, powerSupplyUnits[j], true, powerSupplyFactors[j], 0.0,
								(powerSupplyName.endsWith("Status") ? true : false));
					}
					String[] mainCellMeasurementNames = { "_Status Cell%d", "_Voltage Cell%d", "_Current Cell%d", "_Capacity Cell%d", "_Temperature Cell%d" };
					String[] mainCellMeasurementUnits = { "-", "V", "A", "mAh", "°C" };
					double[] mainCellMeasurementFactors = { 1.0, 1 / 1000.0, 1 / 1000.0, 1 / 1000.0, 1.0 };
					//"%d;%.3fV;%.3fA;%.3fAh;%d°C;", cell1_Status, cell1_Voltage / 1000.0f, cell1_Current / 1000.0f, cell1_Capacity / 1000.0f, cell1_Temperature));
					for (int cell = 1; cell <= 4; cell++) {
						for (int k = 0; k < mainCellMeasurementNames.length; k++) {
							if (k == 0 && WeatronicAdapter.isStatusFilter) continue;

							String cellmeasurementName = measurementName + String.format(mainCellMeasurementNames[k], cell);
							byte[] tempMeasurementBuffer = new byte[64];
							System.arraycopy(cellmeasurementName.getBytes(), 0, tempMeasurementBuffer, 0, cellmeasurementName.getBytes().length);
							System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
							setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, cellmeasurementName, mainCellMeasurementUnits[k], true, mainCellMeasurementFactors[k], 0.0,
									(cellmeasurementName.endsWith("Status") ? true : false));
						}
					}
					measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);
					LogReader.usedMeasurementIds.add(measurement.getId());
					break;

				default:
					measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);

					if ((WeatronicAdapter.isChannelFilter && isChannelFilter(measurement.getId())) || (WeatronicAdapter.isStatusFilter && isStatusFilter(measurement.getId()))
							|| (WeatronicAdapter.isUtcFilter && isUTCFilter(measurement))) break; //skip Tx_Control[%] Id=0x1000, Tx_Servo[%] Id=0x1080, Tx_Function Id=0x1200

					LogReader.usedMeasurementIds.add(measurement.getId());

					int index = 1;
					String tmpMeasurementName = measurement.getName();
					while (isDuplicatedName(this.realUsedMeasurementCount, activeChannelConfigNumber, measurement.getName()))
						measurement.setName(tmpMeasurementName + GDE.STRING_UNDER_BAR + index++);

					if (measurement.getName().contains("_GPS_")) {
						if (measurement.getName().contains("_GPS_Long"))
							setupMeasurement(gde.data.Record.DataType.GPS_LONGITUDE, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("_GPS_Lat"))
							setupMeasurement(gde.data.Record.DataType.GPS_LATITUDE, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("_GPS_Alt"))
							setupMeasurement(gde.data.Record.DataType.GPS_ALTITUDE, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), false, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("_GPS_Course"))
							setupMeasurement(gde.data.Record.DataType.GPS_AZIMUTH, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), false, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("_GPS_Speed"))
							setupMeasurement(gde.data.Record.DataType.GPS_SPEED, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), false, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("_GPS_IsValid"))
							setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("UTC"))
							setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
									measurement.getOffset(), true);
						else if (measurement.getName().contains("Status"))
							setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
									measurement.getOffset(), true);
						else
							setupMeasurement(activeChannelConfigNumber, ordinal++, measurement, false);
					}
					else if (measurement.getName().contains("UTC"))
						setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
								measurement.getOffset(), true);
					else if (measurement.getName().contains("Status"))
						setupMeasurement(gde.data.Record.DataType.DEFAULT, activeChannelConfigNumber, ordinal++, measurement.getName(), measurement.getUnit(), true, measurement.getFactor(),
								measurement.getOffset(), true);
					else
						setupMeasurement(activeChannelConfigNumber, ordinal++, measurement, false);
					break;
				}
			}
			if (this.realUsedMeasurementCount != existingNumberMeasurements) {
				for (int i = this.realUsedMeasurementCount; i < existingNumberMeasurements; i++) {
					LogReader.device.removeMeasurementFromChannel(activeChannelConfigNumber, LogReader.device.getMeasurement(activeChannelConfigNumber, this.realUsedMeasurementCount));
				}
			}
//		existingNumberMeasurements = LogReader.device.getDeviceConfiguration().getMeasurementNames(activeChannelConfigNumber).length;
//		for (int i = 0; i < existingNumberMeasurements; i++) {
//			LogReader.device.getMeasurement(activeChannelConfigNumber, i).setName(i + GDE.STRING_DOLLAR);
//		}
			LogReader.device.getDeviceConfiguration().storeDeviceProperties();

			//build up the record set with variable number of records just fit the sensor data
			String[] recordNames = LogReader.device.getDeviceConfiguration().getMeasurementNames(activeChannelConfigNumber);
			String[] recordSymbols = new String[recordNames.length];
			String[] recordUnits = new String[recordNames.length];
			for (int i = 0; i < recordNames.length; i++) {
				MeasurementType measurement = LogReader.device.getMeasurement(activeChannelConfigNumber, i);
				recordSymbols[i] = GDE.STRING_EMPTY; //measurement.getSymbol();
				recordUnits[i] = measurement.getUnit();
			}
			String recordSetNameExtend = LogReader.device.getRecordSetStateNameReplacement(1); // state name
			String recordSetName = (LogReader.channels.getActiveChannel().size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
			LogReader.recordSet = RecordSet.createRecordSet(recordSetName, LogReader.device, activeChannelConfigNumber, recordNames, recordSymbols, recordUnits, LogReader.device.getTimeStep_ms(), true, true, true);
			LogReader.recordSet.getName(); // cut/correct length of recordSetName
			for (int i = 0; i < LogReader.recordSet.size(); i++) {
				gde.data.Record record = LogReader.recordSet.get(i);
				MeasurementType measurementType = LogReader.device.getMeasurement(activeChannelConfigNumber, record.getOrdinal());
				if (measurementType == null || measurementType.getProperty(gde.data.Record.DataType.DEFAULT.value()) != null) continue;
				record.setDataType();
			}
			//LogReader.recordSet.realSize();
		}

		private boolean isDuplicatedName(int ordinal, int channelConfigNumber, String name) {
			String[] measurementNames = LogReader.device.getMeasurementNamesReplacements(channelConfigNumber);
			for (int i = 0; i < ordinal; i++) {
				if (measurementNames[i].equals(name))
					return true;
			}
			return false;
		}

		private void setupMeasurement(final gde.data.Record.DataType dataType, final int channelConfig, final int measurementOrdinal, String name, String unit, boolean isActive, double factor,
				double offset, boolean isClearStatistics) {
			++this.realUsedMeasurementCount;
			MeasurementType gdeMeasurement = LogReader.device.getMeasurement(channelConfig, measurementOrdinal);
			if (!name.equals(gdeMeasurement.getName())) {
				gdeMeasurement.setName(name);
				gdeMeasurement.setStatistics(null);//delete statistics with trigger, ....
			}
			gdeMeasurement.removeProperties();
			gdeMeasurement.setUnit(unit);
			gdeMeasurement.setActive(isActive);
			gdeMeasurement.setFactor(factor);
			gdeMeasurement.setOffset(offset);

			switch (dataType) {
			case GPS_LATITUDE:
				PropertyType tmpPropertyType = new PropertyType();
				tmpPropertyType.setName(gde.data.Record.DataType.GPS_LATITUDE.value());
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(gde.data.Record.DataType.GPS_LATITUDE.value());
				gdeMeasurement.getProperty().add(tmpPropertyType);
				break;
			case GPS_LONGITUDE:
				tmpPropertyType = new PropertyType();
				tmpPropertyType.setName(gde.data.Record.DataType.GPS_LONGITUDE.value());
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(gde.data.Record.DataType.GPS_LONGITUDE.value());
				gdeMeasurement.getProperty().add(tmpPropertyType);
				break;
			case GPS_ALTITUDE:
				tmpPropertyType = new PropertyType();
				tmpPropertyType.setName(gde.data.Record.DataType.GPS_ALTITUDE.value());
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(gde.data.Record.DataType.GPS_ALTITUDE.value());
				gdeMeasurement.getProperty().add(tmpPropertyType);
				break;
			case GPS_AZIMUTH:
				tmpPropertyType = new PropertyType();
				tmpPropertyType.setName(gde.data.Record.DataType.GPS_AZIMUTH.value());
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(gde.data.Record.DataType.GPS_AZIMUTH.value());
				gdeMeasurement.getProperty().add(tmpPropertyType);
				break;
			case GPS_SPEED:
				tmpPropertyType = new PropertyType();
				tmpPropertyType.setName(gde.data.Record.DataType.GPS_SPEED.value());
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(gde.data.Record.DataType.GPS_SPEED.value());
				gdeMeasurement.getProperty().add(tmpPropertyType);
				break;

			default:
				break;
			}

			if (WeatronicAdapter.properties.get(name) != null) { //scale_sync_ref_ordinal
				String[] measurementNames = LogReader.device.getMeasurementNamesReplacements(channelConfig);
				int syncOrdinal = -1;
				String syncName = (String) WeatronicAdapter.properties.get(name);
				for (int i = 0; i < measurementNames.length; i++) {
					if (measurementNames[i].equals(syncName)) {
						syncOrdinal = i;
						break;
					}
				}

				if (syncOrdinal >= 0) {
					PropertyType tmpPropertyType = new PropertyType();
					tmpPropertyType.setName(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
					tmpPropertyType.setType(DataTypes.INTEGER);
					tmpPropertyType.setValue(syncOrdinal);
					gdeMeasurement.getProperty().add(tmpPropertyType);
				}
			}

			if (isClearStatistics) {
				gdeMeasurement.setStatistics(null);
			}
			else {
				StatisticsType newStatisticsType = gdeMeasurement.getStatistics();
				if (newStatisticsType == null) {
					newStatisticsType = new StatisticsType();
				}
				newStatisticsType.setMin(true);
				newStatisticsType.setMax(true);
				newStatisticsType.setAvg(true);
				newStatisticsType.setSigma(true);
				gdeMeasurement.setStatistics(newStatisticsType);
			}
		}

		private void setupMeasurement(final int channelConfig, final int measurementOrdinal, Measurement measurement,
				boolean isClearStatistics) {
			++this.realUsedMeasurementCount;
			MeasurementType gdeMeasurement = LogReader.device.getMeasurement(channelConfig, measurementOrdinal);
			if (!measurement.getName().equals(gdeMeasurement.getName())) {
				gdeMeasurement.setName(measurement.getName().length() == 0 ? ("???_" + measurementOrdinal) : measurement.getName());
				gdeMeasurement.setStatistics(null);//delete statistics with trigger, ....
			}
			gdeMeasurement.removeProperties();
			gdeMeasurement.setUnit(measurement.getUnit());
			gdeMeasurement.setActive(true);
			gdeMeasurement.setFactor(measurement.getFactor());
			gdeMeasurement.setOffset(measurement.getOffset());

			if (WeatronicAdapter.properties.get(measurement.getName()) != null) { //scale_sync_ref_ordinal
				String[] measurementNames = LogReader.device.getMeasurementNamesReplacements(channelConfig);
				int syncOrdinal = -1;
				String syncName = (String) WeatronicAdapter.properties.get(measurement.getName());
				for (int i = 0; i < measurementNames.length; i++) {
					if (measurementNames[i].equals(syncName)) {
						syncOrdinal = i;
						break;
					}
				}

				if (syncOrdinal >= 0) {
					PropertyType tmpPropertyType = new PropertyType();
					tmpPropertyType.setName(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
					tmpPropertyType.setType(DataTypes.INTEGER);
					tmpPropertyType.setValue(syncOrdinal);
					gdeMeasurement.getProperty().add(tmpPropertyType);
				}
			}


			if (isClearStatistics) {
				gdeMeasurement.setStatistics(null);
			}
			else {
				StatisticsType newStatisticsType = gdeMeasurement.getStatistics();
				if (newStatisticsType == null) {
					newStatisticsType = new StatisticsType();
				}
				newStatisticsType.setMin(true);
				newStatisticsType.setMax(true);
				newStatisticsType.setAvg(true);
				newStatisticsType.setSigma(true);
				gdeMeasurement.setStatistics(newStatisticsType);
			}
		}

		public String getModellName() {
			return this.modellName;
		}

		public int getMeasurementCount() {
			return this.measurementCount > LogReader.measurements.size() ? this.measurementCount : LogReader.measurements.size();
		}

		public Map<Integer, Measurement> getMeasurements() {
			return LogReader.measurements;
		}
	}

	public class ConfigSection {
		int							count;
		int							interval;
		Vector<Integer>	ids					= new Vector<Integer>();
		int							lengthBytes	= 0;

		public ConfigSection(final byte[] buffer, final int offset) {
			this.count = DataParser.parse2UnsignedShort(buffer, 0 + offset);
			if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("config sections %3d, ", this.count));
			this.interval = (buffer[2 + offset] & 0xFF);
			if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("interval %3d, IDs ", this.interval));
			this.lengthBytes = 3 + this.count * 2;
			for (int i = 0; i < this.count; i++) {
				this.ids.add(DataParser.parse2UnsignedShort(buffer, 3 + offset + i * 2));
			}
			if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, this.ids.toString());
		}

		public int getId(final int index) {
			return this.ids.get(index);
		}

		public Vector<Integer> getIds() {
			return this.ids;
		}

		public int getCount() {
			return this.count;
		}

		public int getInterval() {
			return this.interval;
		}

		public int getLength() {
			return this.lengthBytes;
		}
	}

	public class DataItem {
		final Measurement	measurement;
		final DataType		dataType;
		int								value;

		int								longitude;
		int								latitude;
		int								speed;
		int								altitude;
		int								course;
		int								isValid;
		long							timeStamp_ms_utc;

		int								status;
		int								voltage;
		int								current;
		int								inVoltage;
		int								inCurrent;
		int								mainVoltage;
		int								reserveVoltage;
		int								inTemperature;

		int								cell1_Status;
		int								cell1_Voltage;
		int								cell1_Current;
		int								cell1_Capacity;
		int								cell1_Temperature;
		int								cell2_Status;
		int								cell2_Voltage;
		int								cell2_Current;
		int								cell2_Capacity;
		int								cell2_Temperature;
		int								cell3_Status;
		int								cell3_Voltage;
		int								cell3_Current;
		int								cell3_Capacity;
		int								cell3_Temperature;
		int								cell4_Status;
		int								cell4_Voltage;
		int								cell4_Current;
		int								cell4_Capacity;
		int								cell4_Temperature;

		long							dataSectionTimeStamp_ms;

		public DataItem(final byte[] buffer, final int offset, final Measurement measurement, final long dataSectionTimeStemp) {
			this.measurement = measurement;
			this.dataType = measurement.getDataType();
			this.dataSectionTimeStamp_ms = dataSectionTimeStemp;
			switch (this.dataType) {
			case SignedByte: /* 8Bit signed */
				this.value = buffer[offset];
				break;
			case UnsignedByte: /* 8Bit unsigned */
				this.value = buffer[offset] & 0xFF;
				break;
			case SignedShort: /* 16Bit signed */
				this.value = DataParser.parse2Short(buffer, offset);
				break;
			case UnsignedShort: /* 16Bit unsigned */
				this.value = DataParser.parse2UnsignedShort(buffer, offset);
				break;
			case SignedWord: /* 32Bit signed */
				this.value = DataParser.parse2Int(buffer, offset);
				break;
			case UnsignedWord: /* 32Bit unsigned */
				this.value = (int) DataParser.getUInt32(buffer, offset);
				break;
			case Float: /* single float according IEEE754 */
				LogReader.log.log(Level.WARNING, "Float data item received, can not handle");
				break;
			case PaketGPS: /* 20 Bytes */
				//"4_Latitude","4_Longitude","2_Speed","2_Altitude","2_Course","1_isValid","4+1_UTC"
				this.latitude = DataParser.parse2Int(buffer, offset);
				this.longitude = DataParser.parse2Int(buffer, 4 + offset);
				this.speed = DataParser.parse2Short(buffer, 8 + offset);
				this.altitude = DataParser.parse2Short(buffer, 10 + offset);
				this.course = DataParser.parse2UnsignedShort(buffer, 12 + offset);
				this.isValid = (buffer[14 + offset] & 0x01);
				this.timeStamp_ms_utc = DataParser.getUInt32(buffer, 15 + offset) + (buffer[19] & 0xFF);
				break;
			case PaketControlData: /* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -100% ... +100% */
				//24 bytes
				break;
			case PaketServoData: /* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -200% ... +200% */
				//24 bytes
				break;
			case ControlIDPaket: /* 16 16Bit values */
				//32 bytes
				break;
			case PowerSupply: /* 64 Bytes */
				//"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"
				this.status = DataParser.parse2Int(buffer, offset); //2560033
				this.voltage = DataParser.parse2Short(buffer, 4 + offset); //3.744
				this.current = DataParser.parse2Short(buffer, 6 + offset);
				this.inVoltage = DataParser.parse2Short(buffer, 8 + offset);
				this.inCurrent = DataParser.parse2Short(buffer, 10 + offset);
				this.mainVoltage = DataParser.parse2Short(buffer, 12 + offset);
				this.reserveVoltage = DataParser.parse2Short(buffer, 14 + offset);
				this.inTemperature = DataParser.parse2Short(buffer, 16 + offset);

				//"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"
				this.cell1_Status = DataParser.parse2Int(buffer, 20 + offset); //1110272
				this.cell1_Voltage = DataParser.parse2Short(buffer, 24 + offset);
				this.cell1_Current = DataParser.parse2Short(buffer, 26 + offset);
				this.cell1_Capacity = DataParser.parse2Short(buffer, 28 + offset);
				this.cell1_Temperature = buffer[30 + offset];

				this.cell2_Status = DataParser.parse2Int(buffer, 31 + offset);
				this.cell2_Voltage = DataParser.parse2Short(buffer, 35 + offset);
				this.cell2_Current = DataParser.parse2Short(buffer, 37 + offset);
				this.cell2_Capacity = DataParser.parse2Short(buffer, 39 + offset);
				this.cell2_Temperature = buffer[41 + offset];

				this.cell3_Status = DataParser.parse2Int(buffer, 42 + offset);
				this.cell3_Voltage = DataParser.parse2Short(buffer, 46 + offset);
				this.cell3_Current = DataParser.parse2Short(buffer, 48 + offset);
				this.cell3_Capacity = DataParser.parse2Short(buffer, 50 + offset);
				this.cell3_Temperature = buffer[52 + offset];

				this.cell4_Status = DataParser.parse2Int(buffer, 53 + offset);
				this.cell4_Voltage = DataParser.parse2Short(buffer, 57 + offset);
				this.cell4_Current = DataParser.parse2Short(buffer, 59 + offset);
				this.cell4_Capacity = DataParser.parse2Short(buffer, 61 + offset);
				this.cell4_Temperature = buffer[63 + offset];
				break;
			default://empty fields 2 bytes long
			case SignedShort2:
			case SignedShort3:
				this.value = DataParser.parse2Short(buffer, offset);
				break;
			}
		}

		private void printValue(final long dataSectionTimeStamp) {
			switch (this.dataType) {
			case PaketGPS: /* 20 Bytes */
				//"4_Latitude","4_Longitude","2_Speed","2_Altitude","2_Course","1_isValid","4+1_UTC"
				if (LogReader.log.isLoggable(Level.FINER))
					System.out.print(String.format(Locale.ENGLISH, "%.6f°;%.6f°;%.1fkn;%.1fm;%.2f°;%d;%d;", this.latitude / 6000000.0f, this.longitude / 6000000.0f, this.speed / 10.0f, this.altitude / 10.0f,
							this.course / 100.0f, this.isValid, this.timeStamp_ms_utc));

				if (this.dataSectionTimeStamp_ms + 2000 < dataSectionTimeStamp) {
					this.latitude = this.longitude = this.speed = this.altitude = this.course = this.isValid = 0;
					this.timeStamp_ms_utc = 0L;
				}
				break;
			case PowerSupply: /* 64 Bytes */
				//"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"
				if (LogReader.log.isLoggable(Level.FINER))
					System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fV;%.3fA;%.3fV;%.3fV;%d°C;", this.status, this.voltage / 1000.0f, this.current / 1000.0f, this.inVoltage / 1000.0f,
							this.inCurrent / 1000.0f, this.mainVoltage / 1000.0f, this.reserveVoltage / 1000.0f, this.inTemperature));

				//"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"
				if (LogReader.log.isLoggable(Level.FINER)) {
					System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", this.cell1_Status, this.cell1_Voltage / 1000.0f, this.cell1_Current / 1000.0f, this.cell1_Capacity / 1000.0f,
							this.cell1_Temperature));
					System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", this.cell2_Status, this.cell2_Voltage / 1000.0f, this.cell2_Current / 1000.0f, this.cell2_Capacity / 1000.0f,
							this.cell2_Temperature));
					System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", this.cell3_Status, this.cell3_Voltage / 1000.0f, this.cell3_Current / 1000.0f, this.cell3_Capacity / 1000.0f,
							this.cell3_Temperature));
					System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", this.cell4_Status, this.cell4_Voltage / 1000.0f, this.cell4_Current / 1000.0f, this.cell4_Capacity / 1000.0f,
							this.cell4_Temperature));
				}
				if (this.dataSectionTimeStamp_ms + 2000 < dataSectionTimeStamp) {
					this.status = this.voltage = this.current = this.inVoltage = this.inCurrent = this.mainVoltage = this.reserveVoltage = this.inTemperature = 0;
					this.cell1_Status = this.cell1_Voltage = this.cell1_Current = this.cell1_Capacity = this.cell1_Temperature = 0;
					this.cell2_Status = this.cell2_Voltage = this.cell2_Current = this.cell2_Capacity = this.cell2_Temperature = 0;
					this.cell3_Status = this.cell3_Voltage = this.cell3_Current = this.cell3_Capacity = this.cell3_Temperature = 0;
					this.cell4_Status = this.cell4_Voltage = this.cell4_Current = this.cell4_Capacity = this.cell4_Temperature = 0;
				}
				break;
			default:
				String format = "%." + getDecimalPoints(this.measurement.factor) + "f%s;";
				if (LogReader.log.isLoggable(Level.FINER))
					System.out.print(String.format(Locale.ENGLISH, format, this.value * this.measurement.factor + this.measurement.offset, this.measurement.getUnit()));

				if (this.dataSectionTimeStamp_ms + 2000 < dataSectionTimeStamp) {
					this.value = 0;
				}
				break;
			}
		}

		private void addValue(final Vector<Integer> addPointsVector, final long dataSectionTimeStamp) {
			switch (this.dataType) {
			case PaketGPS: //special measurement needs to handle add filters by itself
				//"4_Latitude","4_Longitude","2_Speed","2_Altitude","2_Course","1_isValid","4+1_UTC"
				addPointsVector.add(this.latitude);
				addPointsVector.add(this.longitude);
				addPointsVector.add(this.speed * 1000);
				addPointsVector.add(this.altitude * 1000);
				addPointsVector.add(this.course * 1000);
				addPointsVector.add(this.isValid * 1000);
				if (!WeatronicAdapter.isUtcFilter) addPointsVector.add((int) (this.timeStamp_ms_utc - dataSectionTimeStamp) * 1000);
				break;
			case PowerSupply: //special measurement needs to handle add filters by itself
				//"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"
				if (!WeatronicAdapter.isStatusFilter) addPointsVector.add(this.status * 1000);
				addPointsVector.add(this.voltage * 1000);
				addPointsVector.add(this.current * 1000);
				addPointsVector.add(this.inVoltage * 1000);
				addPointsVector.add(this.inCurrent * 1000);
				addPointsVector.add(this.mainVoltage * 1000);
				addPointsVector.add(this.reserveVoltage * 1000);
				addPointsVector.add(this.inTemperature * 1000);

				//"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"
				if (!WeatronicAdapter.isStatusFilter) addPointsVector.add(this.cell1_Status * 1000);
				addPointsVector.add(this.cell1_Voltage * 1000);
				addPointsVector.add(this.cell1_Current * 1000);
				addPointsVector.add(this.cell1_Capacity * 1000);
				addPointsVector.add(this.cell1_Temperature * 1000);
				if (!WeatronicAdapter.isStatusFilter) addPointsVector.add(this.cell2_Status * 1000);
				addPointsVector.add(this.cell2_Voltage * 1000);
				addPointsVector.add(this.cell2_Current * 1000);
				addPointsVector.add(this.cell2_Capacity * 1000);
				addPointsVector.add(this.cell2_Temperature * 1000);
				if (!WeatronicAdapter.isStatusFilter) addPointsVector.add(this.cell3_Status * 1000);
				addPointsVector.add(this.cell3_Voltage * 1000);
				addPointsVector.add(this.cell3_Current * 1000);
				addPointsVector.add(this.cell3_Capacity * 1000);
				addPointsVector.add(this.cell3_Temperature * 1000);
				if (!WeatronicAdapter.isStatusFilter) addPointsVector.add(this.cell4_Status * 1000);
				addPointsVector.add(this.cell4_Voltage * 1000);
				addPointsVector.add(this.cell4_Current * 1000);
				addPointsVector.add(this.cell4_Capacity * 1000);
				addPointsVector.add(this.cell4_Temperature * 1000);
				break;
			default:
				//skipping values controlled before calling addValues
				if (this.measurement.getName().contains("_GPS_L"))
					addPointsVector.add(this.value);
				else
					addPointsVector.add(this.value * 1000);
				break;
			}
		}

		public int getDecimalPoints(final double factor) {
			if (factor >= 1.0) {
				return 1;
			}
			//Basically number of leading zeroes, e.g. 3 for 0.005
			return (int) Math.ceil(Math.abs(Math.log10(factor)));
		}

		public Measurement getMeasurement() {
			return this.measurement;
		}
	}

	public class DataSection {

		int	length;
		int	interval;
		int	readBytes;

		public DataSection(final byte[] buffer, int offset, final Vector<ConfigSection> configSections, final RecordHeader recordHeader, final long sectionTimeStamp) throws DataInconsitsentException {
			this.length = DataParser.parse2Int(buffer, 0 + offset);
			this.interval = (buffer[4 + offset] & 0xFF);
			if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("data sections %3d, interval %3d", this.length, this.interval));
			this.readBytes = 5;
			
			if (this.length == 0)
				throw new DataInconsitsentException("DataSection with length 0 detected!");

			for (ConfigSection configSection : configSections) {
				if (configSection.getInterval() == this.interval) {
					for (Integer id : configSection.ids) {
						Measurement measurement = recordHeader.getMeasurements().get(id);
						if (measurement != null && (offset + 5 + measurement.getDataSize()) <= buffer.length) {
							if (LogReader.usedMeasurementIds.contains(measurement.id)) // only add measurement which are not filtered out
								LogReader.receivedDataItems.put(id, new DataItem(buffer, offset + 5, measurement, sectionTimeStamp));
							//but keep the data offset pointer actual, this enables to filter single measurement in between without data disruption
							offset += measurement.getDataSize();
							this.readBytes += measurement.getDataSize();
						}
						else {
							if (!LogReader.unknownIds.contains(id)) {
								LogReader.unknownIds.add(id);
							}
							offset += 2; //default width used for unknown measurements
						}
					}
				}
			}
		}

		public int getReadBytes() {
			return this.readBytes;
		}

		public int getLength() {
			return this.length;
		}

		public int getInterval() {
			return this.interval;
		}

	}

	/**
	 * RecordData is a class to handle log records of type data
	 */
	public class RecordData extends Record {

		Vector<ConfigSection>	configSections			= new Vector<LogReader.ConfigSection>();
		int										configSectionCount;
		int										shortestInterval	  = 13;	//0.1Hz
		int										measurementCount		= 0;

		DataSection						dataSection;

		public RecordData(final byte[] buffer, final RecordHeader recordHeader, final long inTimeStamp_ms, final long startTimeStamp_ms) throws DataInconsitsentException {
			super();

			this.configSectionCount = buffer[0 + this.offset];
			if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, "configSectionCount = " + this.configSectionCount);
			int configSectionBytes = 0;
			for (int section = 0; section < this.configSectionCount; section++) {
				ConfigSection configSection = new ConfigSection(buffer, 1 + this.offset + configSectionBytes);
				this.shortestInterval = Math.min(this.shortestInterval, configSection.getInterval());
				this.configSections.add(configSection);
				configSectionBytes += configSection.getLength();
				this.measurementCount += configSection.getCount();
			}
			if (LogReader.log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("header measurement count = %d, config section measurement count = %d", recordHeader.getMeasurementCount(), this.measurementCount));
			this.offset = configSectionBytes + 4 + 8 + 1 + 1; //length, timeStamp, recordType, configSectionCount

			long timeOffset = getTimeOffset(this.shortestInterval);
			int count = 0;
			while (this.offset < (buffer.length - 2)) { // record contains measurement data
				if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, " -> offset = " + this.offset + " buffer.length-2 = " + (buffer.length - 2));
				DataSection tmpDataSection = new DataSection(buffer, this.offset, this.configSections, recordHeader, inTimeStamp_ms);

				//print values after receiving data section with shortest interval
				if (tmpDataSection.interval == this.shortestInterval) {
					long timeStamp_ms = inTimeStamp_ms + timeOffset * count++;
					//debug, enable stop at defined time
					//					String timeStamp = StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS;", timeStamp_ms);
					//					if (timeStamp.equals("2015-10-03 15:38:08.342;"))
					//						System.out.println(timeStamp);
					if (LogReader.log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS;", timeStamp_ms));

					Iterator<Integer> iteratorMeasurementIds = LogReader.usedMeasurementIds.iterator();

					while (iteratorMeasurementIds.hasNext()) {
						int measurementId = iteratorMeasurementIds.next();
						DataItem dataItem = LogReader.receivedDataItems.get(measurementId);
						if (dataItem == null) { //data not received
							Measurement measurement = LogReader.measurements.get(measurementId);
							switch (measurement.getDataType()) {
							case PaketGPS:
								for (int i = 0; i < (WeatronicAdapter.isUtcFilter ? 6 : 7); i++) {
									LogReader.pointsVector.add(0);
									if (LogReader.log.isLoggable(Level.FINER)) System.out.print("-;");
								}
								break;
							case PowerSupply:
								for (int i = 0; i < (WeatronicAdapter.isStatusFilter ? 23 : 28); i++) {
									LogReader.pointsVector.add(0);
									if (LogReader.log.isLoggable(Level.FINER)) System.out.print("-;");
								}
								break;

							default:
								LogReader.pointsVector.add(0);
								if (LogReader.log.isLoggable(Level.FINER)) System.out.print("-;");
								break;
							}
						}
						else {
							dataItem.addValue(LogReader.pointsVector, timeStamp_ms);
							dataItem.printValue(inTimeStamp_ms);
						}
					}
					try {
						if (LogReader.recordSet.realSize() == LogReader.pointsVector.size()) {
							LogReader.points = new int[LogReader.recordSet.realSize()];

							for (int i = 0; i < LogReader.points.length; i++) {
								LogReader.points[i] = LogReader.pointsVector.get(i);
							}
							if (LogReader.receivedDataItems.get(33968) != null) //skip first measurements as long as Tx power supply has data
								LogReader.recordSet.addPoints(LogReader.points, (timeStamp_ms - startTimeStamp_ms) * 1.0);
						}
					}
					catch (DataInconsitsentException e) {
						LogReader.application.openMessageDialogAsync(e.getMessage());
						LogReader.log.log(Level.WARNING, e.getMessage(), e);
					}
					if (LogReader.log.isLoggable(Level.FINER)) System.out.println();
					LogReader.pointsVector.clear();
				}
				if (LogReader.log.isLoggable(Level.FINER))
					if (tmpDataSection.getLength() != tmpDataSection.getReadBytes()) 
						log.log(Level.FINER, "dataSection.getLength() = " + tmpDataSection.getLength() + " != dataSection.getReadBytes() = " + tmpDataSection.getReadBytes());
				this.offset += tmpDataSection.getLength(); //tmpDataSection.getReadBytes();
			}
		}

	}

	/**
	 * RecordEvent is a class to handle all record of the log file of type event
	 */
	public class RecordEvent extends Record {

		byte[]	eventData;

		public RecordEvent(byte[] buffer) {
			super();
			this.eventData = buffer;
		}

	}

	public class LogRecord {
		int						length;
		long					timeStamp_ms;
		RecordType		recordType;
		RecordEvent		recordEvent;
		RecordData		recordData;
		RecordHeader	recordHeader;
		short					crc;

		public LogRecord(final byte[] buffer, final RecordHeader header) throws DataInconsitsentException {
			this.recordHeader = header;
			this.length = DataParser.parse2Int(buffer, 0);
			this.timeStamp_ms = DataParser.parse2Long(buffer, 4);
			this.recordType = RecordType.values()[buffer[12]];
			switch (this.recordType) {
			case HEADER: //RecordType.HEADER
				if (LogReader.log.isLoggable(Level.FINEST)) System.out.println("RecordType.HEADER");
				this.recordHeader = new RecordHeader(buffer);
				break;
			case DATA: //RecordType.DATA
				if (LogReader.log.isLoggable(Level.FINEST)) System.out.println("RecordType.DATA");
				this.recordData = new RecordData(buffer, header, this.timeStamp_ms, LogReader.startTimeStamp_ms);
				break;
			case EVENT: //RecordType.EVENT
				if (LogReader.log.isLoggable(Level.FINEST)) System.out.println("RecordType.EVENT");
				//structure of record type event is unknown and not included in available specification
				this.recordEvent = new RecordEvent(buffer);
				break;

			default:
				LogReader.log.log(Level.WARNING, "RecordType_UNKNOWN");
				LogReader.application.openMessageDialogAsync("RecordType_UNKNOWN");
				break;
			}

		}

		public int getLength() {
			return this.length;
		}

		public long getTimeStamp_ms() {
			return this.timeStamp_ms;
		}

		public RecordType getRecordType() {
			return this.recordType;
		}

		public RecordEvent getRecordEvent() {
			return this.recordEvent;
		}

		public RecordData getRecordData() {
			return this.recordData;
		}

		public RecordHeader getRecordHeader() {
			return this.recordHeader;
		}

		public short getCrc() {
			return this.crc;
		}
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized RecordSet read(String filePath, Integer channelConfigNumber) throws Exception {
		final String $METHOD_NAME = "read";
		long startTime = System.nanoTime() / 1000000;
		File file = new File(filePath);
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		long readByteCount = 0;
		boolean isInitialSwitched = false;
		LogReader.device = (WeatronicAdapter) DataExplorer.getInstance().getActiveDevice();
		LogReader logReader = new LogReader(channelConfigNumber);
		LogRecord logRecord = null; //records red from file
		LogReader.pointsVector.clear();
		LogReader.usedMeasurementIds.clear();
		LogReader.receivedDataItems.clear();
		LogReader.unknownIds.clear();
		Channel 	activeChannel;

		long timeStamp = 0;

		LogReader.startTimeStamp_ms = 0;

		MenuToolBar menuToolBar = LogReader.application.getMenuToolBar();
		GDE.getUiNotification().setProgress(0);

		try {
			if (channelConfigNumber == null)
				activeChannel = LogReader.channels.getActiveChannel();
			else
				activeChannel = LogReader.channels.get(channelConfigNumber);

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
				GDE.getUiNotification().setProgress(0);
				LogReader.channels.setActiveChannelNumber(activeChannel.getNumber());

				int logRecordCount = 0;
				while (readByteCount < fileSize) {
					LogReader.buf_length = new byte[4];
					int readSize = data_in.read(LogReader.buf_length);
					if (readSize == 4) {
						final int logRecordLength = DataParser.parse2Int(LogReader.buf_length, 0);
						if (logRecordLength > 4) {
							if (LogReader.log.isLoggable(Level.FINER)) log.log(Level.FINER, "logRecordLength = " + logRecordLength);
							LogReader.buf_log_record = new byte[logRecordLength];
							System.arraycopy(LogReader.buf_length, 0, LogReader.buf_log_record, 0, 4);
							readByteCount += 4;
							readByteCount += data_in.read(LogReader.buf_log_record, 4, logRecordLength - 4);
							//CRC check should occur at first
							//boolean isOK = Checksum.CRC16CCITT(buf_log_record, 0, buf_log_record.length-2) == DataParser.parse2UnsignedShort(buf_log_record, buf_log_record.length-2);
							//System.out.println("CRC = " + isOK);
							timeStamp = DataParser.parse2Long(LogReader.buf_log_record, 4);
							//System.out.println(StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", timeStamp));
							RecordType recordType = RecordType.values()[LogReader.buf_log_record[12]];
							if (LogReader.startTimeStamp_ms == 0 && recordType == RecordType.DATA) LogReader.startTimeStamp_ms = timeStamp;
		
							try {
								logRecord = logReader.new LogRecord(LogReader.buf_log_record, (logRecord == null ? null : logRecord.getRecordHeader()));
							}
							catch (DataInconsitsentException e) {
								log.log(Level.SEVERE, e.getMessage());
							}
							GDE.getUiNotification().setProgress((int) (readByteCount * 100 / fileSize));
							logRecordCount += 1;
						}
						else {
							log.log(Level.WARNING, "logRecordLength = 0");
							break;
						}
					}
					else {
						log.log(Level.WARNING, "datain.read failed!");
						break;
					}
				}

				LogReader.recordSet.setStartTimeStamp(LogReader.startTimeStamp_ms);
				LogReader.recordSet.setRecordSetDescription(LogReader.device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)
						+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(LogReader.startTimeStamp_ms));

				if (LogReader.log.isLoggable(Level.FINEST)) {
					System.out.println("logRecordCount = " + logRecordCount);
				}

				StringBuilder sb = new StringBuilder().append("unknown Ids = ");
				for (Integer unknownId : LogReader.unknownIds) {
					sb.append(String.format("0x%04x, ", unknownId));
				}
				LogReader.log.log(Level.WARNING, sb.toString());

				LogReader.log.logp(Level.TIME, LogReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

				if (menuToolBar != null && LogReader.recordSet != null) {
					GDE.getUiNotification().setProgress(99);
					String recordSetName = LogReader.recordSet.getName();
					activeChannel = LogReader.application.getActiveChannel();
					activeChannel.put(recordSetName, LogReader.recordSet);
					activeChannel.applyTemplate(recordSetName, false);

					if (!isInitialSwitched) {
						LogReader.channels.switchChannel(activeChannel.getName());
						activeChannel.switchRecordSet(LogReader.recordSet.getName());
					}
					else {
						LogReader.device.makeInActiveDisplayable(LogReader.recordSet);
					}
					LogReader.device.updateVisibilityStatus(LogReader.recordSet, true);

					//write filename after import to record description
					LogReader.recordSet.descriptionAppendFilename(file.getName());

					menuToolBar.updateChannelSelector();
					menuToolBar.updateRecordSetSelectCombo();
					GDE.getUiNotification().setProgress(100);
				}
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
		return LogReader.recordSet;
	}
}
