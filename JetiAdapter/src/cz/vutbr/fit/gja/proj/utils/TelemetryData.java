/*
Copyright (c) 2012, Martin FaltiÄ�ko, OndÅ™ej Vagner
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Jetimodel s.r.o. nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Martin FaltiÄ�ko, OndÅ™ej Vagner BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cz.vutbr.fit.gja.proj.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.SWT;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gde.GDE;
import gde.device.jeti.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;

/**
 *
 * @author Martin Falticko
 */
public class TelemetryData {
	static Logger												log							= Logger.getLogger(TelemetryData.class.getName());
  static final Charset WINDOWS_1250 = Charset.forName("Windows-1250");
  static final Charset UTF_8 = Charset.forName("UTF-8");
  static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	/** Typ dat - 8 bitu */
	public static final int							T_DATA8					= 0;
	/** Typ dat - 16 bitu */
	public static final int							T_DATA16				= 1;
	/** Typ dat - 24 bitu */
	public static final int							T_DATA24				= 4;
	/** Typ dat - Cas */
	public static final int							T_TIME					= 5;
	/** Typ dat - 32 bitu */
	public static final int							T_DATA32				= 8;
	/** Typ dat - GPS */
	public static final int							T_GPS						= 9;
	/** Typ dat - 37 bitu */
	public static final int							T_DATA37				= 12;
	/** Typ dat - event */
	public static final int							T_EVENT					= 16;

	/** start time stamp of the very first telemetryVar.TelemetryItem entry */
	long																startTimeStamp	= 0;

	public static Map<Integer, Integer>	idMap						= new HashMap<Integer, Integer>();

	/**
	 * @return Flag that the data is empty
	 */
	public boolean isEmpty() {
		return this.data.isEmpty();
	}

	public static class TimeVector extends Vector<Long> {
		private static final long	serialVersionUID	= 4860628311229067111L;

		long											firstTimeStamp		= 0;
		long											lastTimeStamp			= 0;
		long											minValue					= Long.MAX_VALUE;
		long											maxValue					= Long.MIN_VALUE;
		long											avgValue					= Long.MIN_VALUE;			// average value (avg = sum(xi)/n)
		long											sigmaValue				= Long.MIN_VALUE;			// sigma value of time steps
		long											maxTimeStamp			= 0;

		public TimeVector() {
			super();
		}

		@Override
		public synchronized boolean add(Long timeStamp) {
			boolean isAdded = false;
			if (timeStamp != 0) {
				if (this.lastTimeStamp != 0 && this.lastTimeStamp < timeStamp) {
					long delta = timeStamp - this.lastTimeStamp;
					//System.out.println("delta = " + delta);
					if (super.size() == 0) {
						this.minValue = this.maxValue = delta;
					}
					else {
						if (delta > this.maxValue)			{this.maxValue = delta; maxTimeStamp = this.lastTimeStamp-this.firstTimeStamp;}
						else if (delta < this.minValue) this.minValue = delta;
					}
					isAdded = super.add(delta);
				}
				else {
					this.firstTimeStamp = timeStamp;
				}
				this.lastTimeStamp = timeStamp;
			}
			return isAdded;
		}

		/**
		 * @return timeStamp max value occurrence
		 */
		public long getMaxValueTimeStamp() {
			return this.maxTimeStamp;
		}
		
		/**
		 * @return the avgValue
		 */
		public long getAvgValue() {
			this.setAvgValue();
			return this.avgValue;
		}

		/**
		 * calculates the avgValue
		 */
		public void setAvgValue() {
			synchronized (this) {
				if (this.size() >= 2) {
					long sum = 0;
					int zeroCount = 0;
					for (Long xi : this) {
						if (xi != 0) {
							sum += xi;
						}
						else {
							zeroCount++;
						}
					}
					this.avgValue = (this.size() - zeroCount) != 0 ? Long.valueOf(sum / (this.size() - zeroCount)).intValue() : 0;
				}
			}
		}

		/**
		 * @return the sigmaValue
		 */
		public long getSigmaValue() {
			this.setSigmaValue();
			return this.sigmaValue;
		}

		/**
		 * calculates the sigmaValue
		 */
		public void setSigmaValue() {
			synchronized (this) {
				if (super.size() >= 2) {
					long average = this.getAvgValue();
					double sumPoweredValues = 0;
					for (Long xi : this) {
						sumPoweredValues += Math.pow(xi - average, 2);
					}
					this.sigmaValue = Double.valueOf(Math.sqrt(sumPoweredValues / (this.size() - 1))).intValue();
				}
			}
		}

		public long getMinValue() {
			return this.minValue;
		}

		public long getMaxValue() {
			return this.maxValue;
		}
	}

	static TimeVector	timeSteps;

	public TelemetryData() {
		TelemetryData.timeSteps = new TimeVector();
	}

	/** describe sensor in use */
	public static class TelemetrySensor implements Comparable<TelemetrySensor> {

		/** sensor ID */
		long									id;
		/** sensor name*/
		String								name;
		/** sensor data  */
		TreeSet<TelemetryVar>	variables	= new TreeSet<TelemetryVar>();

		/**
		 * @return sensor name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return sensor identifier (Tx == 0)
		 */
		public long getId() {
			return this.id;
		}

		/**
		 * @param _id sensor ID
		 * @param _name sensor name
		 */
		public TelemetrySensor(long _id, String _name) {
			this.id = _id;
			this.name = _name;
		}

		/**
		 * add a new variable to the existing sensor
		 * @param v sensor variable
		 */
		public void addVariable(TelemetryVar v) {
			this.variables.add(new TelemetryVar(v));
		}

		/**
		 * Returns a list of all variables
		 * @return List of variable of all sensors
		 */
		public TreeSet<TelemetryVar> getVariables() {
			return this.variables;
		}

		/**
		 * Takes a link from the list of flight values
		 * @param param parameter
		 */
		public TelemetryVar getVar(int param) {
			for (TelemetryVar v : this.variables) {
				if (v.param == param) {
					return v;
				}
			}
			return null;
		}

		/**
		 * Compare two telemetry sensors
		 * @param o comparable value
		 * @return 1...greater
		 */
		@Override
		public int compareTo(TelemetrySensor o) {
			if (this.id > o.id) {
				return 1;
			}
			else if (this.id == o.id) {
				return 0;
			}
			else {
				return -1;
			}
		}
	};

	/**
	 * describe telemetry variable
	 */
	public static class TelemetryVar implements Comparable<TelemetryVar> {

		/** parameter number */
		int												param;
		/** parameter name */
		String										name;
		/** parameter unit */
		String										unit;
		/** list of telemetry values */
		ArrayList<TelemetryItem>	data;
		/** maximum and minimum values */
		double										maxValue	= 0.0, minValue = 0.0;
		/** time steps ms where data added **/
		TimeVector timeSteps;

		/**
		 * constructor telemetry variable
		 * @param _param parameter number
		 * @param _name parameter name
		 * @param _unit parameter unit
		 */
		TelemetryVar(int _param, String _name, String _unit) {
			this.param = _param;
			this.name = _name;
			this.unit = _unit;
			this.data = new ArrayList<TelemetryItem>();
			this.timeSteps = new TimeVector();
		}

		/**
		 * copy constructor
		 * @param e
		 */
		public TelemetryVar(TelemetryVar e) {
			this.param = e.param;
			this.name = e.name;
			this.unit = e.unit;
			this.data = new ArrayList<TelemetryItem>(e.data);
			this.timeSteps = new TimeVector();
		}

		/**
		 * Adds a new telemetry item
		 * @param i data
		 * @param skip if true without time stamp
		 */
		public void addItem(final TelemetryItem i, final boolean skip) {
			this.data.add(new TelemetryItem(i));
			if (!skip) {
				this.timeSteps.add(i.getTimestamp());
				TelemetryData.timeSteps.add(i.getTimestamp());
			}
		}

		/**
		 * Adds a new telemetry item at specified position
		 * @param index position
		 * @param i data
		 * @param skip if true without time stamp
		 */
		public void addItem(final int index, final TelemetryItem i, final boolean skip) {
			this.data.add(index, new TelemetryItem(i));
			if (!skip) {
				this.timeSteps.add(i.getTimestamp());
				TelemetryData.timeSteps.add(i.getTimestamp());
			}
		}

		/**
		 * @return timeSteps of telemetry item
		 */
		public TimeVector getTimeSteps() {
			return this.timeSteps;
		}
		
		/**
		 * @return unit of telemetry item
		 */
		public String getUnit() {
			return this.unit;
		}

		/**
		 * @return name of telemetry item
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return maximum value of telemetry item
		 */
		public double getMax() {
			return this.maxValue;
		}

		/**
		 * @return  minimum value of telemetry item
		 */
		public double getMin() {
			return this.minValue;
		}

		/**
		 * @return translated telemetry item to string
		 */
		@Override
		public String toString() {
			return this.name + " \t" + "[" + this.unit + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		/**
		 * Normalizes the time so that it starts from zero. Looks at max and min.
		 * Returns maximum time set
		 */
		public double normamlizeItems() {
			this.maxValue = 0.0;
			this.minValue = 0.0;
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			Collections.sort(this.data);
			long timeFINEset = 0;
			if (this.data.size() > 0) {
				//System.out.println(name);
				timeFINEset = this.data.get(0).getTimestamp();
				for (TelemetryItem row : this.data) {
					double val = row.getDouble();
					min = Math.min(val, min);
					max = Math.max(val, max);
					row.setTimestamp(row.getTimestamp() - timeFINEset);
				}
				this.maxValue = max;
				this.minValue = min;
				return this.data.get(this.data.size() - 1).timestamp / 1000.0;
			}
			return 0.0;
		}

		/**
		 * @param o telemetry variable
		 * @return Comparison of two telemetry variables
		 */
		@Override
		public int compareTo(TelemetryVar o) {
			return this.param - o.param;
		}

		/**
		 * @return parameter values
		 */
		public int getDecimals() {
			if (this.data.size() > 0) return this.data.get(0).getDecimals();

			return 0;
		}

		/**
		 * @return parameter type (1-15)
		 */
		public int getType() {
			if (this.data.size() > 0) return this.data.get(0).getType();

			return 0;
		}

		/**
		 * @return the value  approximated to the given time
		 */
		public double getDoubleAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getDouble();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getDouble();
			}
			else {
				//interpolates between the most recent time stamps
				for (int i = 0; i < this.data.size() - 1; i++) {
					TelemetryItem i1, i2;
					i1 = this.data.get(i);
					i2 = this.data.get(i + 1);
					if (i1.timestamp <= time_ms && i2.timestamp > time_ms) {
						if (i1.timestamp == i2.timestamp) {
							return i1.getDouble();
						}

						double interv = (time_ms - i1.timestamp) / (i2.timestamp - i1.timestamp);
						return i1.getDouble() + interv * (i2.getDouble() - i1.getDouble());
					}
				}
				return 0;
			}
		}

		/**
		 * @param time_ms
		 * @return potential course
		 */
		public double getDoubleCourseAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getDouble();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getDouble();
			}

			//interpolates between the most recent time stamps
			for (int i = 0; i < this.data.size() - 1; i++) {
				TelemetryItem i1, i2;
				i1 = this.data.get(i);
				i2 = this.data.get(i + 1);
				if (i1.timestamp <= time_ms && i2.timestamp > time_ms) {
					if (i1.timestamp == i2.timestamp) {
						return i1.getDouble();
					}
					double interv = (time_ms - i1.timestamp) / (i2.timestamp - i1.timestamp);
					double d1 = i1.getDouble();
					double d2 = i2.getDouble();
					if (Math.abs(d2 - d1) > 180) {
						return d1 + interv * (d2 - d1 + (d2 > d1 ? -360 : 360));
					}
					return d1 + interv * (d2 - d1);
				}
			}
			return 0;
		}

		/**
		 * @param time_ms
		 * @return integer value matching last recent time
		 */
		public int getNextIntFrom(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getInt();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getInt();
			}
			else {
				//retrieve data from most recent time stamps
				for (int i = 0; i < this.data.size() - 1; i++) {
					TelemetryItem i1, i2;
					i1 = this.data.get(i);
					i2 = this.data.get(i + 1);
					if (i1.timestamp <= time_ms && i2.timestamp > time_ms) {
						if (i1.timestamp == i2.timestamp) {
							return i1.getInt();
						}
						return i1.getInt();
					}
				}
				return 0;
			}
		}

		/**
		 * @param time_ms
		 * @return integer value matching last recent time
		 */
		public int getIntAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getInt();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getInt();
			}
			else {
				//retrieve data from most recent time stamps
				for (int i = 0; i < this.data.size(); i++) {
					TelemetryItem i1;
					i1 = this.data.get(i);
					if (i1.timestamp >= time_ms) {
						return i1.getInt();
					}
				}
				return 0;
			}
		}

		/**
		 * @param time_ms
		 * @return integer value matching last recent time
		 */
		public int getIntValueAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getIntValue();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getIntValue();
			}
			else {
				//retrieve data from most recent time stamps
				for (int i = 0; i < this.data.size(); i++) {
					TelemetryItem i1;
					i1 = this.data.get(i);
					if (i1.timestamp >= time_ms) {
						return i1.getIntValue();
					}
				}
				return 0;
			}
		}

		/**
		 * @return list of telemetry items
		 */
		public ArrayList<TelemetryItem> getItems() {
			return this.data;
		}

		/**
		 * @return the parameter
		 */
		public synchronized int getParam() {
			return this.param;
		}
	}

	/** Telemetry Record Item */
	public static class TelemetryItem implements Comparable<TelemetryItem> {

		/** data type(1-15) */
		private int		dataType;
		/** decimals */
		private int		decimals;
		/** value */
		private int		value;
		/** time stamp */
		private long	timestamp;

		/**
		 * Konstruktor
		 * @param type dataType
		 * @param dec decimals
		 * @param _value value
		 * @param _timestamp time stamp
		 */
		public TelemetryItem(int type, int dec, int _value, long _timestamp) {
			this.dataType = type;
			this.decimals = dec;
			this.value = _value;
			this.timestamp = _timestamp;
		}

		/**
		 * copy constructor
		 * @param i
		 */
		TelemetryItem(TelemetryItem i) {
			this.dataType = i.dataType;
			this.decimals = i.decimals;
			this.value = i.value;
			this.timestamp = i.timestamp;
		}

		/**
		 *
		 * @return data type
		 */
		public int getType() {
			return this.dataType;
		}

		/**
		 * @return value as double
		 */
		public double getDouble() {
			switch (this.dataType) {
			case T_DATA8:
			case T_DATA16:
			case T_DATA24:
			case T_DATA32:
			case T_DATA37:
				return this.value * Math.pow(10, -this.decimals);
			case T_GPS:
				double minute = (this.value & 0xFFFF) / 1000.0;
				double stupne = (this.value >> 16) & 0xFF;
				stupne = stupne + minute / 60.0;
				return stupne * (((this.decimals >> 1) & 1) == 1 ? -1 : 1);
			case T_TIME:
				return this.value * 1.;
			default:
				return 0.0;
			}
		}

		/**
		 * @return value as integer corrected with decimals (factor) and adapted to GDE data model
		 */
		public int getIntValue() {
			switch (this.dataType) {
			case T_DATA8:
			case T_DATA16:
			case T_DATA24:
			case T_DATA32:
			case T_DATA37:
				switch (this.decimals) {
				case 0: //1.0
					return (int) (this.value * 1000);
				case 1: //0.1
					return (int) (this.value * 100);
				case 2: //0.01
					return (int) (this.value * 10);
				case 3: //0.001
					return (int) (this.value * 1);
				default:
					return (int) (this.value * Math.pow(10, -this.decimals) * 1000);
				}
			case T_GPS:
				double minute = (this.value & 0xFFFF) / 1000.0;
				double stupne = (this.value >> 16) & 0xFF;
				stupne = stupne + minute / 60.0;
				return (int) (stupne * (((this.decimals >> 1) & 1) == 1 ? -1 : 1) * 1000000);
			case T_TIME:
				switch (this.decimals) {
				case 0: //time
					return (((this.value >> 16) * 10000) + ((this.value >> 8 & 0x00FF) * 100) + (this.value & 0x0000FF)) * 1000;
				case 1: //date
					return (((this.value & 0x0000FF) * 10000) + ((this.value >> 8 & 0x00FF) * 100) + (this.value >> 16)) * 1000;
				default:
					return this.value * 1000;
				}
			default:
				return 0;
			}
		}

		/**
		 * @return value as integer
		 */
		public int getInt() {
			return this.value;
		}

		/**
		 * @return Compare two telemetry items
		 */
		@Override
		public int compareTo(TelemetryItem o) {
			if (this.timestamp > o.timestamp) {
				return 1;
			}
			else if (this.timestamp == o.timestamp) {
				return 0;
			}
			else {
				return -1;
			}
		}

		/**
		 * @return time stamp as long
		 */
		public long getTimestamp() {
			return this.timestamp;
		}

		/**
		 * set time stamp
		 * @param l
		 */
		private void setTimestamp(long l) {
			this.timestamp = l;
		}

		/**
		 *
		 * @return Number of decimal places
		 */
		private int getDecimals() {
			return this.decimals;
		}
	}

	/**
	 * Structure with telemetry sensor data 
	 */
	private TreeSet<TelemetrySensor>	data					= new TreeSet<TelemetrySensor>();
	/**
	 * Maximal time stamp - total count of milliseconds
	 */
	private double										maxTimestamp	= 0.0;

	private String										modelName			= "";														//$NON-NLS-1$

	/**
	 * @return a list of loaded telemetry data
	 */
	public TreeSet<TelemetrySensor> getData() {
		return this.data;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getModelName() {
		return this.modelName;
	}

	/**
	 * @return the sensor according to the given ID
	 */
	public TelemetrySensor getSensor(long id) {
		for (TelemetrySensor s : this.data) {
			if (s.id == id) {
				return s;
			}
		}
		return null;
	}

	/**
	   * @return Maximum time during flight
	   */
	public double getMaxTimestamp() {
		return this.maxTimestamp;
	}

	/**
	 * load data from file. Supported is *.log or *.jml
	 * @param file
	 */
	public boolean loadData(String file) {
		TelemetryData.idMap.clear();
		this.maxTimestamp = 0.0;
		int mid = file.lastIndexOf("."); //$NON-NLS-1$
		String ext = file.substring(mid + 1, file.length());
		if (ext.equalsIgnoreCase("log")) { //$NON-NLS-1$
			this.data.clear();
			if (!loadCSV(file)) {
				return false;
			}
		}
		else if (ext.equalsIgnoreCase("jml")) { //$NON-NLS-1$
			this.data.clear();
			if (!loadJML(file)) {
				return false;
			}
		}
		else {
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2900, new String[] { file }));
			//JOptionPane.showMessageDialog(null, "Error: " + "NeznÃ¡mÃ¡ koncovka souboru", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		//Now all the items and times, data
		for (TelemetrySensor s : this.data) {
			for (TelemetryVar d : s.variables) {
				this.maxTimestamp = Math.max(this.maxTimestamp, d.normamlizeItems());
			}
		}
		return true;
	}

	/**
	 * It uploads a jml file that is based on XML
	 * @param filename
	 * @return true|false true means successful
	 */
	boolean loadJML(String filename) {
		try {
			File file = new File(filename);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList sensors = doc.getElementsByTagName("dataStorage"); //$NON-NLS-1$

			for (int s = 0; s < sensors.getLength(); s++) {
				Node sensor = sensors.item(s);

				if (sensor.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) sensor;
					long ID = Long.parseLong(fstElmnt.getAttribute("dataStorageID")); //$NON-NLS-1$
					//introduce new sensor
					TelemetrySensor tel = new TelemetrySensor(ID, "-"); //$NON-NLS-1$
					this.data.add(tel);
					//go through the attributes
					NodeList elements = fstElmnt.getElementsByTagName("attrDescription"); //$NON-NLS-1$
					for (int i = 0; i < elements.getLength(); i++) {
						Node var = elements.item(i);
						if (var.getNodeType() == Node.ELEMENT_NODE) {
							Element varElem = (Element) var;
							int varId = Integer.parseInt(varElem.getAttribute("attrID")); //$NON-NLS-1$
							String name = varElem.getAttribute("name"); //$NON-NLS-1$
							String unit = varElem.getAttribute("units"); //$NON-NLS-1$
							TelemetryVar telvar = new TelemetryVar(varId, name, unit);
							//introduce the telemetry variable
							tel.addVariable(telvar);
						}
					}

					//going through the data for the given clue
					elements = fstElmnt.getElementsByTagName("entity"); //$NON-NLS-1$
					for (int i = 0; i < elements.getLength(); i++) {
						Node var = elements.item(i);
						if (var.getNodeType() == Node.ELEMENT_NODE) {
							Element varElem = (Element) var;
							String row = String.valueOf(ID) + ";" + varElem.getAttribute("plainData"); //$NON-NLS-1$ //$NON-NLS-2$
							String rowData[] = row.split(";"); //$NON-NLS-1$
							if (rowData.length > 2) {
								//Shuffle ID and timestamp
								String tmp = rowData[1];
								rowData[1] = rowData[0];
								rowData[0] = tmp;
								parseLineParams(rowData);
							}
						}
					}
				}
				int progress = s * 100 / (sensors.getLength() * 2 / 3);
				if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) {
					GDE.getUiNotification().setProgress(progress);
					try {
						Thread.sleep(2);
					}
					catch (Exception e) {
						// ignore
					}
				}

			}
			return true;
		}
		catch (Exception e) {
			this.getData().clear();
			TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2901, new String[] { filename }));
			//JOptionPane.showMessageDialog(null, "Chyba JML, " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/**
	 * uploads a CSV file. 
	 * @return info on success
	 */
	boolean loadCSV(String file) {
		int line = 0;
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader in = new InputStreamReader(fis, "ISO-8859-1"); //$NON-NLS-1$

			long inputFileSize = new File(file).length();
			int progressLineLength = 0;

			//FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			//DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(in);

			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				line++;
				//strLine = strLine.trim();
				//First character - commentary?
				if (strLine.startsWith("#")) { //$NON-NLS-1$
					this.modelName = strLine.substring(1).trim();
					continue;
				}

				progressLineLength = progressLineLength > strLine.length() ? progressLineLength : strLine.length();
				int progress = (int) (line * 100 / (inputFileSize / progressLineLength));
				if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) {
					GDE.getUiNotification().setProgress(progress);
					try {
						Thread.sleep(2);
					}
					catch (Exception e) {
						// ignore
					}
				}

				List<String> array = new ArrayList<String>();
				array.addAll(Arrays.asList(strLine.replace("|", ";").split(";")));
				if (array != null && array.size() > 0) {
					if (array.size() == 4) { //only sensors/variables may have 4 entries in array while missing a unit 
						log.log(Level.WARNING, String.format("Sensor variable missing unit! - %s", array));
						array.add(GDE.STRING_EMPTY);
					}
					parseLineParams(array.toArray(new String[5]));
				}
			}
			//Close the input stream
			in.close();
			return true;
		}
		catch (Exception e) {//Catch exception if any
			this.getData().clear();
			TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2902, new String[] { file, String.valueOf(line) }));
			//JOptionPane.showMessageDialog(null, "Chyba na Å™Ã¡dku " + String.valueOf(line) + ", " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/**
	 * parse string array into parameter
	 */
	void parseLineParams(String params[]) {
		final int ST_TIME = 0;
		final int ST_DEVICE_ID = 1;
		final int ST_PARAM_NUM = 2;
		final int ST_DATATYPE = 3;
		final int ST_DECIMALS = 4;
		final int ST_VALUE = 5;
		final int ST_LABEL = 6;
		final int ST_UNIT = 7;

		int state = ST_TIME;
		long timestamp = 0;
		long deviceId = 0;
		int paramId = 0;
		int dataType = 0;
		int decimals = 0;
		String label = ""; //$NON-NLS-1$
		String unit = ""; //$NON-NLS-1$
		if (params == null) {
			return;
		}
		for (String param : params) {
			switch (state) {
			case ST_TIME:
				timestamp = Long.parseLong(param);
				state = ST_DEVICE_ID;
				break;
			case ST_DEVICE_ID:
				//device id sometimes ;42020    1; LiVa or | 0   0| Alarm: Cap..
				try {
					deviceId = Long.parseLong(!(param.startsWith(" ") && param.endsWith("0"))  ? param.replace(' ', '0') : param);
				}
				catch (NumberFormatException e) {
					log.log(Level.WARNING, "skip | param = " + param);
				}
				state = ST_PARAM_NUM;
				break;
			case ST_PARAM_NUM:
				paramId = Integer.parseInt(param);
				if (timestamp == 0) {
					state = ST_LABEL;
				}
				else {
					state = ST_DATATYPE;
				}
				break;
			case ST_LABEL:
				label = param;
				//Insert a new sensor and exit the queue
				if (timestamp == 0 && paramId == 0) {
					if (TelemetryData.log.isLoggable(Level.INFO)) TelemetryData.log.log(Level.INFO, "adding sensor " + label);
					TelemetrySensor sensor = new TelemetrySensor(deviceId, label);
					this.data.add(sensor);
					return;
				}
				//call the parameter label
				state = ST_UNIT;
				break;
			case ST_UNIT:
				unit = param;
				TelemetryVar var = new TelemetryVar(paramId, label, unit);
				TelemetrySensor s = this.getSensor(deviceId);
				if (s != null) {
					if (TelemetryData.log.isLoggable(Level.INFO)) TelemetryData.log.log(Level.INFO, String.format("%d add variable %s[%s] ID=%d", deviceId, var.name, unit, paramId));
					s.addVariable(var);
				}
				//no function
				return;
			case ST_DATATYPE:
				dataType = Integer.parseInt(param);
				state = ST_DECIMALS;
				break;
			case ST_DECIMALS:
				decimals = Integer.parseInt(param);
				state = ST_VALUE;
				break;
			case ST_VALUE:
				long val = 0;
				try {
					val = Long.parseLong(param);
				}
				catch (NumberFormatException e) {
					if (param.length() > 3 && this.startTimeStamp > 0) {
						// String value, for instance an alarm
						String utf8String = new String(param.getBytes(ISO_8859_1));
						String message = TimeLine.getFomatedTimeWithUnit(timestamp) + " - " + utf8String;
						TelemetryData.log.log(Level.WARNING, message);
						GDE.getUiNotification().setStatusMessage(message, SWT.COLOR_RED);
						// Alarm:  Capacity
						deviceId = 0; //force this even if there is a real Tx ID to enable sort in JetiDataReader
						TelemetrySensor sensor = this.getSensor(deviceId);
						if (sensor == null) {
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "adding sensor " + label);
							sensor = new TelemetrySensor(deviceId, label);
							this.data.add(sensor);
						}
						label = utf8String;
						paramId = getParameterIdByName(sensor, label);
						dataType = TelemetryData.T_DATA8;
						if (TelemetryData.log.isLoggable(Level.FINE))
							TelemetryData.log.log(Level.FINE, "deviceId = " + deviceId + ", paramId = " + paramId + ", untit = " + unit + ", dataType = " + dataType + ", state = " + state + ", decimals = "
									+ decimals);
						TelemetryVar par = sensor.getVar(paramId);
						if (par == null) {
							par = new TelemetryVar(paramId, label, unit);
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add variable " + par.name);
							sensor.addVariable(par);
							TelemetryItem item = new TelemetryItem(dataType, decimals, 0, this.startTimeStamp);
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.getName() + "=" + item.getInt());
							par = sensor.getVar(paramId); //read again since copy constructor was used during addVariable
							par.addItem(item, true);
						}

						//add old value, just before new event value in respect of time
						par.addItem(new TelemetryItem(dataType, decimals, par.getItems().get(par.getItems().size() - 1).getInt(), timestamp - 5), true);
						par.addItem(new TelemetryItem(dataType, decimals, (int) (par.maxValue += 1), timestamp), true);
						if (TelemetryData.log.isLoggable(Level.FINE)) {
							TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.getName() + "=" + par.getItems().get(par.getItems().size() - 1).getInt());
							TelemetryData.log.log(Level.FINE, "add sensor variable value count " + par.getItems().size());
						}
					}
					state = ST_PARAM_NUM;
					break;
				}
				//Try to insert a new record
				int intval = 0;
				if (dataType == TelemetryData.T_DATA16)
					intval = (short) val;
				else if (dataType == TelemetryData.T_DATA8)
					intval = (byte) val;
				else
					intval = (int) val;
				if (TelemetryData.log.isLoggable(Level.FINE))
					log.log(Level.FINE, String.format("TelemetryData: deviceId=%d, paramId=%d, untit='%s', dataType=%d, state=%d, decimals=%d, value=%d, timeStamp=%d", deviceId, paramId, unit, dataType, state, decimals, intval, timestamp));
				TelemetryItem item = new TelemetryItem(dataType, decimals, intval, timestamp);
				TelemetrySensor sen = this.getSensor(deviceId);
				if (sen != null) {
					TelemetryVar par = sen.getVar(paramId);
					if (par != null) {
						if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.name + "=" + item.value);
						par.addItem(item, false);
						if (this.startTimeStamp == 0) {
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "set startTimeStamp = " + timestamp);
							this.startTimeStamp = timestamp;
						}
					}
				}
				state = ST_PARAM_NUM;
				break;
			}
		}
	}

	public static int getParameterIdByName(final TelemetrySensor sensor, final String label) {
		int labelParamId = 0;
		for (int i = 0; i < label.length(); i++) {
			labelParamId += label.charAt(i);
		}
		if (TelemetryData.idMap.get(labelParamId) == null) {
			int newPartamId = sensor.getVariables().size() + 1;
			TelemetryData.idMap.put(labelParamId, newPartamId);
		}
		return TelemetryData.idMap.get(labelParamId);
	}

	public long getMinTimeStep() {
		return TelemetryData.timeSteps.getMinValue();
	}

	public long getMaxTimeStep() {
		return TelemetryData.timeSteps.getMaxValue();
	}

	public long getSigmaTimeStep() {
		return TelemetryData.timeSteps.getSigmaValue();
	}

	public long getAvgTimeStep() {
		return TelemetryData.timeSteps.getAvgValue();
	}

}
