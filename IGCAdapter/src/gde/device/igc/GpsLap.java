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

public class GpsLap extends HashMap<String, String>{

	private static final long serialVersionUID = -415113738338117060L;

	/**
	 * @param lapInput "alt":106,"altGainLos":-88,"index":155,"time":120.07
	 */
	public GpsLap(String lapInput) {
		super();
		add(lapInput);
	}
	
	/**
	 * @param input "alt":106,"altGainLos":-88,"index":155,"time":120.07
	 */
	public void add(String input) {
		String[] entries = input.substring(input.indexOf('{')+1, input.length()-1).split(",");
		for (String entry : entries) {
			String[] value = entry.split(":");
			if (value.length == 2) this.put(value[0].substring(1, value[0].length()-1), value[1]);
		}
	}

	public int getAlt() {
		return Integer.parseInt(this.get("alt"));
	}

	public int getAltGainLos() {
		return Integer.parseInt(this.get("altGainLos"));
	}

	public int getIndex() {
		return Integer.parseInt(this.get("index"));
	}

	public double getTime() {
		return Double.parseDouble(this.get("time"));
	}

	public int getIntTime() {		
		return Double.valueOf(this.get("time")).intValue();
	}
	
	public String getFormatedTime(int time) {
		int minutes = time/60;
		return String.format("%2d:%02d", minutes, (time - (minutes * 60)));		
	}
	
	/**
	 * @param lap
	 * @return String containing LAP  INDEX	 DURATION	LAP-TIME ALT ∆ALT
	 */
	public String toString(int lap, Double duration, String taskType) {	
		double lapFlightSpeed_kmh = 0.0, idealLapSpeed_kmh = 0.0, ratio_m = 0.0, sink_m_s = 0.0;
		switch (taskType) {
		case "Light":
			double trianglePathLength_km =  (200 + 200 + 200 * Math.sqrt(2.) + 200 * Math.sqrt(2.)) / 1000;
			double totalPathLength_km = trianglePathLength_km * getIndex() / 100.;
			lapFlightSpeed_kmh = totalPathLength_km / getTime() * 3600.;
			idealLapSpeed_kmh = trianglePathLength_km / getTime() * 3600.;
			if (getAltGainLos() < -15 && getIndex() < 130) {
				ratio_m = trianglePathLength_km * 1000 / getAltGainLos() * -1;
				sink_m_s = getAltGainLos() / getTime();
			}
			break;
		case "Sport":
			trianglePathLength_km =  (400 + 400 + 400 * Math.sqrt(2.) + 400 * Math.sqrt(2.)) / 1000;
			totalPathLength_km = trianglePathLength_km * getIndex() / 100.;
			lapFlightSpeed_kmh = totalPathLength_km / getTime() * 3600.;
			idealLapSpeed_kmh = trianglePathLength_km / getTime() * 3600.;
			if (getAltGainLos() < -20 && getIndex() < 130) {
				ratio_m = trianglePathLength_km * 1000 / getAltGainLos() * -1;
				sink_m_s = getAltGainLos() / getTime();
			}
			break;
		default:
			trianglePathLength_km =  (500 + 500 + 500 * Math.sqrt(2.) + 500 * Math.sqrt(2.)) / 1000;
			totalPathLength_km = trianglePathLength_km * getIndex() / 100.;
			lapFlightSpeed_kmh = totalPathLength_km / getTime() * 3600.;
			idealLapSpeed_kmh = trianglePathLength_km / getTime() * 3600.;
			if (getAltGainLos() < -30 && getIndex() < 130) {
				ratio_m = trianglePathLength_km * 1000 / getAltGainLos() * -1;
				sink_m_s = getAltGainLos() / getTime();
			}
			break;
		}

		return String.format("%2d  %4d %7s   %7s  %4d  %4d  %5.1f    %5.1f     %5.1f    %5.2f\n", lap, getIndex(),  getFormatedTime(duration.intValue() + getIntTime()), getFormatedTime(getIntTime()), getAlt(), getAltGainLos(), idealLapSpeed_kmh, lapFlightSpeed_kmh, ratio_m, sink_m_s);		
	}
}
