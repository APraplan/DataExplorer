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
package gde.device.powerbox;

import java.nio.charset.Charset;
import java.util.TreeSet;
import java.util.logging.Logger;

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

	
	public TelemetryData() {
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
		/** parameter decimals */
		int												decimals;
		/** parameter decimals */
		int												dataType = TelemetryData.T_DATA16;

		/**
		 * constructor telemetry variable
		 * @param _param parameter number
		 * @param _name parameter name
		 * @param _unit parameter unit
		 * @param _decimals parameter decimals
		 */
		TelemetryVar(int _param, String _name, String _unit, int _decimals) {
			this.param = _param;
			this.name = _name;
			this.unit = _unit;
			this.decimals = _decimals;
		}

		/**
		 * copy constructor
		 * @param e
		 */
		public TelemetryVar(TelemetryVar e) {
			this.param = e.param;
			this.name = e.name;
			this.unit = e.unit;
			this.decimals = e.decimals;
			this.dataType = e.dataType;
		}
		
		public void setDataType(int _dataType) { this.dataType = _dataType; }
		public int getDataType() { return this.dataType; }

		/**
		 * Adds a new telemetry item
		 * @param i data
		 * @param skip if true without time stamp
		 */
		
		/**
		 * @return unit of telemetry item
		 */
		public String getUnit() {
			return this.unit.trim();
		}

		/**
		 * @return name of telemetry item
		 */
		public String getName() {
			return this.name.trim();
		}

		/**
		 * set name of telemetry item, used in case of duplicate name found
		 */
		public void setName(String newRecordName) {
			this.name = newRecordName.trim();
		}

		/**
		 * @return translated telemetry item to string
		 */
		@Override
		public String toString() {
			return this.name + " \t" + "[" + this.unit + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}


		/**
		 * @return the parameter
		 */
		public synchronized int getParam() {
			return this.param;
		}

		@Override
		public int compareTo(TelemetryVar o) {
			return this.param - o.param;
		}
	}

}
