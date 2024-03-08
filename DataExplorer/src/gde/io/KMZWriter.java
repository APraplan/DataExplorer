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

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.ObjectData;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

/**
 * Class to write KMz XML files
 * @author Winfried Brügmann
 */
public class KMZWriter {
	static Logger							log							= Logger.getLogger(KMZWriter.class.getName());

	static final String				ALTITUDE_ABSOLUTE					= "absolute";
	static final String				ALTITUDE_RELATIVE2GROUND	= "relativeToGround";
	static final String				ALTITUDE_CLAMP2GROUNDE		= "clampToGround";

	static final String				header					= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" + GDE.LINE_SEPARATOR + GDE.LINE_SEPARATOR;	//$NON-NLS-1$

	static final String				position				= "<Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<name>%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<open>1</open>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<description>%s</description>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<LookAt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<gx:TimeSpan>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<begin>%sT%sZ</begin>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<end>%sT%sZ</end>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</gx:TimeSpan>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<altitude>0</altitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<heading>%d</heading>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<tilt>%d</tilt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<range>%d</range>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<altitudeMode>relativeToGround</altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t</LookAt>" + GDE.LINE_SEPARATOR;				//$NON-NLS-1$

	static final String[]			icons					= {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
	static final String				iconsdef 				= "\t<Style id=\"track-%s_n\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>0.4</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t<LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t</LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t</Style>" + GDE.LINE_SEPARATOR
																						+ "\t<Style id=\"track-%s_h\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>1.0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t</Style>" + GDE.LINE_SEPARATOR
																						+ "\t<StyleMap id=\"track-%s\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<key>normal</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#track-%s_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t<key>highlight</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#track-%s_h</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t</StyleMap>" + GDE.LINE_SEPARATOR;

	static final String				statistics 			= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t<name>%s</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t<Snippet maxLines=\"0\"></Snippet>" + GDE.LINE_SEPARATOR
																						+ "\t\t<description><![CDATA[<table>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Distance</b> %.1f km </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Min Alt</b> %.0f meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Max Alt</b> %.0f%s meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Min %s</b> %.1f %s </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Avg %s</b> %.1f %s </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Max %s</b> %.1f %s </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Start Time</b> %sT %sZ  </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>End Time</b> %sT %sZ  </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t</table>]]></description>" + GDE.LINE_SEPARATOR
																						+ "\t\t<TimeSpan>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<begin>%sT%sZ</begin>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<end>%sT%sZ</end>" + GDE.LINE_SEPARATOR
																						+ "\t\t</TimeSpan>" + GDE.LINE_SEPARATOR
																						+ "\t</Folder>" + GDE.LINE_SEPARATOR;

	static final String				pointsLeader		= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<name>Data-Track</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<visibility>0</visibility>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<open>0</open>" + GDE.LINE_SEPARATOR;

	static final String				dataPoint				= "\t\t<Placemark>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<visibility>0</visibility>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<name>t:%ds   x=%.1f%s   h=%.0fm</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Snippet maxLines=\"0\"></Snippet>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<description><![CDATA[<table>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Longitude: %.7f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Latitude: %.7f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Altitude: %.0f meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>%s: %.1f %s </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Heading: %.1f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Time: %sT %sZ </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</table>]]></description>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<LookAt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<altitude>%.0f</altitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tilt>0</tilt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<range>0</range>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</LookAt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<TimeStamp><when>%sT%sZ</when></TimeStamp>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#%s</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Point>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<altitudeMode>%s</altitudeMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<coordinates>%.7f,%.7f,%.0f</coordinates>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Point>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Placemark>" + GDE.LINE_SEPARATOR;

	static final String				emptyStyleMap		= "\t\t<StyleMap id=\"none\">" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t<Pair>" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t\t<key>normal</key>" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t\t<styleUrl>#none_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<key>highlight</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<styleUrl>#none_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t</StyleMap>" + GDE.LINE_SEPARATOR
																						+ "\t\t<Style id=\"none_n\">" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<scale>1</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<heading>0</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<color>000000000</color>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<scale>0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Style>" + GDE.LINE_SEPARATOR;

	static final String				pointsTrailer 	= "\t</Folder>" + GDE.LINE_SEPARATOR;



	static final String				speedHeader			= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t<name>%s-Track</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t<open>0</open>" + GDE.LINE_SEPARATOR;

	static final String				speedLeader			= "\t\t\t<Placemark>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<name>gx:%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t</LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<PolyStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<color>5f%s</color>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<colorMode>%s</colorMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<outline>0</outline>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t</PolyStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<gx:altitudeMode>%s</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<coordinates>" + GDE.LINE_SEPARATOR;																																																	//$NON-NLS-1$

	static final String				speedTrailer		= "\t\t\t\t</coordinates>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t</LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</Placemark>" + GDE.LINE_SEPARATOR;	//$NON-NLS-1$
	static final String				speedFooter			= "\t</Folder>" + GDE.LINE_SEPARATOR;

	static final String				triangleHeader	= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t<name>%s-Track</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t<open>0</open>" + GDE.LINE_SEPARATOR;

	static final String				triangleLeader	= "\t\t\t<Placemark>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<name>gx:%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t</LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<PolyStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<color>5f%s</color>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<colorMode>%s</colorMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<outline>0</outline>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t</PolyStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<gx:altitudeMode>%s</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<coordinates>" + GDE.LINE_SEPARATOR;																																																	//$NON-NLS-1$

	static final String				triangleTrailer	= "\t\t\t\t</coordinates>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t</LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</Placemark>" + GDE.LINE_SEPARATOR;	//$NON-NLS-1$
	static final String				triangleFooter	= "\t</Folder>" + GDE.LINE_SEPARATOR;

	static final String				footer					= "</Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "</kml>" + GDE.LINE_SEPARATOR;																																																							//$NON-NLS-1$

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * write data KMZ file containing one KML file
	 * @param kmzFilePath
	 * @param kmlFileName
	 * @param recordSet
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalAltitude
	 * @param ordinalMeasurement
	 * @param ordinalSlope
	 * @param ordinalAltitude
	 * @param ordinalTripLength
	 * @param ordinalTrackDirection
	 * @param isAltRelative
	 * @param isClampToGround
	 * @throws Exception
	 */
	public static void write(String kmzFilePath, String kmlFileName, RecordSet recordSet, final int ordinalLongitude, final int ordinalLatitude, final int ordinalAltitude, final int ordinalMeasurement, final int ordinalSlope, final int ordinalTripLength, final int ordinalTrackDirection,
 final boolean isAltRelative, boolean isClampToGround) throws Exception {
		StringBuilder sb = new StringBuilder();
		double height0 = 0.0, altitudeDelta = 0.0;
		long startTime = new Date().getTime();
		int timeStep_ms = Settings.getInstance().getKmzExportTimeStep_ms();
		ZipOutputStream zipWriter = null;
		IDevice device = DataExplorer.getInstance().getActiveDevice();

		try {
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_KMZ, kmzFilePath }));
			File targetFile = new File(kmzFilePath);
			if (targetFile.exists()) {
				if (!targetFile.delete()) log.log(Level.WARNING, kmzFilePath + " could not deleted!");
				if (!targetFile.createNewFile()) log.log(Level.WARNING, kmzFilePath + " could not created!");
			}
			zipWriter = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
			zipWriter.putNextEntry(new ZipEntry(kmlFileName));

			zipWriter.write(KMZWriter.header.getBytes());

			// write data
			int realDataSize = recordSet.getRecordDataSize(true);
			int dataSize = recordSet.getRecordDataSize(false);
			int progressCycle = 0;
			GDE.getUiNotification().setProgress(progressCycle);
			final Record recordLongitude = recordSet.get(ordinalLongitude);
			final Record recordLatitude = recordSet.get(ordinalLatitude);
			final Record recordAltitude = ordinalAltitude < 0 ? null : recordSet.get(ordinalAltitude).hasReasonableData() ? recordSet.get(ordinalAltitude) : null;
			final Record recordMeasurement = ordinalMeasurement < 0 ? null : recordSet.get(ordinalMeasurement).hasReasonableData() ? recordSet.get(ordinalMeasurement) : null;
			final Record recordSlope = ordinalSlope < 0 ? null : recordSet.get(ordinalSlope).hasReasonableData() ? recordSet.get(ordinalSlope) : null;
			final String measurementName = recordMeasurement != null ? recordMeasurement.getName() : GDE.STRING_EMPTY;
			final String measurementUnit = recordMeasurement != null ? recordMeasurement.getUnit() : GDE.STRING_EMPTY;

			boolean isExtrude = false;
			String randomColor = "";
			if (application.isObjectoriented()) {
				ObjectData object = application.getObject();
				if (object != null) {
					Properties properties = object.getProperties();
					if (properties != null) {
						if (properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value()) != null)
							isExtrude = Boolean.parseBoolean(properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value()).toString());
						if (properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value()) != null)
							randomColor = Boolean.parseBoolean(properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value()).toString()) ? "random" : "";
					}
				}
			}
			else { //device oriented
				Integer activeChannelNumber = application.getActiveChannelNumber();
				Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
				if(activeChannelNumber != null && measurementOrdinal != null && measurementOrdinal >= 0) {
					PropertyType property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value());
					if (property != null) {
						try {
							isExtrude = Boolean.parseBoolean(property.getValue());
						}
						catch (Exception e) {
							// ignore
						}
					}
					property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value());
					if (property != null) {
						try {
							randomColor = Boolean.parseBoolean(property.getValue()) ? "random" : "";
						}
						catch (Exception e) {
							// ignore
						}
					}
				}
			}

			if (recordLongitude == null || recordLatitude == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { Messages.getString(MessageIds.GDE_MSGT0599), recordSet.getChannelConfigName() }));
			if (recordAltitude == null) {
				isClampToGround = true;
				altitudeDelta = 0.0;
			}
			else {
				altitudeDelta = Math.abs(device.translateValue(recordAltitude, recordAltitude.getMaxValue() / 1000.0) - device.translateValue(recordAltitude, recordAltitude.getMinValue() / 1000.0));
				isClampToGround = isClampToGround || altitudeDelta < 10;
			}
			String altitudeMode = isClampToGround ? ALTITUDE_CLAMP2GROUNDE : isAltRelative ? ALTITUDE_RELATIVE2GROUND : ALTITUDE_ABSOLUTE;

			Record recordTripLength = ordinalTripLength < 0 ? null : recordSet.get(ordinalTripLength);
			Vector<Integer> recordAzimuth;
			try {
				if (ordinalTrackDirection >= 0)
					recordAzimuth = recordSet.get(ordinalTrackDirection);
				else
					recordAzimuth = GPSHelper.calculateAzimuth(device, recordSet, ordinalLatitude, ordinalLongitude, ordinalAltitude);
			}
			catch (Exception e) {
				recordAzimuth = GPSHelper.calculateAzimuth(device, recordSet, ordinalLatitude, ordinalLongitude, ordinalAltitude);
			}
			//find date in description
			long date = new Date().getTime();
			try {
				String[] arrayDescription = recordSet.getRecordSetDescription().split(GDE.STRING_BLANK);
				int[] intDate = new int[3];
				for (String strDate : arrayDescription) {
					String[] tmp = strDate.replace(GDE.CHAR_COMMA, GDE.CHAR_DASH).replace(GDE.CHAR_SEMICOLON, GDE.CHAR_DASH).replace(GDE.CHAR_RETURN, GDE.CHAR_DASH).replace(GDE.CHAR_NEW_LINE, GDE.CHAR_DASH).split(GDE.STRING_DASH);
					if (tmp.length >= 3 && Character.isDigit(tmp[0].charAt(0)) && Character.isDigit(tmp[1].charAt(0)) && Character.isDigit(tmp[2].charAt(0))) {
						intDate = new int[] { Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]) };
						break;
					}
				}
				int[] intTime = new int[3];
				for (String strTime : arrayDescription) {
					String[] tmp = strTime.replace(GDE.CHAR_COMMA, GDE.CHAR_COLON).replace(GDE.CHAR_SEMICOLON, GDE.CHAR_COLON).replace(GDE.CHAR_RETURN, GDE.CHAR_COLON).replace(GDE.CHAR_NEW_LINE, GDE.CHAR_COLON).split(GDE.STRING_COLON);
					if (tmp.length >= 3 && Character.isDigit(tmp[0].charAt(0)) && Character.isDigit(tmp[1].charAt(0)) && Character.isDigit(tmp[2].charAt(0))) {
						intTime = new int[] { Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]) };
						break;
					}
				}
				GregorianCalendar calendar = new GregorianCalendar(intDate[0], intDate[1] - 1, intDate[2], intTime[0], intTime[1], intTime[2]);
				date = calendar.getTimeInMillis();
			}
			catch (Exception e) {
				// ignore and use previous initialized time
			}
			String dateString = new SimpleDateFormat("yyyy-MM-dd").format(date); //$NON-NLS-1$
			
			//find altitude value which can be used as start point while exporting relative to ground
			//this is required while absolute GPS-altitude or absolute pressure related altitude where start value is not zero
			int gpsStartIndex = GPSHelper.getStartIndexGPS(recordSet, ordinalLatitude, ordinalLongitude);
			height0 = isAltRelative && !isClampToGround && recordAltitude != null ? device.translateValue(recordAltitude, recordAltitude.realGet(gpsStartIndex) / 1000.0) : 0;

			boolean isPositionWritten = false;
			double positionLongitude = device.translateValue(recordLongitude, recordLongitude.get(1) / 1000.0);
			double positionLatitude = device.translateValue(recordLongitude, recordLatitude.get(1) / 1000.0);
			int i = gpsStartIndex;
			for (; i < realDataSize; i++) {
				if (recordLongitude.get(i) != 0 && recordLatitude.get(i) != 0) {
					if (!isPositionWritten) {
						// longitude, latitude, heading, tilt, range, lineColor, lineWidth, extrude
						positionLongitude = device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0);
						positionLatitude = device.translateValue(recordLongitude, recordLatitude.get(i) / 1000.0);
						String recordSetDescription = recordSet.getRecordSetDescription();
						recordSetDescription = recordSetDescription.contains(GDE.STRING_NEW_LINE) ? recordSetDescription.split(GDE.STRING_NEW_LINE)[0] : recordSetDescription;
						zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.position, recordSet.getName(),
								recordSetDescription,
								//recordSet.getRecordSetDescription().replace("<", "min").replace(">", "max"),
								dateString, new SimpleDateFormat("HH:mm:ss").format(date),
								dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1)),
								positionLongitude, positionLatitude, -50, 70, 1000).getBytes());
						isPositionWritten = true;
						break;
					}
				}
			}
			int startIndex = i;
			int velocityRange = 0;
			int velocityAvg = recordMeasurement != null ? (int) device.translateValue(recordMeasurement, recordMeasurement.getAvgValue() / 1000.0) : 0;
			int measurementLowerLimit = 20;
			int velocityUpperLimit = 100;
			String withinLimitsColor = "ff0000ff"; //$NON-NLS-1$
			String lowerLimitColor = "ff00ff00"; //$NON-NLS-1$
			String upperLimitColor = "ff00ffff"; //$NON-NLS-1$

			if (KMZWriter.application.isObjectoriented()) {
				ObjectData object = KMZWriter.application.getObject();
				Properties properties = object.getProperties();
				if (properties != null) {
					double avgLimitFactor = 0.0;
					int lowerLimitVelocity = 0, upperLimitVelocity = 0;
					try {
						avgLimitFactor = Double.parseDouble(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					try {
						lowerLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					try {
						upperLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					measurementLowerLimit = (int) (avgLimitFactor != 0 ? velocityAvg / avgLimitFactor : lowerLimitVelocity != 0 ? lowerLimitVelocity : 0);
					velocityUpperLimit = (int) (avgLimitFactor != 0 ? velocityAvg * avgLimitFactor : upperLimitVelocity != 0 ? upperLimitVelocity : 500);

					int r, g, b;
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						withinLimitsColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						lowerLimitColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						upperLimitColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
				}
			}
			else {
				PropertyType propertyAvg = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
				PropertyType propertyLowerLimit = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
				PropertyType propertyUpperLimit = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
				measurementLowerLimit = (int) (propertyAvg != null ? velocityAvg / Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyLowerLimit.getValue()) : 0);
				velocityUpperLimit = (int) (propertyAvg != null ? velocityAvg * Double.valueOf(propertyAvg.getValue()) : propertyUpperLimit != null ? Integer.valueOf(propertyUpperLimit.getValue()) : 500);

				PropertyType propertyWithinLimitsColor = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
				PropertyType propertyLowerLimitColor = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
				PropertyType propertyUpperLimitColor = recordMeasurement == null ? null : recordMeasurement.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalMeasurement,
						MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
				String[] colorRGB = propertyWithinLimitsColor != null ? propertyWithinLimitsColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				withinLimitsColor = propertyWithinLimitsColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff0000ff"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyLowerLimitColor != null ? propertyLowerLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				lowerLimitColor = propertyLowerLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ff00"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyUpperLimitColor != null ? propertyUpperLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				upperLimitColor = propertyUpperLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ffff"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String[] velocityColors = new String[] { lowerLimitColor, withinLimitsColor, upperLimitColor };

			//write track statistics
			double relAltitude = !isClampToGround && recordAltitude != null ? device.translateValue(recordAltitude, recordAltitude.get(i) / 1000.0) - height0 : 0;
			zipWriter.write(String.format(Locale.ENGLISH, statistics,
					"Statistics",
					recordTripLength == null ? 0 : device.translateValue(recordTripLength, recordTripLength.getMaxValue() / 1000.0),
					relAltitude,
					recordAltitude == null ? 0 : device.translateValue(recordAltitude, recordAltitude.getMaxValue() / 1000.0) - height0,
					isAltRelative ? "" : String.format("(%.0f)", (recordAltitude == null ? 0.0f : device.translateValue(recordAltitude, recordAltitude.getMaxValue() / 1000.0) - relAltitude)),
					measurementName,
					recordMeasurement == null ? 0 : device.translateValue(recordMeasurement, recordMeasurement.getMinValue() / 1000.0),
					measurementUnit,
					measurementName,
					recordMeasurement == null ? 0 : device.translateValue(recordMeasurement, recordMeasurement.getAvgValue() / 1000.0),
					measurementUnit,
					measurementName,
					recordMeasurement == null ? 0 : device.translateValue(recordMeasurement, recordMeasurement.getMaxValue() / 1000.0),
					measurementUnit,
					dateString,
					new SimpleDateFormat("HH:mm:ss").format(date), dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1)), dateString,
					new SimpleDateFormat("HH:mm:ss").format(date), dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1))).getBytes());

			//speed-track
			zipWriter.write(String.format(KMZWriter.speedHeader, measurementName).getBytes());
			String initialPlacemarkName = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { measurementName, measurementLowerLimit, measurementUnit });
			zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, initialPlacemarkName, lowerLimitColor, 2, lowerLimitColor.substring(2), randomColor, isExtrude ? 1 : 0, altitudeMode).getBytes());
			long lastTimeStamp = -1, timeStamp;
			long recordSetStartTimeStamp = recordSet.getTime(recordSet.isZoomMode() ? 0 : startIndex) / 10;
			for (i = recordSet.isZoomMode() ? 0 : startIndex; isPositionWritten && i < dataSize; i++) {
				timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
				if ((timeStamp - lastTimeStamp) >= timeStep_ms || lastTimeStamp == -1) {// write a point all ~500 ms
					int velocity = recordMeasurement == null ? 0 : (int) device.translateValue(recordMeasurement, recordMeasurement.get(i) / 1000.0);
					if (recordMeasurement != null && !((velocity < measurementLowerLimit && velocityRange == 0) || (velocity >= measurementLowerLimit && velocity <= velocityUpperLimit && velocityRange == 1) || (velocity > velocityUpperLimit && velocityRange == 2))) {
						velocityRange = switchColor(zipWriter, recordMeasurement, velocity, measurementLowerLimit, velocityUpperLimit, velocityColors, velocityRange, altitudeMode, isExtrude, randomColor);

						//re-write last coordinates
//						height = device.translateValue(recordHeight, recordHeight.get(i - 1) / 1000.0) - height0;
						// add data entries, translate according device and measurement unit
//						sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
//								.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
//								.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

						zipWriter.write(sb.toString().getBytes());
					}
					sb = new StringBuilder();

					relAltitude = recordAltitude == null ? 0 : (device.translateValue(recordAltitude, recordAltitude.get(i) / 1000.0) - height0);
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.0f", relAltitude < 0 ? 0 : relAltitude)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

					zipWriter.write(sb.toString().getBytes());

					if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle * 5000) / dataSize));
					KMZWriter.log.log(java.util.logging.Level.FINER, "data line = " + sb.toString()); //$NON-NLS-1$
					lastTimeStamp = timeStamp;
				}
			}
			zipWriter.write(KMZWriter.speedTrailer.getBytes());
			zipWriter.write(KMZWriter.speedFooter.getBytes());

			//triangle-track
			int relativeAltitude = (int) (recordAltitude == null ? 0 : device.translateValue(recordAltitude, recordAltitude.getMaxValue() / 1000.0) - height0); //0 == clamp to ground
			String[] triangleTaskDefinition = recordSet.getRecordSetDescription().split(GDE.LINE_SEPARATOR);
			if (triangleTaskDefinition.length == 2 && triangleTaskDefinition[1].length() > 25 && triangleTaskDefinition[1].split(GDE.STRING_MESSAGE_CONCAT).length == 4) {
				List<String> wayPoints = new ArrayList<>();
				for (String strCoords : triangleTaskDefinition[1].split(GDE.STRING_MESSAGE_CONCAT)) {
					if (strCoords.startsWith("WP", 17))
						wayPoints.add(strCoords.substring(0,8) + GDE.STRING_SEMICOLON + strCoords.substring(8, 17));
				}
			
			zipWriter.write(String.format(KMZWriter.triangleHeader, "triangle").getBytes());
			zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.triangleLeader, "triangle",  "ffff0000", 2, "ffff0000".substring(2), randomColor, 1, altitudeMode).getBytes());
			for (i = 0; i < 3; i++) {
					sb = new StringBuilder();
					double latitude = Double.parseDouble(wayPoints.get(i).split(GDE.STRING_SEMICOLON)[0].substring(0,7))/100;
					latitude = wayPoints.get(i).split(GDE.STRING_SEMICOLON)[0].endsWith("N") ? latitude : -1 * latitude;
					double longitude = Double.parseDouble(wayPoints.get(i).split(GDE.STRING_SEMICOLON)[1].substring(0,7))/10;
					longitude = wayPoints.get(i).split(GDE.STRING_SEMICOLON)[1].endsWith("E") ? longitude : -1 * longitude;
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, longitude))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, latitude))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%d", relativeAltitude)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

					zipWriter.write(sb.toString().getBytes());
					KMZWriter.log.log(java.util.logging.Level.INFO, "data line = " + sb.toString()); //$NON-NLS-1$
			}
			sb = new StringBuilder();
			double latitude = Double.parseDouble(wayPoints.get(0).split(GDE.STRING_SEMICOLON)[0].substring(0,7))/100;
			latitude = wayPoints.get(0).split(GDE.STRING_SEMICOLON)[0].endsWith("N") ? latitude : -1 * latitude;
			double longitude = Double.parseDouble(wayPoints.get(0).split(GDE.STRING_SEMICOLON)[1].substring(0,7))/10;
			longitude = wayPoints.get(0).split(GDE.STRING_SEMICOLON)[1].endsWith("E") ? longitude : -1 * longitude;
			// add data entries, translate according device and measurement unit
			sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, longitude))) //$NON-NLS-1$
					.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, latitude))) //$NON-NLS-1$
					.append(String.format(Locale.ENGLISH, "%d", relativeAltitude)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

			zipWriter.write(sb.toString().getBytes());
			KMZWriter.log.log(java.util.logging.Level.INFO, "data line = " + sb.toString()); //$NON-NLS-1$

			zipWriter.write(KMZWriter.triangleTrailer.getBytes());
			zipWriter.write(KMZWriter.triangleFooter.getBytes());
			}

			//data-track
			zipWriter.write(KMZWriter.pointsLeader.getBytes());
			lastTimeStamp = -1;
			for (i = recordSet.isZoomMode() ? 0 : startIndex; isPositionWritten && i < dataSize; i++) {
				timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
				if ((timeStamp - lastTimeStamp) >= timeStep_ms || lastTimeStamp == -1) {// write a point all ~500 ms
					double speed = recordMeasurement == null ? 0 : device.translateValue(recordMeasurement, recordMeasurement.get(i) / 1000.0);
					double slope = recordSlope == null ? 0 : device.translateValue(recordSlope, recordSlope.get(i) / 1000.0);
					double slopeLast = i==0 ? slope : recordSlope == null ? 0 : device.translateValue(recordSlope, recordSlope.get(i-1) / 1000.0);
					boolean isSlope0 = speed > 2 && ((slope <= 0 && slopeLast > 0) || (slope > 0 && slopeLast <= 0) || slope == 0);
					relAltitude = recordAltitude == null ? 0 : (device.translateValue(recordAltitude, recordAltitude.get(i) / 1000.0) - height0);
					zipWriter.write(String.format(Locale.ENGLISH, dataPoint,
							recordSet.getTime(i) / 1000 / 10,
							speed,
							measurementUnit,
							relAltitude,
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0),
							device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0),
							relAltitude,
							measurementName,
							speed,
							measurementUnit,
							recordAzimuth.get(i) / 1000.0,
							dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(i)),
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0),	relAltitude, recordAzimuth.get(i) / 1000.0,
							dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(i)),
							isSlope0 ? getTrackIcon(recordAzimuth.get(i) / 1000.0) : "none",
							altitudeMode,
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0),
							relAltitude < 0 ? 0 : relAltitude).getBytes()); //correct visualization while height < 0

					lastTimeStamp = timeStamp;
				}
			}
			zipWriter.write(pointsTrailer.getBytes());

			//write track icons style definition
			for (int j = 0; j < icons.length; j++) {
				zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.iconsdef, icons[j], (22.5*j), icons[j], (22.5*j), icons[j], icons[j], icons[j]).getBytes());
			}
			zipWriter.write(emptyStyleMap.getBytes());

			zipWriter.write(KMZWriter.footer.getBytes());

			zipWriter.closeEntry();

			// save the track icon
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { SWTResourceManager.getImage("gde/resource/track.png").getImageData() };
			zipWriter.putNextEntry(new ZipEntry("track.png"));
			imageLoader.save(zipWriter, SWT.IMAGE_PNG);
			zipWriter.closeEntry();

			zipWriter.flush();
			zipWriter.close();
			zipWriter = null;
		}
		catch (IOException e) {
			KMZWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_KMZ, kmzFilePath, e.getMessage() }));
		}
		catch (Exception e) {
			KMZWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			if (zipWriter != null) {
				try {
					zipWriter.close();
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
				zipWriter = null;
			}
		}
		GDE.getUiNotification().setProgress(100);
		GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "KML file = " + kmzFilePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
				+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * return the fitting icon to the given azimut value
	 * @param tmpAzimuth
	 * @return
	 */
	private static String getTrackIcon(double tmpAzimuth) {
		String trackIcon = "track-" + icons[0];
		if (tmpAzimuth >= 348.75 && tmpAzimuth <= 360 || tmpAzimuth >= 0 && tmpAzimuth < 11.25)
			trackIcon = "track-" + icons[0];
		else if (tmpAzimuth >= 11.25 && tmpAzimuth < 33.75)
			trackIcon = "track-" + icons[1];
		else if (tmpAzimuth >= 33.75 && tmpAzimuth < 56.25)
			trackIcon = "track-" + icons[2];
		else if (tmpAzimuth >= 56.25 && tmpAzimuth < 78.75)
			trackIcon = "track-" + icons[3];
		else if (tmpAzimuth >= 78.75 && tmpAzimuth < 101.25)
			trackIcon = "track-" + icons[4];
		else if (tmpAzimuth >= 101.25 && tmpAzimuth < 123.75)
			trackIcon = "track-" + icons[5];
		else if (tmpAzimuth >= 123.75 && tmpAzimuth < 146.25)
			trackIcon = "track-" + icons[6];
		else if (tmpAzimuth >= 146.25 && tmpAzimuth < 168.75)
			trackIcon = "track-" + icons[7];
		else if (tmpAzimuth >= 168.75 && tmpAzimuth < 191.25)
			trackIcon = "track-" + icons[8];
		else if (tmpAzimuth >= 191.25 && tmpAzimuth < 213.75)
			trackIcon = "track-" + icons[9];
		else if (tmpAzimuth >= 213.75 && tmpAzimuth < 236.25)
			trackIcon = "track-" + icons[10];
		else if (tmpAzimuth >= 236.25 && tmpAzimuth < 258.75)
			trackIcon = "track-" + icons[11];
		else if (tmpAzimuth >= 258.75 && tmpAzimuth < 281.25)
			trackIcon = "track-" + icons[12];
		else if (tmpAzimuth >= 281.25 && tmpAzimuth < 303.75)
			trackIcon = "track-" + icons[13];
		else if (tmpAzimuth >= 303.75 && tmpAzimuth < 326.25)
			trackIcon = "track-" + icons[14];
		else if (tmpAzimuth >= 326.25 && tmpAzimuth < 348.75)
			trackIcon = "track-" + icons[15];

		return trackIcon;
	}

	/**
		velocityRange = 0; // 0 = <= 30; 1 = <= 70; 2 = <= 90; 3 = > 120
	 * @param writer
	 * @param measurementRecord
	 * @param actualValue
	 * @param measurementLowerLimit
	 * @param measurementUpperLimit
	 * @param measurementColors {lowerLimitColor, withinLimitsColor, upperLimitColor)
	 * @param velocityRange
	 * @param altitudeMode
	 * @param isExtrude
	 * @param randomColor
	 * @return
	 * @throws IOException
	 */
	public static int switchColor(ZipOutputStream writer, Record measurementRecord, int actualValue, int measurementLowerLimit, int measurementUpperLimit, String[] measurementColors, int velocityRange,
			String altitudeMode, final boolean isExtrude, final String randomColor) throws IOException {

		if (actualValue < measurementLowerLimit) {
			String placemarkName0 = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { measurementRecord.getName(), measurementLowerLimit, measurementRecord.getUnit() });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName0, measurementColors[0], 2, measurementColors[0].substring(2), randomColor, isExtrude ? 1 : 0, altitudeMode).getBytes());
			velocityRange = 0;
		}
		else if (actualValue >= measurementLowerLimit && actualValue <= measurementUpperLimit) {
			String placemarkName1 = Messages.getString(MessageIds.GDE_MSGT0605, new Object[] { measurementRecord.getName(), measurementLowerLimit, measurementUpperLimit, measurementRecord.getUnit() });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName1, measurementColors[1], 2, measurementColors[1].substring(2), randomColor, isExtrude ? 1 : 0, altitudeMode).getBytes());
			velocityRange = 1;
		}
		else if (actualValue > measurementUpperLimit) {
			String placemarkName2 = Messages.getString(MessageIds.GDE_MSGT0606, new Object[] { measurementRecord.getName(), measurementUpperLimit, measurementRecord.getUnit() });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName2, measurementColors[2], 2, measurementColors[2].substring(2), randomColor, isExtrude ? 1 : 0, altitudeMode).getBytes());
			velocityRange = 2;
		}
		return velocityRange;
	}

}
