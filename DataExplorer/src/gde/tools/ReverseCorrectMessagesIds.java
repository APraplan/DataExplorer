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
package gde.tools;

import gde.GDE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Properties;

/**
 * class to correct updated message IDs reverse to messages
 * @author Winfried Brügmann
 */
public class ReverseCorrectMessagesIds {

	final static String	fileHeader				= 
		"#/************************************************************************************** \n" + "#  	This file is part of GNU DataExplorer. \n"
	+ "# \n" + "#    GNU DataExplorer is free software: you can redistribute it and/or modify \n"
	+ "#    it under the terms of the GNU General Public License as published by \n"
	+ "#    the Free Software Foundation, either version 3 of the License, or \n" + "#    (at your option) any later version. \n" + "# \n"
	+ "#    DataExplorer is distributed in the hope that it will be useful, \n"
	+ "#    but WITHOUT ANY WARRANTY; without even the implied warranty of \n" + "#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the \n"
	+ "#    GNU General Public License for more details. \n" + "# \n" + "#    You should have received a copy of the GNU General Public License \n"
	+ "#    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>. \n"
	+ "#\n"
	+ "#Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann \n"
	+ "#****************************************************************************************/ \n"
	+ "# DataExplorer message file Locale.GERMANY Winfried Brügmann  - 22 Jan 2008 \n" + " \n" 
	+ "# GDE_MSGE0000 -> error messages \n"
	+ "# GDE_MSGW0000 -> warning messages \n" 
	+ "# GDE_MSGI0000 -> info messages \n" 
	+ "# GDE_MSGT0000 -> normal text \n"
	+ "# range " + new DecimalFormat("0000").format(GDE.NUMBER_RANGE_MIN_GDE) + " to " + GDE.NUMBER_RANGE_MAX_GDE + " reserved for base GDE code \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_SAMPLE_SIM + " to " + GDE.NUMBER_RANGE_MAX_SAMPLE_SIM + " Sample + Simulator \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_AKKUMASTER + " to " + GDE.NUMBER_RANGE_MAX_AKKUMASTER + " AkkuMaster device \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_PICOLARIO + " to " + GDE.NUMBER_RANGE_MAX_PICOLARIO + " Picolario device \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_UNILOG + " to " + GDE.NUMBER_RANGE_MAX_UNILOG + " UniLog device \n" 
	+ "# range " + GDE.NUMBER_RANGE_MIN_ESTATION + " to " + GDE.NUMBER_RANGE_MAX_ESTATION + " eStation device \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_VC800 + " to " + GDE.NUMBER_RANGE_MAX_VC800 + " VC800 device \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_LIPOWATCH + " to " + GDE.NUMBER_RANGE_MAX_LIPOWATCH + " LiPoWatch device \n" 
	+ "# range " + GDE.NUMBER_RANGE_MIN_CSV2SERIAL + " to " + GDE.NUMBER_RANGE_MAX_CSV2SERIAL + " CSV2SerialDataAdapter device \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_WSTECHVARIO + " to " + GDE.NUMBER_RANGE_MAX_WSTECHVARIO + " WStechVario device  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_QC_COPTER + " to " + GDE.NUMBER_RANGE_MAX_QC_COPTER + " QC-Copter device  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_GPS_LOGGER + " to " + GDE.NUMBER_RANGE_MAX_GPS_LOGGER + " GPS-Logger device  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_NMEA_ADAPTER + " to " + GDE.NUMBER_RANGE_MAX_NMEA_ADAPTER + " NMEA-Adapter device  \n"  
	+ "# range " + GDE.NUMBER_RANGE_MIN_ULTRAMAT_DUOPLUS + " to " + GDE.NUMBER_RANGE_MAX_ULTRAMAT_DUOPLUS + " Ultramat/UltraDuoPlus devices  \n" 
	+ "# range " + GDE.NUMBER_RANGE_MIN_HOTTADAPTER + " to " + GDE.NUMBER_RANGE_MAX_HOTTADAPTER + " HoTTAdapter  \n"  
	+ "# range " + GDE.NUMBER_RANGE_MIN_UNILOG2 + " to " + GDE.NUMBER_RANGE_MAX_UNILOG2 + " UniLog2  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_ICHARGER + " to " + GDE.NUMBER_RANGE_MAX_ICHARGER + " iCharger  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_FLIGHTRECORDER + " to " + GDE.NUMBER_RANGE_MAX_FLIGHTRECORDER + " FlightRecorder  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_JLOG2 + " to " + GDE.NUMBER_RANGE_MAX_JLOG2 + " JLog2  \n"
	+ "# range " + GDE.NUMBER_RANGE_MIN_JETIADAPTER + " to " + GDE.NUMBER_RANGE_MAX_JETIADAPTER + " JetiAdapter  \n"
	// add new supported device here
	+ " \n";
	
	final static String	begin_GDE_MSGE		= "# begin GDE_MSGE0000 -> error messages \n";
	final static String	end_GDE_MSGE			= "# end GDE_MSGE0000 -> error messages \n\n";
	final static String	begin_GDE_MSGW		= "# begin GDE_MSGW0000 -> warning messages \n";
	final static String	end_GDE_MSGW			= "# end GDE_MSGW0000 -> warning messages \n\n";
	final static String	begin_GDE_MSGI		= "# begin GDE_MSGI0000 -> info messages \n";
	final static String	end_GDE_MSGI			= "# end GDE_MSGI0000 -> info messages \n\n";
	final static String	begin_GDE_MSGT		= "# begin GDE_MSGT0000 -> normal text \n";
	final static String	end_GDE_MSGT			= "# end GDE_MSGT0000 -> normal text \n\n";
	final static String	skipLine					= " \n";


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String srcFilePath = args[0]; //"src/osde/messages/messages.properties";
		String inFilePathIds = args[1]; //"src/osde/messages/MessageIds.java";
		String outFilePath = args[2]; //"src/osde/messages/messages_.properties";
		BufferedReader readerIds; // to read the ID data
		BufferedWriter writer; // to write the data
		Properties msgProps = new Properties();
		String line;
		msgProps.load(new InputStreamReader(new FileInputStream(srcFilePath), "ISO-8859-1"));
		readerIds = new BufferedReader(new InputStreamReader(new FileInputStream(inFilePathIds), "UTF-8"));
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilePath), "ISO-8859-1"));
		writer.write(fileHeader);
		writer.write(skipLine);
		readerIds.mark(50000);

		//GDE_MSGE*
		writer.write(begin_GDE_MSGE);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGE")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGE);
		readerIds.reset();

		//GDE_MSGW*
		writer.write(begin_GDE_MSGW);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGW")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGW);
		readerIds.reset();

		//GDE_MSGI*
		writer.write(begin_GDE_MSGI);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGI")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");

		}
		writer.write(end_GDE_MSGI);
		readerIds.reset();

		//GDE_MSGT*
		writer.write(begin_GDE_MSGT);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGT")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGT);

		//		writer.write(fileClosing);

		writer.close();
		readerIds.close();
	}

	/**
	 * @param sections
	 * @return
	 */
	static Integer calcMsgValue(String[] sections) {
		return Integer.valueOf(sections[6].substring(10, sections[6].length()-2));
	}
}
