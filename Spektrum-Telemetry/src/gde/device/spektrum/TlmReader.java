/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.spektrum;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.monstarmike.tlmreader.Flight;
import com.monstarmike.tlmreader.IFlight;
import com.monstarmike.tlmreader.TLMReader;
import com.monstarmike.tlmreader.datablock.AirspeedBlock;
import com.monstarmike.tlmreader.datablock.AltitudeBlock;
import com.monstarmike.tlmreader.datablock.AltitudeZeroBlock;
import com.monstarmike.tlmreader.datablock.CurrentBlock;
import com.monstarmike.tlmreader.datablock.DataBlock;
import com.monstarmike.tlmreader.datablock.EscBlock;
import com.monstarmike.tlmreader.datablock.FlightPackBlock;
import com.monstarmike.tlmreader.datablock.GForceBlock;
import com.monstarmike.tlmreader.datablock.GPSCollectorBlock;
import com.monstarmike.tlmreader.datablock.GPSLocationBlock;
import com.monstarmike.tlmreader.datablock.GPSStatusBlock;
import com.monstarmike.tlmreader.datablock.HeaderBlock;
import com.monstarmike.tlmreader.datablock.HeaderNameBlock;
import com.monstarmike.tlmreader.datablock.JetCatBlock;
import com.monstarmike.tlmreader.datablock.PowerBoxBlock;
import com.monstarmike.tlmreader.datablock.RxBlock;
import com.monstarmike.tlmreader.datablock.ServoDataBlock;
import com.monstarmike.tlmreader.datablock.StandardBlock;
import com.monstarmike.tlmreader.datablock.TemperatureBlock;
import com.monstarmike.tlmreader.datablock.VarioBlock;
import com.monstarmike.tlmreader.datablock.VoltageBlock;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;
import gde.utils.TimeLine;
import gde.utils.WaitTimer;

public class TlmReader {
	
	final static int		TIME_LIMIT_MS	= 5000;
	final static Logger	log						= Logger.getLogger(TlmReader.class.getName());

	/**
	 * Spektrum binary log files reader class to parse TLM files
	 */
	public TlmReader() {
		
	}

	/**
	 * merge method to integrate raw data values into normalized output data points
	 * @param measurementRawValues input values from DataBlock
	 * @param points output data points
	 * @param destPos 
	 * @param length
	 * @param recordSet to be reset min/max with start values
	 * @param isResetMinMax supported data block index
	 * @param zeroValueIndex value to be use to detect valid data
	 */
	private static boolean mergeRawData(List<Integer> measurementRawValues, int[] points, int destPos, int length, RecordSet recordSet, boolean isResetMinMax, int zeroValueIndex) {
		boolean isReset = isResetMinMax;
		int[] srcValues = new int[measurementRawValues.size()];
		for (int i=0; i < measurementRawValues.size(); ++i)
			srcValues[i] = measurementRawValues.get(i) * 1000;
		
		if (!isResetMinMax && measurementRawValues.get(zeroValueIndex) != 0) {
			for (int i = destPos, j = 0; i < destPos+length; ++i, ++j) 
				recordSet.get(i).setMinMax(srcValues[j], srcValues[j]);
			isReset = true;
		}

		System.arraycopy(srcValues, 0, points, destPos, length);
		return isReset;
	}

	/**
	 * merge method to integrate raw data values into normalized output data points
	 * @param measurementRawValues input values from DataBlock
	 * @param points output data points
	 * @param destPos 
	 * @param length
	 * @param recordSet to be reset min/max with start values
	 * @param isResetMinMax supported data block index
	 * @param zeroValueIndex value to be use to detect valid data
	 */
	private static boolean mergeGPSRawData(List<Integer> measurementRawValues, int[] points, int destPos, int length, RecordSet recordSet, boolean isResetMinMax, int zeroValueIndex) {
		boolean isReset = isResetMinMax;
		int[] srcValues = new int[measurementRawValues.size()-1];
		for (int i=0; i < measurementRawValues.size()-1; ++i)
			if (i == 1 || i == 2)
				srcValues[i] = measurementRawValues.get(i);
			else
				srcValues[i] = measurementRawValues.get(i) * 1000;

		if (!isResetMinMax && measurementRawValues.get(zeroValueIndex) != 0) {
			for (int i = destPos, j = 0; i < destPos+length; ++i, ++j) 
				recordSet.get(i).setMinMax(srcValues[j], srcValues[j]);
			isReset = true;
		}

		System.arraycopy(srcValues, 0, points, destPos, length);
		return isReset;
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param fileName
	 * @return
	 */
	protected static String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * @param selectedImportFile
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	public static RecordSet read(String selectedImportFile) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		TLMReader reader = new TLMReader();
		RecordSet tmpRecordSet = null;
		MenuToolBar menuToolBar = SpektrumAdapter.application.getMenuToolBar();
		SpektrumAdapter device = (SpektrumAdapter) SpektrumAdapter.application.getActiveDevice();
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(new File(selectedImportFile).getName());
		Channel channel = null;
		int channelNumber = SpektrumAdapter.analyser.getActiveChannel().getNumber();
		
		if (new File(selectedImportFile).exists()) {
			String modelName = "???";
			int index = 0;
			List<IFlight> flights = new ArrayList<>();
			try {
				flights = reader.parseFlightDefinitions(selectedImportFile);
				if (log.isLoggable(Level.FINE)) 
					log.log(Level.FINE, "found " + flights.size() + " flight sessions");
			}
			catch (Exception e) {
				DataExplorer.getInstance().openMessageDialogAsync(e.getMessage());
				log.log(Level.SEVERE, e.getMessage(),e);
			}
			String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date().getTime()); //$NON-NLS-1$
			String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date().getTime()); //$NON-NLS-1$
			channel = SpektrumAdapter.channels.get(channelNumber);
			channel.setFileDescription(SpektrumAdapter.application.isObjectoriented() ? date + GDE.STRING_BLANK + SpektrumAdapter.application.getObjectKey() : date);
			
//										//print all measurement names										
//										System.out.println(new StandardBlock(new byte[25], new HeaderRpmBlock(new byte[25])).getMeasurementNames().toString());
//										System.out.println(new RxBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new VarioBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AltitudeBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AltitudeZeroBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new VoltageBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new CurrentBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new TemperatureBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AirspeedBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(GPSCollectorBlock.getInstance().getMeasurementNames().toString());
//										System.out.println(new FlightPackBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new EscBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new PowerBoxBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new JetCatBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new GForceBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new ServoDataBlock(new byte[25]).getMeasurementNames().toString());
			if (log.isLoggable(Level.TIME)) 
				log.log(Level.TIME, "read flight definitions time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			for (IFlight flight : flights) {
				if (flight.getDuration_ms() > TIME_LIMIT_MS || flight.getNumberOfDataBlocks() > 10 || flight.getHeaderBlocks().size() > 0) {
					if (log.isLoggable(Level.INFO))  
						log.log(Level.INFO, String.format("flight.getDuration() = %d ms", flight.getDuration_ms()));
					 
					Flight currentFlight = reader.parseFlight(selectedImportFile, index);

					for (HeaderBlock header : currentFlight.getHeaderBlocks()) {
						if (header instanceof HeaderNameBlock) {
							if (((HeaderNameBlock) header).getModelName().length() > 3) modelName = ((HeaderNameBlock) header).getModelName();
						}
//													else if (header instanceof HeaderRxBlock)
//														System.out.println("isSpectrumTelemetrySystem=" + ((HeaderRxBlock) header).isSpectrumTelemetrySystem());
//													else if (header instanceof HeaderDataBlock) {
//														System.out.println("SensorTypeEnabled=" + ((HeaderDataBlock) header).getSensorTypeEnabled());
//														System.out.println("isTerminatingBlock=" + ((HeaderDataBlock) header).isTerminatingBlock());
//													}
//													else
//														System.out.println(header.getClass().getSimpleName() + " - " + header.toString());
					}

					if (currentFlight.getDuration_ms() > TIME_LIMIT_MS) {
						if (log.isLoggable(Level.INFO)) 
							log.log(Level.INFO, String.format("model %s flight %d duration() = %s", modelName, index, TimeLine.getFomatedTimeWithUnit(currentFlight.getDuration_ms())));
						 
							List<DataBlock> dataBlocks = currentFlight.getDataBlocks();
						if (log.isLoggable(Level.INFO)) 
							log.log(Level.INFO, "current flight contains " + dataBlocks.size() + " DataBlocks, and " + currentFlight.getHeaderBlocks().size() + " headerBlocks");
						
						currentFlight.removeRedundantDataBlocks();
						
						int recordSetNumber = SpektrumAdapter.channels.get(1).maxSize() + 1;
						long numberDatablocks = dataBlocks.size() + 1;
						int progressIndicator = (int) (numberDatablocks / 5);
						GDE.getUiNotification().setProgress(0);
						int indexDataBlock = 0;
						recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
						tmpRecordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
						channel.put(recordSetName, tmpRecordSet);
						tmpRecordSet = channel.get(recordSetName);
						tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
						//tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
						int[] points = new int[device.getNumberOfMeasurements(channelNumber)];
						long timeOffset = -1;
						//recordSet initialized and ready to add data
						
						//1=Standard 2=Rx 3=Vario 4=Altitude 5=AltitudeZero 6=Voltage 7=Current 8=Temperature 9=AirSpeed
						//10=GPS	11=FlightPack 12=ESC 13=PowerBox 14=JetCat 15=GForce 16=Servo
						int sizeSupportedDataBlockTypes = 16;
						boolean[] isResetMinMax = new boolean[sizeSupportedDataBlockTypes];

						Iterator<DataBlock> iterator = dataBlocks.iterator();
						while (iterator.hasNext()) {
							DataBlock data = iterator.next();
						//for (DataBlock data : dataBlocks) {
							++indexDataBlock;
							if (data instanceof StandardBlock) {
								//System.out.println(((StandardBlock) data).toString());
								//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
								isResetMinMax[0] = mergeRawData(((StandardBlock) data).getMeasurementValues(), points, 0, 5, tmpRecordSet, isResetMinMax[0], 2);
							}
							else if (data instanceof RxBlock) {
								//System.out.println(((RxBlock) data).toString());
								//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
								isResetMinMax[1] = mergeRawData(((RxBlock) data).getMeasurementValues(), points, 5, 7, tmpRecordSet, isResetMinMax[1], 6);
							}
							else if (data instanceof VarioBlock) {
								//System.out.println(((VarioBlock) data).toString());
								//Vario 12=Altitude V, 13=Climb V
								mergeRawData(((VarioBlock) data).getMeasurementValues(), points, 12, 2, tmpRecordSet, true, -1);
							}
							//primitive data blocks
							else if (data instanceof AltitudeBlock) {
								//System.out.println(((AltitudeBlock) data).toString());
								//Altitude	14=Altitude A
								mergeRawData(((AltitudeBlock) data).getMeasurementValues(), points, 14, 1, tmpRecordSet, true, -1);
							}
							else if (data instanceof AltitudeZeroBlock) {
								//System.out.println(((AltitudeZeroBlock) data).toString());
								//AltitudeZero 15=Altitude Offset
								mergeRawData(((AltitudeZeroBlock) data).getMeasurementValues(), points, 15, 1, tmpRecordSet, true, -1);
							}
							else if (data instanceof VoltageBlock) {
								//System.out.println(((VoltageBlock) data).toString());
								//Voltage 16=Voltage V
								isResetMinMax[5] = mergeRawData(((VoltageBlock) data).getMeasurementValues(), points, 16, 1, tmpRecordSet, isResetMinMax[5], 0);
							}
							else if (data instanceof CurrentBlock) {
								//System.out.println(((CurrentBlock) data).toString());
								//Current 17=Current C
								mergeRawData(((CurrentBlock) data).getMeasurementValues(), points, 17, 1, tmpRecordSet, true, -1);
							}
							else if (data instanceof TemperatureBlock) {
								//System.out.println(((TemperatureBlock) data).toString());
								//Temperature 18=Temperature T
								isResetMinMax[7] = mergeRawData(((TemperatureBlock) data).getMeasurementValues(), points, 18, 1, tmpRecordSet, isResetMinMax[7], 0);
							}
							else if (data instanceof AirspeedBlock) {
								//System.out.println(((AirspeedBlock) data).toString());
								//AirSpeed 19=AirSpeed
								mergeRawData(((AirspeedBlock) data).getMeasurementValues(), points, 19, 1, tmpRecordSet, true, -1);
							}
							//other important data blocks
							else if (data instanceof GPSLocationBlock) {
								//System.out.println(((GPSLocationBlock) data).toString());
								GPSCollectorBlock.getInstance().updateLocation((GPSLocationBlock) data);
								if (GPSCollectorBlock.getInstance().isUpdated()) {
									//System.out.println(GPSCollectorBlock.getInstance().toString());
									//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
									isResetMinMax[9] = mergeGPSRawData(GPSCollectorBlock.getInstance().getMeasurementValues(), points, 20, 8, tmpRecordSet, isResetMinMax[9], 1);
								}
							}
							else if (data instanceof GPSStatusBlock) {
								//System.out.println(((GPSStatusBlock) data).toString());
								GPSCollectorBlock.getInstance().updateStatus((GPSStatusBlock) data);
								if (GPSCollectorBlock.getInstance().isUpdated()) {
									//System.out.println(GPSCollectorBlock.getInstance().toString());
									isResetMinMax[9] = mergeGPSRawData(GPSCollectorBlock.getInstance().getMeasurementValues(), points, 20, 8, tmpRecordSet, isResetMinMax[9], 1);
								}
							}
							else if (data instanceof FlightPackBlock) {
								//System.out.println(((FlightPackBlock) data).toString());
								//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
								mergeRawData(((FlightPackBlock) data).getMeasurementValues(), points, 29, 6, tmpRecordSet, true, -1);
							}
							else if (data instanceof EscBlock) {
								//System.out.println(((EscBlock) data).toString());
								//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
								isResetMinMax[11] = mergeRawData(((EscBlock) data).getMeasurementValues(), points, 35, 9, tmpRecordSet, isResetMinMax[11], 1);
							}
							else if (data instanceof PowerBoxBlock) {
								//System.out.println(((PowerBoxBlock) data).toString());
								//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
								isResetMinMax[12] = mergeRawData(((PowerBoxBlock) data).getMeasurementValues(), points, 44, 5, tmpRecordSet, isResetMinMax[12], 0);
							}
							else if (data instanceof JetCatBlock) {
								//System.out.println(((JetCatBlock) data).toString());
								//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
								isResetMinMax[13] = mergeRawData(((JetCatBlock) data).getMeasurementValues(), points, 49, 7, tmpRecordSet, isResetMinMax[13], 2);
							}
							else if (data instanceof GForceBlock) {
								//System.out.println(((GForceBlock) data).toString());
								//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
								mergeRawData(((GForceBlock) data).getMeasurementValues(), points, 56, 7, tmpRecordSet, true, -1);
							}
							else if (data instanceof ServoDataBlock) {
								//System.out.println(((ServoDataBlock) data).toString());
								//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
								mergeRawData(((ServoDataBlock) data).getMeasurementValues(), points, 63, 20, tmpRecordSet, true, -1);
							}
							else
								log.log(Level.WARNING, data.toString());
							

							if (timeOffset == -1) timeOffset = data.getTimestamp();

							tmpRecordSet.addPoints(points, (data.getTimestamp() - timeOffset) * 10.0);
							if (indexDataBlock % progressIndicator == 0) 
								GDE.getUiNotification().setProgress((int) (indexDataBlock * 100 / numberDatablocks));
							
							iterator.remove();
						}
						
						if (GDE.isWithUi() && tmpRecordSet != null) {
							device.makeInActiveDisplayable(tmpRecordSet);

							// write filename after import to record description
							tmpRecordSet.descriptionAppendFilename(new File(selectedImportFile).getName());
							channel.applyTemplate(recordSetName, false);
							menuToolBar.updateChannelSelector();
							menuToolBar.updateRecordSetSelectCombo();
							SpektrumAdapter.channels.switchChannel(channelNumber, recordSetName);
							GDE.getUiNotification().setProgress(100);
						}
						if (log.isLoggable(Level.TIME)) 
							log.log(Level.TIME, String.format("read flight %d time = %s", index, StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime)))); //$NON-NLS-1$ //$NON-NLS-2$
						WaitTimer.delay(100); //enable refresh
					}
					++index;
				}
			}
		}
		if (log.isLoggable(Level.TIME)) 
			log.log(Level.TIME, "overall read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
		
		return tmpRecordSet;
	}

}
