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

    Copyright (c) 2022 Winfried Bruegmann
****************************************************************************************/
package gde.device.igc;

import java.util.HashMap;
import java.util.Vector;

import gde.utils.StringHelper;

public class GpsTaskResult extends HashMap<String, String> {
	//LISTAT{"allTrianglesAvgSpeed":3.63,"averageTriangleTime":266.06,"currentTime":1653484985032,"distanceCovered":0.0,"flightStart":1653484452960,"laps":2,"lapsStats":[],"lastStartAltitude":77.0,"lastStartCrossing":1653484985080,"lastTriangleAvgSpeed":2.34,"lastTriangleIndex":505.41,"lastTriangleTime":412.05,"scoringCode":"0000","signatureValid":false,"startAltitude":194.0,"startEntryAlti":194.0,"startEntrySpeed":25.672728,"startPenaltyPoints":94,"taskHeight":200,"taskLength":200,"timeElapsedSeconds":532,"triangleAlt":-29.0,"zoneEntered":false}

	private static final long serialVersionUID = 3386446540799897163L;
	
	private Vector<GpsLap> gpsLaps = new Vector<>();
	
	
	public GpsTaskResult() {
		super();
	}
	
	public GpsTaskResult(String input) {
		super();
		add(input);
	}
	
	public void addLap(GpsLap lap) {
		gpsLaps.add(lap);
	}

// LSTAT{"allTrianglesAvgSpeed":7.65,"averageTriangleTime":126.31,"currentTime":1655283214352,"distanceCovered":1597.1399,"flightStart":1655281983030,"laps":9,"lapsStats":[],"lastStartAltitude":121.0,"lastStartCrossing":1655283119830,"lastTriangleAvgSpeed":13.11,"lastTriangleIndex":115.0,"lastTriangleTime":73.65,"scoringCode":"0000","signatureValid":false,"startAltitude":244.0,"startEntryAlti":244.0,"startEntrySpeed":9.291832,"startPenaltyPoints":138,"taskHeight":200,"taskLength":200,"timeElapsedSeconds":1231,"triangleAlt":-10.0,"zoneEntered":false}

	/**
	 * add entries from string
	 * @param input "allTrianglesAvgSpeed":3.63,"averageTriangleTime":266.06,"currentTime":1653484985032,"distanceCovered":0.0,"flightStart":1653484452960,"laps":2,"lapsStats":[],"lastStartAltitude":77.0,"lastStartCrossing":1653484985080,"lastTriangleAvgSpeed":2.34,"lastTriangleIndex":505.41,"lastTriangleTime":412.05,"scoringCode":"0000","signatureValid":false,"startAltitude":194.0,"startEntryAlti":194.0,"startEntrySpeed":25.672728,"startPenaltyPoints":94,"taskHeight":200,"taskLength":200,"timeElapsedSeconds":532,"triangleAlt":-29.0,"zoneEntered":false
	 */
	public void add(String input) {
		String[] entries = input.substring(input.indexOf('{')+1, input.length()-1).split(",");
		for (String entry : entries) {
			String[] value = entry.split(":");
			if (value.length == 2) this.put(value[0].substring(1, value[0].length()-1), value[1]);
		}
	}

	public double getAllTrianglesAvgSpeed() {
		return Double.parseDouble(this.get("allTrianglesAvgSpeed"));
	}

	public int getAverageTriangleTime() {
		return Double.valueOf(this.get("averageTriangleTime")).intValue();
	}

	public long getCurrentTime() {
		return Long.parseLong(this.get("currentTime"));
	}

	public double getDistanceCovered() {
		return Double.parseDouble(this.get("distanceCovered"));
	}

	public long getFlightStart() {
		return Long.parseLong(this.get("flightStart"));
	}

	public int getLaps() {
		return Integer.parseInt(this.get("laps"));
	}

	public Vector<GpsLap> getLapsStats() {
		return gpsLaps;
	}
	
	public void addGpsLap(GpsLap lap) {
		this.gpsLaps.add(lap);
	}

	public double getLastStartAltitude() {
		return Double.parseDouble(this.get("lastStartAltitude"));
	}

	public long getLastStartCrossing() {
		return Long.parseLong(this.get("lastStartCrossing"));
	}

	public double getLastTriangleAvgSpeed() {
		return Double.parseDouble(this.get("lastTriangleAvgSpeed"));
	}

	public double getLastTriangleIndex() {
		return Double.parseDouble(this.get("lastTriangleIndex"));
	}

	public double getLastTriangleTime() {
		return Double.parseDouble(this.get("lastTriangleTime"));
	}

	public int getScoringCode() {
		return Integer.parseInt(this.get("scoringCode"));
	}

	public double getStartAltitude() {
		return Double.parseDouble(this.get("startAltitude"));
	}

	public double getStartEntryAlti() {
		return Double.parseDouble(this.get("startEntryAlti"));
	}

	public double getStartEntrySpeed() {
		return Double.parseDouble(this.get("startEntrySpeed"));
	}

	public boolean getSignatureValid() {
		return Boolean.parseBoolean(this.get("signatureValid"));
	}

	public int getStartPenaltyPoints() {
		return Integer.parseInt(this.get("startPenaltyPoints"));
	}

	public int getTaskHeight() {
		return Integer.parseInt(this.get("taskHeight"));
	}

	public int getTaskLength() {
		return Integer.parseInt(this.get("taskLength"));
	}

	public int getTimeElapsedSeconds() {
		return Integer.parseInt(this.get("timeElapsedSeconds"));
	}

	public double getTriangleAlt() {
		return Double.parseDouble(this.get("triangleAlt"));
	}

	public boolean getZoneEntered() {
		return Boolean.parseBoolean(this.get("zoneEntered"));
	}
	
	public String getFormatedTime(int time) {
		int minutes = time/60;
		return String.format("%2d:%02d", minutes, (time - (minutes * 60)));		
	}

	public String toString(String taskType) {
		StringBuilder sb = new StringBuilder("\n\n");
		sb.append(String.format("Task: %s  Date Time: %s  Duration: %s [mm:ss]\n", taskType, StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", getFlightStart()), getFormatedTime(getTimeElapsedSeconds())));
		sb.append(String.format("Start Alt/Speed: %3.0f m/%6.2f km/h  Penalty: %d  SavetyZoneHit: %b\n", getStartEntryAlti(), getStartEntrySpeed()*3.6, getStartPenaltyPoints(), getZoneEntered()));
		sb.append(String.format("Laps: %2d  AvgSpeed: %5.2f km/h  AvgLapTime: %s\n", getLaps(), getAllTrianglesAvgSpeed()*3.6, getFormatedTime(getAverageTriangleTime())));
		
		
		sb.append("\nLAP INDEX DURATION LAP-TIME  ALT  ∆ALT  Speed   IndexSpeed  Ratio   Sink");
		sb.append("\n[#]  [%]  [mm:ss]   [mm:ss]  [m]   [m]  [km/h]   [km/h]     [m/1]   [m/s]\n");
		int lapNo = 1;
		Double duration = 0.;
		for (GpsLap lap : gpsLaps) {
			sb.append(lap.toString(lapNo++, duration, taskType));
			duration += lap.getTime();
		}

		return sb.toString();
	}
}
