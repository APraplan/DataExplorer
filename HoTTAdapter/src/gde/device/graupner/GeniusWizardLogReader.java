/**************************************************************************************
2  	This file is part of GNU DataExplorer.

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

    Copyright (c) 2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner Genius Wizard binary data
 * @author Winfried Brügmann
 */
public class GeniusWizardLogReader extends HoTTbinReader {
	final static String	$CLASS_NAMEX	= GeniusWizardLogReader.class.getName();

	final static String	PPRODUCT_NAME			= "product_name";
	final static String	PPRODUCT_CODE			= "product_code";
	final static String	PPRODUCT_VERSION	= "product_version";
	final static String	LOG_DATA_TYPE			= "log_data_type";
	final static String	LOG_DATA_COUNT		= "log_data_count";
	final static String	LOG_DATA_OFFSET		= "log_data_offset";
	final static String	LAP_DATA_SIZE			= "lap_data_size";

	final static int headerSize = 128;
	final static Map<String, Object> header = new HashMap<String, Object>();
	final static int logDataSize = 51;
	final static StringBuilder lapData = new StringBuilder();

	static int[] points;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath) throws Exception {

		File inputFile = new File(filePath);
		FileInputStream file_input = new FileInputStream(inputFile);
		DataInputStream data_in = new DataInputStream(file_input);
		try {
			if (inputFile.exists()) {
				byte[] buffer = new byte[GeniusWizardLogReader.headerSize];
				data_in.read(buffer);
				if (new String(buffer).contains("GENIUS")) {
					byte[] flag = new byte[4];
					System.arraycopy(buffer, 0, flag, 0, 4);
					byte[] flagReverse = new byte[4];
					System.arraycopy(buffer, 4, flagReverse, 0, 4);
					for (int i = 0; i < flag.length; i++) {
						flag[i] = (byte) (0xFF - flag[i]);
						//log.fine(() ->   String.format("flag[%d] match flagReverse[%d] = %b", i,i, flag[i] == flagReverse[i]));
					}
					byte[] productname = new byte[33];
					System.arraycopy(buffer, 9, productname, 0, 33);
					log.fine(() ->  String.format("product name = %s", new String(productname)));
					header.put(PPRODUCT_NAME, new String(productname));
					/*
					byte[] productversion = new byte[4];
					System.arraycopy(buffer, 41, productversion, 0, 4);
					log.fine(() ->  String.format("product version = %s", StringHelper.byte2Hex2CharString(productversion)));
					header.put(PPRODUCT_VERSION, String.format("%d.%d%d%d", productversion[0], productversion[1], productversion[2], productversion[3]));
					*/
					byte[] logdatatype = new byte[4];
					System.arraycopy(buffer, 49, logdatatype, 0, 4);
					log.fine(() ->  String.format("logdatatype = %s", StringHelper.byte2Hex2CharString(logdatatype)));
					header.put(LOG_DATA_TYPE, StringHelper.byte2Hex2CharString(logdatatype));

					byte[] logdatacount = new byte[4];
					System.arraycopy(buffer, 53, logdatacount, 0, 4);
					log.fine(() -> String.format("logdatacount = %d", DataParser.parse2Int(logdatacount, 0)));
					header.put(LOG_DATA_COUNT, DataParser.parse2Int(logdatacount, 0));
					header.put(LOG_DATA_OFFSET, inputFile.length() - (DataParser.parse2Int(logdatacount, 0) * logDataSize));
					log.fine(() ->  String.format("logdataoffset = %d", header.get(LOG_DATA_OFFSET)));

					byte[] lapdatasize = new byte[4];
					System.arraycopy(buffer, 57, lapdatasize, 0, 4);
					log.fine(() ->  String.format("lapdatasize = %d", DataParser.parse2Int(lapdatasize, 0)));
					header.put(LAP_DATA_SIZE, DataParser.parse2Int(lapdatasize, 0));

					if ((int)header.get(LAP_DATA_SIZE) > 0) { //TODO actually no samples with lap data available!
//						JSONParser parser = new JSONParser();
//						buffer = new byte[(int)header.get(LAP_DATA_SIZE)];
//						data_in.read(buffer);
//						JSONObject json = (JSONObject) parser.parse(new String(buffer));
//						//JSONObject paredString = (JSONObject) parser.parse("{\"balance\": 1000.21, \"num\":100, \"is_vip\":true, \"name\":\"foo\"}");
//						//log.fine(() ->  "balance = " + paredString.get("balance"));
//						log.fine(() ->  "esctime = " + json.get("esctime"));
//						log.fine(() ->  "lap_starttime = " + json.get("lap_starttime"));
//						log.fine(() ->  "lap_started = " + json.get("lap_started"));
//						log.fine(() ->  "lap_value = " + json.get("lap_value"));
//						JSONArray labValues = (JSONArray) json.get("lap_value");
//						for (Object object : labValues) {
//							log.fine(() ->  object.toString());
//						}
						//TODO change implementation to use GSON, generation of class GeniusLapData is required
//						buffer = new byte[(int)header.get(LAP_DATA_SIZE)];
//						data_in.read(buffer);
//						GeniusLapData lapData = new Gson().fromJson(new String(buffer), GeniusLapData.class);
					}

					GeniusWizardLogReader.readSingle(new File(filePath));

				}
				else {
					GeniusWizardLogReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2408));
					throw new DataTypeException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2408));
				}
			}
			else throw new IOException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2409));
		}
		catch (Exception e) {
			log.log(Level.WARNING,e.getMessage(), e);
		}
		finally {
			if (data_in != null)
					data_in.close();

		}
	}

	/**
	* read log data according to version 0
	* @param file
	* @throws IOException
	* @throws DataInconsitsentException
	*/
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		GeniusWizard device = (GeniusWizard) GeniusWizardLogReader.application.getActiveDevice();
		int recordSetNumber = GeniusWizardLogReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		//String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		//0=Voltage 1=VoltageMin 2=Current 3=CurrentMax 4=RPM 5=RPM_Max 6=Capacity 7=Temp 8=TempMax 9=TempMoter 10=TempMoterMax 11=Throttle
		GeniusWizardLogReader.recordSetESC = null;
		GeniusWizardLogReader.points = new int[device.getNumberOfMeasurements(1)];
		double startLogTimeStamp_ms = 0, logTimeStamp_ms, lastLogTimeStamp_ms = 0;
		int numTimeStamps = 0;
		GeniusWizardLogReader.buf = new byte[logDataSize];
		long numberDatablocks = (fileSize - headerSize - (int)header.get(GeniusWizardLogReader.LAP_DATA_SIZE)) / logDataSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		MenuToolBar menuToolBar = GeniusWizardLogReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			HoTTbinReader.recordSets.clear();
			//ESC always contained
			channel = GeniusWizardLogReader.channels.get(1);
			channel.setFileDescription(GeniusWizardLogReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + GeniusWizardLogReader.application.getObjectKey() : date);
			recordSetName = String.format("%d) %s[%s]", recordSetNumber, device.getRecordSetStateNameReplacement(1), file.getName().substring(0, file.getName().lastIndexOf(GDE.CHAR_DOT)));
			GeniusWizardLogReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, GeniusWizardLogReader.recordSetESC);
			GeniusWizardLogReader.recordSetESC = channel.get(recordSetName);
			GeniusWizardLogReader.recordSetESC.setRecordSetDescription(String.format("%s - %s %s\n%s", device.getName(), Messages.getString(MessageIds.GDE_MSGT0129), dateTime, header.get(GeniusWizardLogReader.PPRODUCT_NAME)));
			GeniusWizardLogReader.recordSetESC.setStartTimeStamp(startTimeStamp_ms);
			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, false);
			}

			data_in.skip(headerSize - (int)header.get(GeniusWizardLogReader.LAP_DATA_SIZE)); //header + lap JSON structure

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(GeniusWizardLogReader.buf);
				if (GeniusWizardLogReader.log.isLoggable(Level.FINE)) {
					GeniusWizardLogReader.log.logp(Level.FINE, GeniusWizardLogReader.$CLASS_NAMEX, $METHOD_NAME, StringHelper.byte2Hex4CharString(GeniusWizardLogReader.buf, GeniusWizardLogReader.buf.length));
				}

				logTimeStamp_ms = DataParser.parse2Int(buf, 46) * 10.0;
				if (logTimeStamp_ms != 0 && startLogTimeStamp_ms == 0)
						startLogTimeStamp_ms = logTimeStamp_ms;

				//log.log(Level.OFF, "time = " + (logTimeStamp_ms-startLogTimeStamp_ms));
				if (logTimeStamp_ms > lastLogTimeStamp_ms) {
					GeniusWizardLogReader.recordSetESC.addPoints(device.convertDataBytes(points, buf), logTimeStamp_ms-startLogTimeStamp_ms);
					lastLogTimeStamp_ms = logTimeStamp_ms;
				}
				else if (logTimeStamp_ms == lastLogTimeStamp_ms) {
					GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2411, new Object[] { ++numTimeStamps }), SWT.COLOR_RED);
					continue;
				}
				else {
					if (numTimeStamps == 0)
						GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2412), SWT.COLOR_RED);
					else
						GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2413, new Object[] {numTimeStamps}), SWT.COLOR_RED);
					break;
				}

				if (i % progressIndicator == 0)
					GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
			}

			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, true);
				//write filename after import to record description
				GeniusWizardLogReader.recordSetESC.descriptionAppendFilename(file.getName());
				channel.setActiveRecordSet(GeniusWizardLogReader.recordSetESC);

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of this record set
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (file.getName().contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(file.getName().substring(0, file.getName().lastIndexOf(GDE.CHAR_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().lastIndexOf(GDE.CHAR_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(file.getName().substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().length()).length() <= 8 + 4)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}
}
