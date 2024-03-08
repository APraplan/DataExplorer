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
package gde.device.renschler;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read and write geo points exchange format data
 * @author Winfried Brügmann
 */
public class Picolario2LogReader {
	static Logger							log					= Logger.getLogger(Picolario2LogReader.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");														//$NON-NLS-1$
	static StringBuffer				sb;

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * read GPS exchange format track point and extension data
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @return
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) {
		Channel activeChannel = null;
		int lineNumber = 0;
		String recordSetNameExtend = device.getRecordSetStemNameReplacement();
		RecordSet recordSet = null;
		DataInputStream data_in = null;
		int[] points = new int[device.getNumberOfMeasurements(1)];
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		boolean isOutdated = false;

		MenuToolBar menuToolBar = Picolario2LogReader.application.getMenuToolBar();
		GDE.getUiNotification().setProgress(0);
		GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.renschler.MessageIds.GDE_MSGT1251) + filePath);

		try {
			if (channelConfigNumber == null)
				activeChannel = Picolario2LogReader.channels.getActiveChannel();
			else
				activeChannel = Picolario2LogReader.channels.get(channelConfigNumber);
			channelConfigNumber = Picolario2LogReader.channels.getActiveChannelNumber();

			if (activeChannel != null) {
				if (Picolario2LogReader.log.isLoggable(Level.FINE))
					Picolario2LogReader.log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + channelConfigNumber); //$NON-NLS-1$

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
				recordSetName = recordNameExtend.length() > 2 ? recordSetName + GDE.STRING_BLANK_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET : recordSetName;

				long startTime = System.nanoTime() / 1000000;
				File file = new File(filePath);
				FileInputStream file_input = new FileInputStream(file);
				data_in = new DataInputStream(file_input);
				long fileSize = file.length();
				long numReads = 0;
				byte[] buffer = new byte[4];
				data_in.read(buffer);
				String firmware = String.format("%.1f", DataParser.parse2Int(buffer, 0)/10.0);
				fileSize -= 4;
				data_in.read(buffer);
				double dataRate = (buffer[buffer.length-1] & 0x80) == 0 ? 1000.0/40 : 1000.0/33;
				int numValues = 0;
				for (int i = 0; i < buffer.length-1; i++) {
					byte tmpByte = buffer[i];
					for (int j=0; j < 8; j++) {
						if ((tmpByte & 0x01) != 0) {
							++numValues;
							tmpByte = (byte) (tmpByte >> 1);
						}
					}
				}
				log.log(Level.FINE, String.format("firmware version = %s; number of values = %d", firmware, numValues));
				int[] startValues = new int[numValues+1]; //height calculated from pressure
				fileSize -= 4;
//				0C 00 00 00 	03 00 00 80 	6E 82 01 00 	2F 05 01 FD
//				00 06 00 0A 	00 F7 00 08 	00 02 00 F2 	00 0B 00 02
//				00 F4 00 0F 	00 00 00 F0 	00 0D 00 00 	00 F2 00 0F
//				00 F2 00 FF 	00 0E 00 00 	00 F3 00 02 	00 09 00 F5
//				00 0F 00 FF 	00 F0 00 02 	00 0D 00 F4 	00 00 00 0E
				buffer = new byte[4];
				data_in.read(buffer);
				points[1] = startValues[1] = DataParser.parse2Int(buffer, 0) * 1000; //0x0001826E = 989.26 mbar
				startValues[0] = points[0] =  (int)(44330 * (1 - Math.pow(((points[1] / 100000.0) / 1013.25), 1/5.225)) * 1000); //height calculation
				buffer = new byte[2];
				data_in.read(buffer);
				points[2] = startValues[2] = DataParser.parse2Short(buffer, 0) * 1000; //0x052F = 1327 * 3.8 mV = 5.0 Volt
				log.log(Level.FINE, String.format("start pressure = %d; start voltage = %d", startValues[1]/1000, startValues[2]/1000));
				fileSize -= 4;

				recordSet = RecordSet.createRecordSet(recordSetName, device, channelConfigNumber, true, true, true);
				activeChannel.put(recordSetName, recordSet);
				recordSet = activeChannel.get(recordSetName);
				recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format("\nFirmware : %s", firmware));
				try {
					isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
				}
				catch (Exception e) {
					// ignore and state as not outdated
				}
				if (!isOutdated) {
					long startTimeStamp = (long) (new File(filePath).lastModified() - recordSet.getMaxTime_ms());
					recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
					recordSet.setStartTimeStamp(startTimeStamp);
					activeChannel.setFileDescription(application.isObjectoriented() ? dateTime + GDE.STRING_BLANK + application.getObjectKey() : dateTime);
					//activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
				}
				else {
					recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
				}

				if (GDE.isWithUi()) {
					activeChannel.applyTemplate(recordSetName, false);
				}
				numReads = fileSize/numValues;

				buffer = new byte[numValues]; //signed byte
				double timeStamp_ms = 0.0;
				int integrationSize = 15;
				Vector<Integer[]> values = new Vector<Integer[]>(integrationSize);
				for (int i = 0; i < integrationSize/2; i++) {
					values.add(new Integer[] {0,startValues[1]});
				}

				for (int j = 0; j < integrationSize/2+1 && data_in.read(buffer) > 0; j++) {
					Integer[] valuePoints = new Integer[numValues+1];
					for (int i = 2; i <= buffer.length; i++) {
						valuePoints[i] = points[i] += buffer[i-1] * 1000;
					}
					valuePoints[1] = points[1] += buffer[0] * 1000;
					valuePoints[0] = valuePoints[1];//pressure not corrected

					values.add(valuePoints);
					//System.out.println(StringHelper.intArrayToString(valuePoints));
				}

				do {
					for (int i = 2; i <= buffer.length; i++) {
						points[i] = values.get(integrationSize/2+1)[i];
					}
					points[1] = 0;
					for (int i = 0; i < values.size(); i++) {
						if (i != integrationSize/2+1)
							points[1] += values.get(i)[1]/(integrationSize-1);
					}
					//points[0] = values.get(integrationSize/2+1)[0];//pressure not corrected
					points[0] = (int)(44330 * (1 - Math.pow(((points[1] / 100000.0) / 1013.25), 1/5.225)) * 1000); //height calculation

					Integer[] valuePoints = new Integer[numValues+1];
					for (int i = 2; i <= buffer.length; i++) {
						valuePoints[i] = values.lastElement()[i] + buffer[i-1] * 1000;
					}
					valuePoints[1] = values.lastElement()[1] + buffer[0] * 1000;
					valuePoints[0] = valuePoints[1];//pressure not corrected
					values.add(valuePoints);
					//System.out.println(StringHelper.intArrayToString(valuePoints));
					values.remove(0);


					recordSet.addPoints(points, timeStamp_ms);
					timeStamp_ms += dataRate;

					if (fileSize % 100 == 0) {
						GDE.getUiNotification().setProgress((int) (100 - fileSize/numValues * 100 / numReads));
					}
					fileSize -= numValues;
				}
				while (data_in.read(buffer) > 0);

				data_in.close();
				data_in = null;

				GDE.getUiNotification().setProgress(100);

				if (GDE.isWithUi()) {
					Channels.getInstance().switchChannel(activeChannel.getName());
					activeChannel.switchRecordSet(recordSetName);
					device.updateVisibilityStatus(recordSet, true);

					menuToolBar.updateChannelSelector();
					menuToolBar.updateRecordSetSelectCombo();
				}

				//write filename after import to record description
				recordSet.descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1));
				log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (FileNotFoundException e) {
			Picolario2LogReader.log.log(Level.WARNING, e.getMessage(), e);
			Picolario2LogReader.application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			Picolario2LogReader.log.log(Level.WARNING, e.getMessage(), e);
			Picolario2LogReader.application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			Picolario2LogReader.log.log(Level.WARNING, e.getMessage(), e);
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			Picolario2LogReader.log.log(Level.WARNING, msg, e);
			Picolario2LogReader.application.openMessageDialog(msg);
		}
		finally {
			if (data_in != null) {
				try {
					data_in.close();
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				data_in = null;
			}
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

		return recordSet;
	}
}
