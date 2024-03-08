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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Class to read and write geo points exchange format data
 * @author Winfried Brügmann
 */
public class GPXWriter {
	static Logger							log					= Logger.getLogger(GPXWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");														//$NON-NLS-1$

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	static final String				ALTITUDE_ABSOLUTE					= "absolute";
	static final String				ALTITUDE_RELATIVE2GROUND	= "relativeToGround";
	static final String				ALTITUDE_CLAMP2GROUNDE		= "clampToGround";

	static final String				header_xml			= "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	static final String				header_gpx			= "<gpx version=\"1.1\" creator=\"GNU DataExplorer\">" + GDE.LINE_SEPARATOR;	//$NON-NLS-1$
	static final String				header_garmin		= "<gpx xmlns=\"https://www.topografix.com/GPX/1/1\" xmlns:gpxx=\"https://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:gpxtrkx=\"https://www.garmin.com/xmlschemas/TrackStatsExtension/v1\" xmlns:wptx1=\"https://www.garmin.com/xmlschemas/WaypointExtension/v1\" xmlns:gpxtpx=\"https://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxacc=\"https://www.garmin.com/xmlschemas/AccelerationExtension/v1\" creator=\"GNU DataExplorer\" version=\"1.1\" xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://www.topografix.com/GPX/1/1 https://www.topografix.com/GPX/1/1/gpx.xsd https://www.garmin.com/xmlschemas/GpxExtensions/v3 https://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd https://www.garmin.com/xmlschemas/TrackStatsExtension/v1 https://www8.garmin.com/xmlschemas/TrackStatsExtension.xsd https://www.garmin.com/xmlschemas/WaypointExtension/v1 https://www8.garmin.com/xmlschemas/WaypointExtensionv1.xsd https://www.garmin.com/xmlschemas/TrackPointExtension/v1 https://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd https://www.garmin.com/xmlschemas/AccelerationExtension/v1 https://www.garmin.com/xmlschemas/AccelerationExtensionv1.xsd\">" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
//static final String				header_garmin		= "<gpx xmlns=\"https://www.topografix.com/GPX/1/1\" xmlns:gpxacc=\"https://www.garmin.com/xmlschemas/AccelerationExtension/v1\" version=\"1.1\" creator=\"GNU DataExplorer\">" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				footer					= "</gpx>"  + GDE.LINE_SEPARATOR;

	static final String				metadata				= "  <metadata>" + GDE.LINE_SEPARATOR
			+ "    <link href=\"https://www.nongnu.org/dataexplorer\">" + GDE.LINE_SEPARATOR
			+ "      <text>DataExplorer</text>" + GDE.LINE_SEPARATOR
			+ "    </link>" + GDE.LINE_SEPARATOR
			+ "    <time>%sT%sZ</time>" + GDE.LINE_SEPARATOR
			+ "  </metadata>" + GDE.LINE_SEPARATOR;
	//<metadata><link href="https://www.garmin.com"><text>Garmin International</text></link><time>2013-12-04T12:31:22Z</time></metadata>

	static final String				track_begin					= "  <trk>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <name>%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <desc>%s</desc>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <trkseg>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	static final String				track_begin_garmin	= "  <trk>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <name>%s %s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <link href=\"\\DCIM\\100_VIRB\\VIRB0001.MP4\"></link>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <extensions>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "       <gpxx:TrackExtension>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxx:DisplayColor>Cyan</gpxx:DisplayColor>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "       </gpxx:TrackExtension>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "       <gpxtrkx:TrackStatsExtension>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:Distance>100</gpxtrkx:Distance>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:TimerTime>100</gpxtrkx:TimerTime>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:TotalElapsedTime>%d</gpxtrkx:TotalElapsedTime>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MovingTime>%d</gpxtrkx:MovingTime>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:StoppedTime>%d</gpxtrkx:StoppedTime>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MovingSpeed>%d</gpxtrkx:MovingSpeed>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MaxSpeed>%d</gpxtrkx:MaxSpeed>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MaxElevation>%d</gpxtrkx:MaxElevation>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MinElevation>%d</gpxtrkx:MinElevation>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:Ascent>5</gpxtrkx:Ascent>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:Descent>8</gpxtrkx:Descent>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:AvgAscentRate>2</gpxtrkx:AvgAscentRate>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MaxAscentRate>0</gpxtrkx:MaxAscentRate>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:AvgDescentRate>1</gpxtrkx:AvgDescentRate>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "         <gpxtrkx:MaxDescentRate>0</gpxtrkx:MaxDescentRate>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "       </gpxtrkx:TrackStatsExtension>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    </extensions>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    <trkseg>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				track_end					= "    </trkseg>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
			+ "    </trk>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				track_point_begin	= "      <trkpt lat=\"%.10f\" lon=\"%.10f\">" + GDE.LINE_SEPARATOR //$NON-NLS-1$
      + "        <ele>%.3f</ele>" + GDE.LINE_SEPARATOR //$NON-NLS-1$		<!-- Höhe in m -->
      + "        <time>%sT%s</time>" + GDE.LINE_SEPARATOR //$NON-NLS-1$<!-- Datum und Zeit (UTC/Zulu) ISO 8601 Format: yyyy-mm-ddThh:mm:ssZ -->
      + "        <sat>%d</sat>" + GDE.LINE_SEPARATOR //$NON-NLS-1$			<!-- Anzahl der zur Positionsberechnung herangezogenen Satelliten -->
      + "        <hdop>%.1f</hdop>" + GDE.LINE_SEPARATOR //$NON-NLS-1$	<!-- HDOP: Horizontale Streuung der Positionsangabe -->
      + "        <vdop>%.1f</vdop>" + GDE.LINE_SEPARATOR //$NON-NLS-1$	<!-- VDOP: Vertikale Streuung der Positionsangabe -->
      + "        <pdop>%.1f</pdop>" + GDE.LINE_SEPARATOR;//$NON-NLS-1$	<!-- PDOP: Streuung der Positionsangabe -->

	static final String				track_point_end		= "      </trkpt>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				extension_begin		= "        <extensions>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				extension_end			= "        </extensions>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String				garmin_acc_ext_begin		= "        <gpxacc:AccelerationExtension>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	static final String				garmin_acc_ext 					= "          <gpxacc:accel offset=\"%d\" x=\"%.1f\" y=\"%.1f\" z=\"%.1f\" />" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	static final String				garmin_acc_ext_end			= "        </gpxacc:AccelerationExtension>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$


		static final String			extension					= "<%s>%.3f</%s>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
		/* sample track point with Garmin extension
	  <trkpt lat="38.855947041884065" lon="-94.797341292724013">
	    <ele>345.79998779296875</ele>
	    <time>2013-10-11T17:08:03Z</time>
	    <extensions>
	      <gpxtpx:TrackPointExtension>
	        <gpxtpx:hr>111</gpxtpx:hr>
	        <gpxtpx:cad>55</gpxtpx:cad>
	      </gpxtpx:TrackPointExtension>
	      <gpxx:TrackPointExtension />
	      <acc:AccelerationExtension xmlns:acc="https://www.garmin.com/xmlschemas/AccelerationExtension/v1">
	        <acc:accel offset="9" x="0.1" y="0.9" z="1.3" />
	        <acc:accel offset="110" x="0.2" y="0.9" z="1.4" />
	        <acc:accel offset="209" x="0.2" y="0.8" z="1.4" />
	        <acc:accel offset="309" x="0.2" y="0.8" z="1.4" />
	        <acc:accel offset="409" x="0.1" y="0.8" z="1.4" />
	        <acc:accel offset="513" x="0" y="0.9" z="1.4" />
	        <acc:accel offset="609" x="-0.1" y="0.8" z="1.1" />
	        <acc:accel offset="709" x="-0.2" y="0.8" z="0.9" />
	        <acc:accel offset="809" x="-0.4" y="0.7" z="0.7" />
	        <acc:accel offset="909" x="-0.2" y="0.7" z="0.6" />
	      </acc:AccelerationExtension>
	    <extensions>
	  </trkpt>
		*/
	/* sample track point with unspecified extensions
	 <trkpt lat="47.2000629" lon="7.3916264">
		 <ele>1</ele>
		 <time>2013-03-14T10:01:52Z</time>
		 <sat>0</sat>
		   <extensions>
		   <Altimeter>1</Altimeter>
		   <Set_Altitude>1</Set_Altitude>
		   <Variometer>-1</Variometer>
		   <Course>0</Course>
		   <GroundSpeed>0</GroundSpeed>
		   <VerticalSpeed>0</VerticalSpeed>
		   <FlightTime>0:00</FlightTime>
		   <Voltage>15.6</Voltage>
		   <Current>0.5</Current>
		   <Capacity>6</Capacity>
		   <RCQuality>208</RCQuality>
		   <RSSI>208</RSSI>
		   <Compass>9</Compass>
		   <NickAngle>0</NickAngle>
		   <RollAngle>-4</RollAngle>
		   <NCFlags>0x01</NCFlags>
		   <FCFlags2>0xC0,0x08</FCFlags2>
		   <Thrust>21</Thrust>
		   <ErrorCode>0</ErrorCode>
		   <WaypointIndex>0</WaypointIndex>
		   <WaypointTotal>0</WaypointTotal>
		   <WaypointHoldTime>0:00</WaypointHoldTime>
		   <OperatingRadius>250</OperatingRadius>
		   <HomeDistance>0</HomeDistance>
		   <TargetBearing>0</TargetBearing>
		   <TargetDistance>0</TargetDistance>
		   <MotorCurrent>0,0,0,0,0,0,0,0,0,0,0,0</MotorCurrent>
		   <BL_Temperature>19,18,21,18,17,21,0,0,0,0,0,0</BL_Temperature>
		   <WinkelNick>9</WinkelNick>
		   <WinkelRoll>-46</WinkelRoll>
		   <AccNick>6</AccNick>
		   <AccRoll>-49</AccRoll>
		   <OperatingRadius>250</OperatingRadius>
		   <FC_Flags>192</FC_Flags>
		   <NC_Flags>1</NC_Flags>
		   <NickServo>0</NickServo>
		   <RollServo>0</RollServo>
		   <GPSDaten>210</GPSDaten>
		   <KompassRichtung>11</KompassRichtung>
		   <GyroRichtung>9</GyroRichtung>
		   <SPIFehler>0</SPIFehler>
		   <SPIOK>1006</SPIOK>
		   <I2C_Fehler>0</I2C_Fehler>
		   <I2COK>2078</I2COK>
		   <Value_16>0</Value_16>
		   <Value_17>0</Value_17>
		   <Value_18>0</Value_18>
		   <Value_19>0</Value_19>
		   <EarthMagnet>94</EarthMagnet>
		   <Z_Speed>0</Z_Speed>
		   <GeschwN>0</GeschwN>
		   <GeschwO>0</GeschwO>
		   <MagnetX>-138</MagnetX>
		   <MagnetY>380</MagnetY>
		   <MagnetZ>-256</MagnetZ>
		   <EntfernungN>0</EntfernungN>
		   <EntfernungO>0</EntfernungO>
		   <GPSNick>0</GPSNick>
		   <GPSRoll>0</GPSRoll>
		   <AnzSat>0</AnzSat>
		   </extensions>
		</trkpt>
	 */

		/**
		 * write data GPX file
		 * @param fullQualifiedPathName
		 * @param recordSet
		 * @param ordinalLongitude
		 * @param ordinalLatitude
		 * @param altitudeOrdinal
		 * @param speedOrdinal
		 * @param satellitesOrdinal
		 * @param hdopOrdinal
		 * @param vdopOrdinal
		 * @param pdodOrdinal
		 * @param accelerationXYZ
		 * @throws Exception
		 */
		public static void write(final String fullQualifiedPathName, final RecordSet recordSet,
				final int latitudeOrdinal, final int longitudeOrdinal, final int altitudeOrdinal, final int speedOrdinal, final int satellitesOrdinal,
				final int hdopOrdinal, final int vdopOrdinal, final int pdodOrdinal,
				final int[] accelerationXYZ) throws Exception {
			long startTime = new Date().getTime();
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			BufferedWriter writer = null;

			try {
				HashMap<Integer, Boolean> usedOrdinals = new HashMap<Integer, Boolean>();

				final Record recordLongitude = recordSet.get(longitudeOrdinal);
				usedOrdinals.put(longitudeOrdinal, true);
				final Record recordLatitude = recordSet.get(latitudeOrdinal);
				usedOrdinals.put(latitudeOrdinal, true);
				final Record recordHeight = recordSet.get(altitudeOrdinal);
				usedOrdinals.put(altitudeOrdinal, true);
				final Record recordSpeed = recordSet.get(speedOrdinal);
				//usedOrdinals.put(speedOrdinal, true);
				final Record recordSatellites = satellitesOrdinal == -1 ? null : recordSet.get(satellitesOrdinal);
				if (recordSatellites != null) usedOrdinals.put(satellitesOrdinal, true);
				final Record recordHDOP = hdopOrdinal == -1 ? null : recordSet.get(hdopOrdinal);
				if (recordHDOP != null) usedOrdinals.put(hdopOrdinal, true);
				final Record recordVDOP = vdopOrdinal == -1 ? null : recordSet.get(vdopOrdinal);
				if (recordVDOP != null) usedOrdinals.put(vdopOrdinal, true);
				final Record recordPDOP = pdodOrdinal == -1 ? null : recordSet.get(pdodOrdinal);
				if (recordPDOP != null) usedOrdinals.put(pdodOrdinal, true);

				final String dateString = new SimpleDateFormat("yyyy-MM-dd").format(recordSet.getStartTimeStamp());

				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_GPX, fullQualifiedPathName }));
				File targetFile = new File(fullQualifiedPathName);
				if (targetFile.exists()) {
					if (!targetFile.delete()) log.log(Level.WARNING, fullQualifiedPathName + " could not deleted!");
					if (!targetFile.createNewFile()) log.log(Level.WARNING, fullQualifiedPathName + " could not created!");
				}
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullQualifiedPathName), "UTF-8")); //$NON-NLS-1$

				writer.write(GPXWriter.header_xml);
				if (accelerationXYZ != null && accelerationXYZ.length == 3)  //Garmin extension
					writer.write(GPXWriter.header_garmin);
				else
					writer.write(GPXWriter.header_gpx);

				long lastTimeStep_sec = recordSet.getStartTimeStamp()/1000;
				//long lastTimeStep_msec = recordSet.getStartTimeStamp();
				long startTimeStep_msec = recordSet.getStartTimeStamp();
				writer.write(String.format(Locale.ENGLISH, GPXWriter.metadata, new SimpleDateFormat("yyyy-MM-dd").format(startTimeStep_msec), new SimpleDateFormat("HH:mm:ss").format(startTimeStep_msec)));
				if (accelerationXYZ != null && accelerationXYZ.length == 3)  //Garmin extension
					writer.write(String.format(Locale.ENGLISH, GPXWriter.track_begin_garmin,
							new SimpleDateFormat("yyyy-MM-dd").format(startTimeStep_msec), new SimpleDateFormat("HH:mm:ss").format(startTimeStep_msec),
							recordSet.getTime(recordSet.getRecordDataSize(true)-1)/1000, //seconds total time
							recordSet.getTime(recordSet.getRecordDataSize(true)-1)/1000, //seconds moving time
							recordSet.getTime(recordSet.getRecordDataSize(true)-1)/1000, //seconds stopped time
							recordSpeed.getAvgValue()/1000, //average moving speed
							recordSpeed.getMaxValue()/1000, //maximum moving speed
							(int)recordHeight.getMaxDisplayValue(), //maximum elevaltion
							(int)recordHeight.getMinDisplayValue()  //maximum elevaltion
							));
				else
					writer.write(String.format(Locale.ENGLISH, GPXWriter.track_begin, recordSet.getName(), 
							recordSet.getRecordSetDescription().contains("\n")
								? recordSet.getRecordSetDescription().substring(0, recordSet.getRecordSetDescription().indexOf('\n'))
								: recordSet.getRecordSetDescription()));

				// write data
				int realDataSize = recordSet.getRecordDataSize(true);
				int dataSize = recordSet.getRecordDataSize(false);
				int progressCycle = 0;
				GDE.getUiNotification().setProgress(progressCycle);

				double positionLongitude = device.translateValue(recordLongitude, recordLongitude.realGet(0) / 1000.0);
				double positionLatitude = device.translateValue(recordLongitude, recordLatitude.realGet(0) / 1000.0);
				double altitude = device.translateValue(recordHeight, recordHeight.realGet(0) / 1000.0);
				boolean isGarminExtensionWritten = false;
				int i = 0;
				for (; i < realDataSize;) {
					if (recordLongitude.realGet(i) != 0 && recordLatitude.realGet(i) != 0) {
						// latitude, longitude, heading, tilt, range, lineColor, lineWidth, extrude
						positionLatitude = device.translateValue(recordLongitude, recordLatitude.realGet(i) / 1000.0);
						positionLongitude = device.translateValue(recordLongitude, recordLongitude.realGet(i) / 1000.0);
						altitude = device.translateValue(recordHeight, recordHeight.realGet(i) / 1000.0);
						if (accelerationXYZ != null && accelerationXYZ.length == 3)  //Garmin extension
							writer.write(String.format(Locale.ENGLISH, GPXWriter.track_point_begin, positionLatitude, positionLongitude,
									altitude, dateString, new SimpleDateFormat("HH:mm:ss").format(startTimeStep_msec + recordSet.getTime(i)/10),
									recordSatellites != null ? recordSatellites.realGet(i) / 1000 : 0,
									recordHDOP != null ? recordHDOP.realGet(i) / 1000.0 : 0.0,
									recordVDOP != null ? recordVDOP.realGet(i) / 1000.0 : 0.0,
									recordPDOP != null ? recordPDOP.realGet(i) / 1000.0 : 0.0));
						else
							writer.write(String.format(Locale.ENGLISH, GPXWriter.track_point_begin, positionLatitude, positionLongitude,
									altitude, dateString, new SimpleDateFormat("HH:mm:ss.SSS").format(startTimeStep_msec + recordSet.getTime(i)/10),
									recordSatellites != null ? recordSatellites.realGet(i) / 1000 : 0,
									recordHDOP != null ? recordHDOP.realGet(i) / 1000.0 : 0.0,
									recordVDOP != null ? recordVDOP.realGet(i) / 1000.0 : 0.0,
									recordPDOP != null ? recordPDOP.realGet(i) / 1000.0 : 0.0));
						writer.write(GPXWriter.extension_begin);
						if (accelerationXYZ != null && accelerationXYZ.length == 3) {
							writer.write(GPXWriter.garmin_acc_ext_begin);
							//<acc:accel offset=\"%d\" x=\"%.1f\" y=\"%.1f\" z=\"%.1f\" />
//							writer.write(String.format(Locale.ENGLISH(), GPXWriter.garmin_acc_ext,
//									((startTimeStep_msec + recordSet.getTime(i)/10) - lastTimeStep_msec),
//									recordSet.get(accelerationXYZ[0]) != null ? recordSet.get(accelerationXYZ[0]).realGet(i) / 1000.0 : 0.0,
//									recordSet.get(accelerationXYZ[1]) != null ? recordSet.get(accelerationXYZ[1]).realGet(i) / 1000.0 : 0.0,
//									recordSet.get(accelerationXYZ[2]) != null ? recordSet.get(accelerationXYZ[2]).realGet(i) / 1000.0 : 0.0));
							while (i < realDataSize && (startTimeStep_msec + recordSet.getTime(i)/10)/1000 <= lastTimeStep_sec) {
								//<acc:accel offset=\"%d\" x=\"%.1f\" y=\"%.1f\" z=\"%.1f\" />
								writer.write(String.format(Locale.ENGLISH, GPXWriter.garmin_acc_ext,
										((startTimeStep_msec + recordSet.getTime(i)/10) - lastTimeStep_sec*1000),
										recordSet.get(accelerationXYZ[0]) != null ? recordSet.get(accelerationXYZ[0]).realGet(i) / 1000.0 : 0.0,
										recordSet.get(accelerationXYZ[1]) != null ? recordSet.get(accelerationXYZ[1]).realGet(i) / 1000.0 : 0.0,
										recordSet.get(accelerationXYZ[2]) != null ? recordSet.get(accelerationXYZ[2]).realGet(i) / 1000.0 : 0.0));
								isGarminExtensionWritten = true;
								++i;
							}
							writer.write(GPXWriter.garmin_acc_ext_end);
							if (isGarminExtensionWritten) {
								isGarminExtensionWritten = false;
								--i;
							}
						}
						else {
							for (int j = 0; j < recordSet.size(); j++) {
								if (usedOrdinals.get(j) == null && recordSet.get(j).hasReasonableData()) {
									//<Capacity>6</Capacity>
									final Record record = recordSet.get(j);
									if (record != null) {
										final String name = record.getName().replace(GDE.CHAR_BLANK, GDE.CHAR_UNDER_BAR).replace(GDE.STRING_LEFT_PARENTHESIS, GDE.STRING_EMPTY).replace(GDE.STRING_RIGHT_PARENTHESIS, GDE.STRING_EMPTY);
										writer.write(String.format(Locale.ENGLISH, "          <%s>%.3f</%s>\n", name, record.realGet(i) / 1000.0, name));
									}
								}
							}
						}
						writer.write(GPXWriter.extension_end);
						writer.write(GPXWriter.track_point_end);
						if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle * 5000) / dataSize));
						if (i < realDataSize-1 && accelerationXYZ != null && accelerationXYZ.length == 3)
							lastTimeStep_sec = (startTimeStep_msec+recordSet.getTime(i+1)/10)/1000;
//						if (i < realDataSize-1) lastTimeStep_msec = (startTimeStep_msec+recordSet.getTime(i+1)/10);

						++i;
					}
				}
				writer.write(GPXWriter.track_end);
				writer.write(GPXWriter.footer);
				writer.flush();
				writer.close();
				writer = null;
			}
			catch (IOException e) {
				GPXWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_KMZ, fullQualifiedPathName, e.getMessage() }));
			}
			catch (Exception e) {
				GPXWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			finally {
				GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
				if (writer != null) {
					try {
						writer.close();
					}
					catch (IOException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
					writer = null;
			}
		}
		GDE.getUiNotification().setProgress(100);
		GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "GPX file = " + fullQualifiedPathName + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
		}

}
