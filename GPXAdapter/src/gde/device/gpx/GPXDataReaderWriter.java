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
package gde.device.gpx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read and write geo points exchange format data
 * @author Winfried Brügmann
 */
public class GPXDataReaderWriter {
	static Logger										log					= Logger.getLogger(GPXDataReaderWriter.class.getName());

	static String										lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat						df3					= new DecimalFormat("0.000");														//$NON-NLS-1$
	static StringBuffer							sb;

	final static DataExplorer				application	= DataExplorer.getInstance();
	final static Channels						channels		= Channels.getInstance();

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
		RecordSet channelRecordSet = null;
		MenuToolBar menuToolBar = GPXDataReaderWriter.application.getMenuToolBar();
		GDE.getUiNotification().setProgress(0);
		GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.gpx.MessageIds.GDE_MSGT1776) + filePath);

		try {
			if (channelConfigNumber == null)
				activeChannel = GPXDataReaderWriter.channels.getActiveChannel();
			else
				activeChannel = GPXDataReaderWriter.channels.get(channelConfigNumber);
			channelConfigNumber = GPXDataReaderWriter.channels.getActiveChannelNumber();

			if (activeChannel != null) {
				if (GPXDataReaderWriter.log.isLoggable(Level.FINE))
					GPXDataReaderWriter.log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + channelConfigNumber); //$NON-NLS-1$

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
				recordSetName = recordNameExtend.length() > 2 ? recordSetName + GDE.STRING_BLANK_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET : recordSetName;

				GDE.getUiNotification().setProgress(30);

				parseInputXML(filePath, device, activeChannel, recordSetName);
				channelRecordSet = activeChannel.get(recordSetName);
				if (channelRecordSet != null) {
					//write filename after import to record description
					channelRecordSet.descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1));
				}

				GDE.getUiNotification().setProgress(100);

				if (GDE.isWithUi()) {
					Channels.getInstance().switchChannel(activeChannel.getName());
					activeChannel.switchRecordSet(recordSetName);
					device.updateVisibilityStatus(channelRecordSet, true);
					channelRecordSet.updateVisibleAndDisplayableRecordsForTable();

					menuToolBar.updateChannelSelector();
					menuToolBar.updateRecordSetSelectCombo();
				}
			}
		}
		catch (FileNotFoundException e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
			GPXDataReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
			GPXDataReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
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
			GPXDataReaderWriter.log.log(Level.WARNING, msg, e);
			GPXDataReaderWriter.application.openMessageDialog(msg);
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

		return channelRecordSet;
	}

	public static void parseInputXML(final String localUnixFullQualifiedPath, final IDevice device, final Channel activeChannel, final String recordSetName) throws ParserConfigurationException, SAXException, IOException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		SAXParser saxParser = factory.newSAXParser();
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$ //$NON-NLS-2$
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "file:/resource/gpx.xsd"); //$NON-NLS-1$ //$NON-NLS-2$

		DefaultHandler handler = new DefaultHandler() {
			boolean										isDescription						= false;
			boolean										isDescription2					= false;
			boolean										isElevation							= false;
			boolean										isTime									= false, isDateSet = false;
			boolean										isNumSatelites					= false;
			boolean										isSpeed									= false;
			Boolean										isExtensionFirstCalled	= null;
			boolean										isExtension							= false;
			final int[]								date										= new int[3];
			final int[]								time										= new int[3];
			long											timeStamp								= 0, startTimeStamp = 0;
			final Map<String, String>	tmpPoints								= new LinkedHashMap<String, String>();
			final Vector<String>			extensionNames					= new Vector<String>();
			String										extensionName						= GDE.STRING_EMPTY;
			int[]											points									= new int[device.getNumberOfMeasurements(activeChannel.getNumber())];
			int												pointsIndex							= 0;
			RecordSet									activeRecordSet;
			String 										recordSetDescription 		= GDE.STRING_EMPTY;

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

				if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "Start Element :" + qName); //$NON-NLS-1$
				if (qName != null && qName.length() > 1) {
					if (qName.equalsIgnoreCase("text")) //$NON-NLS-1$
						this.isDescription = true; //<text>MikroKopter</text>
					else if (qName.equalsIgnoreCase("desc")) //$NON-NLS-1$
						this.isDescription2 = true;//<desc>FC HW:2.1 SW:0.88e + NC HW:2.0 SW:0.28i</desc>

					// <trkpt lat="+41.0334244" lon="-73.5230532">
					else if (qName.equalsIgnoreCase("trkpt")) { //$NON-NLS-1$
						if (attributes.getLength() == 2) {
							this.tmpPoints.put("lat", attributes.getValue("lat")); //lat="+41.0334244" //$NON-NLS-1$ //$NON-NLS-2$
							this.tmpPoints.put("lon", attributes.getValue("lon")); //lon="-73.5230532" //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					else if (qName.equalsIgnoreCase("ele")) //$NON-NLS-1$
						this.isElevation = true;//<ele>12.863</ele>
					else if (qName.equalsIgnoreCase("time")) //$NON-NLS-1$
						this.isTime = true;//<time>2012-04-19T15:37:33Z</time>
					else if (qName.equalsIgnoreCase("sat")) //$NON-NLS-1$
						this.isNumSatelites = true;//<sat>10</sat>
					else if (qName.equalsIgnoreCase("speed")) //$NON-NLS-1$
						this.isSpeed = true;//<speed>10.1</speed>

					//<extensions>
					else if (qName.equalsIgnoreCase("extensions")) { //$NON-NLS-1$
						this.isExtension = true;
						if (this.isExtensionFirstCalled == null) this.isExtensionFirstCalled = true;
					}
					else if (this.isExtension) {
						this.extensionName = qName;
					}

				}

				//				<extensions>
				//				<Altimeter>252,' '</Altimeter>
				//				<Variometer>89</Variometer>
				//				<Course>297</Course>
				//				<GroundSpeed>175</GroundSpeed>
				//				<VerticalSpeed>508</VerticalSpeed>
				//				<FlightTime>3</FlightTime>
				//				<Voltage>15.8</Voltage>
				//				<Current>68.9</Current>
				//				<Capacity>76</Capacity>
				//				<RCQuality>197</RCQuality>
				//				<RCRSSI>0</RCRSSI>
				//				<Compass>094,095</Compass>
				//				<NickAngle>006</NickAngle>
				//				<RollAngle>000</RollAngle>
				//				<MagnetField>102</MagnetField>
				//				<MagnetInclination>64,-4</MagnetInclination>
				//				<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
				//				<BL_Temperature>25,27,20,27,26,24,0,0,0,0,0,0</BL_Temperature>
				//				<AvaiableMotorPower>255</AvaiableMotorPower>
				//				<FC_I2C_ErrorCounter>000</FC_I2C_ErrorCounter>
				//				<AnalogInputs>21,12,24,760</AnalogInputs>
				//				<NCFlag>0x82</NCFlag>
				//				<Servo>153,128,0</Servo>
				//				<WP>----,0,13,0</WP>
				//				<FCFlags2>0xc3,0x18</FCFlags2>
				//				<ErrorCode>000</ErrorCode>
				//				<TargetBearing>090</TargetBearing>
				//				<TargetDistance>12</TargetDistance>
				//				<RCSticks>0,0,0,30,1,127,1,153,1,1,1,1</RCSticks>
				//				<GPSSticks>-77,-14,0,'D'</GPSSticks>
				//				</extensions>

			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "End Element :" + qName); //$NON-NLS-1$
				if (qName.equalsIgnoreCase("trkpt")) { //$NON-NLS-1$
					this.pointsIndex = 0;
					this.points[this.pointsIndex++] = (int) (Double.valueOf(this.tmpPoints.get("lat").replace(GDE.STRING_PLUS, GDE.STRING_EMPTY).trim()) * 1000000); //$NON-NLS-1$
					this.points[this.pointsIndex++] = (int) (Double.valueOf(this.tmpPoints.get("lon").replace(GDE.STRING_PLUS, GDE.STRING_EMPTY).trim()) * 1000000); //$NON-NLS-1$
					this.points[this.pointsIndex++] = this.tmpPoints.get("ele") != null ? (int) (Double.valueOf(this.tmpPoints.get("ele").trim()) * 1000) : 0; //$NON-NLS-1$
					this.points[this.pointsIndex++] = this.tmpPoints.get("sat") != null ? Integer.valueOf(this.tmpPoints.get("sat").trim()) * 1000 : 0; //$NON-NLS-1$
					this.points[this.pointsIndex++] = this.tmpPoints.get("speed") != null ? (int) (Double.valueOf(this.tmpPoints.get("speed").trim()) * 1000) : 0; //$NON-NLS-1$

					if (this.isExtensionFirstCalled != null || this.isExtension == false) {
						if (this.isExtensionFirstCalled == null || this.isExtensionFirstCalled) {

							int measurementSize = device.getNumberOfMeasurements(activeChannel.getNumber());
							if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "measurementSize = " + measurementSize); //$NON-NLS-1$

							Vector<String> vecRecordNames = new Vector<String>();
							//add the 5 always contained records
							for (int i = 0; i < 5; i++) {
								vecRecordNames.add(device.getMeasurementNamesReplacements(activeChannel.getNumber())[i]);
							}
							//ad records from extension declaration
							for (String tmpExtensionName : this.extensionNames) {
								if (GPXAdapter.ignoreMap.get(tmpExtensionName.split(GDE.STRING_BLANK)[0]) == null) {
									String[] values = this.tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
									for (int i = 0; i < values.length && (this.points.length - this.pointsIndex) > 0; i++) {
										//System.out.println(i + " values.lenght " + this.pointsIndex + " - " + (this.points.length - this.pointsIndex));
										String newRecordName = tmpExtensionName + (values.length > 1 ? GDE.STRING_BLANK + (i + 1) : GDE.STRING_EMPTY);
										//System.out.print(activeRecordSet.getRecordNames()[this.pointsIndex] + " -> ");
										vecRecordNames.add(newRecordName);
										//System.out.println(activeRecordSet.getRecordNames()[this.pointsIndex]);
										try {
											this.points[this.pointsIndex] = (int) (Double.valueOf(values[i].trim()) * 1000.0);
										}
										catch (NumberFormatException e) {
											try {
												if (values[i].indexOf('x') >= 0)
													this.points[this.pointsIndex] = Integer.valueOf(values[i].substring(values[i].indexOf('x')+1).trim(), 16)*1000;
											}
											catch (NumberFormatException e1) {
												// ignore and keep existing value
											}
										}
										++this.pointsIndex;
									}
								}
							}

							//build up the record set with variable number of records just fit the sensor data
							String[] recordNames = vecRecordNames.toArray(new String[0]);
							String[] recordSymbols = new String[recordNames.length];
							String[] recordUnits = new String[recordNames.length];
							String referenceName = GDE.STRING_EMPTY;
							int referenceOrdinal = -1;
							for (int i = 0; i < recordNames.length; i++) {
								MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordNames[i]);
								measurement.setName(recordNames[i]);
								recordSymbols[i] = measurement.getSymbol();
								recordUnits[i] = measurement.getUnit();
								if (GPXAdapter.unitMap.size() > 0 && i > 3) {
									if (GPXAdapter.unitMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null)
										measurement.setUnit(recordUnits[i] = GPXAdapter.unitMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]));
									else if (!measurement.getUnit().isEmpty()) measurement.setUnit(GDE.STRING_EMPTY);
								}
								if (GPXAdapter.symbolMap.size() > 0 && i > 3) {
									if (GPXAdapter.symbolMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null)
										measurement.setSymbol(recordUnits[i] = GPXAdapter.symbolMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]));
									else if (!measurement.getSymbol().isEmpty()) measurement.setSymbol(GDE.STRING_EMPTY);
								}
								if (GPXAdapter.factorMap.size() > 0 && i > 3) {
									if (GPXAdapter.factorMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null)
										measurement.setFactor(GPXAdapter.factorMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]));
									else if (measurement.getFactor() != 1.0) measurement.setFactor(1.0);
								}
								if (GPXAdapter.offsetMap.size() > 0 && i > 3) {
									if (GPXAdapter.offsetMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null)
										measurement.setOffset(GPXAdapter.factorMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]));
									else if (measurement.getOffset() != 0) measurement.setOffset(0.0);
								}
								if (GPXAdapter.reductionMap.size() > 0 && i > 3) {
									if (GPXAdapter.reductionMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null)
										measurement.setReduction(GPXAdapter.factorMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]));
									else if (measurement.getReduction() != 0) measurement.setReduction(0.0);
								}
								if (GPXAdapter.syncMap.size() > 0 && i > 3) {
									if (GPXAdapter.syncMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null && recordNames[i].split(GDE.STRING_BLANK).length > 1
											&& recordNames[i].split(GDE.STRING_BLANK)[1] != null && recordNames[i].split(GDE.STRING_BLANK)[1].equals("1")) { //$NON-NLS-1$
										referenceName = recordNames[i].split(GDE.STRING_BLANK)[0];
										referenceOrdinal = i;
									}
									else if (GPXAdapter.syncMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null && recordNames[i].split(GDE.STRING_BLANK)[0].equals(referenceName)
											&& recordNames[i].split(GDE.STRING_BLANK)[1] != null && recordNames[i].split(GDE.STRING_BLANK).length > 1 && !recordNames[i].split(GDE.STRING_BLANK)[1].equals("1")) //$NON-NLS-1$
										device.setMeasurementPropertyValue(activeChannel.getNumber(), i, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value(), DataTypes.INTEGER, referenceOrdinal);
									else
										((DeviceConfiguration) device).removeMeasruementProperty(activeChannel.getNumber(), i, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
								}
								//translate extension names to others, translate
								if (GPXAdapter.languageMap.get(recordNames[i].split(GDE.STRING_BLANK)[0]) != null) {
									measurement.setName(recordNames[i] = GPXAdapter.languageMap.get(recordNames[i].split(GDE.STRING_BLANK)[0])
											+ (recordNames[i].split(GDE.STRING_BLANK).length > 1 ? recordNames[i].substring(recordNames[i].indexOf(GDE.CHAR_BLANK)) : GDE.STRING_BLANK));
								}
							}
							activeRecordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), true, true, true);
							String correctedRecordSetName = activeRecordSet.getName(); // cut/correct length of recordSetName
							activeChannel.put(correctedRecordSetName, activeRecordSet);

							if (GPXDataReaderWriter.log.isLoggable(Level.FINE))
								GPXDataReaderWriter.log.log(Level.FINE, correctedRecordSetName + " created for channel " + activeChannel.getName()); //$NON-NLS-1$
							activeChannel.setActiveRecordSet(correctedRecordSetName);
							activeRecordSet = activeChannel.get(recordSetName);
							activeChannel.applyTemplate(recordSetName, false);

							//shorten points array to fit actual created recordSet
							int[] newPoints = new int[activeRecordSet.size()];
							for (int i = 0; i < newPoints.length; i++) {
								newPoints[i] = points[i];
							}
							points = newPoints;

							this.isExtensionFirstCalled = false;
						}
						else if (!this.isExtensionFirstCalled) {
							for (String tmpExtensionName : this.extensionNames) {
								if (GPXAdapter.ignoreMap.get(tmpExtensionName.split(GDE.STRING_BLANK)[0]) == null) {
									String[] values = this.tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
									for (int i = 0; i < values.length && (this.points.length - this.pointsIndex) > 0; i++) {
										//System.out.println(i + " values.lenght " + this.pointsIndex + " - " + (this.points.length - this.pointsIndex));
										try {
											this.points[this.pointsIndex] = (int) (Double.valueOf(values[i].trim()) * 1000.0);
										}
										catch (NumberFormatException e) {
											try {
												if (values[i].indexOf('x') >= 0)
													this.points[this.pointsIndex] = Integer.valueOf(values[i].substring(values[i].indexOf('x')+1).trim(), 16) * 1000;
											}
											catch (NumberFormatException e1) {
												// ignore and keep existing value
											}
										}
										++this.pointsIndex;
									}
								}
							}
						}
					}
					try {
						if (this.startTimeStamp == 0) this.startTimeStamp = this.timeStamp;
						if (GPXDataReaderWriter.log.isLoggable(Level.FINER)) GPXDataReaderWriter.log.log(Level.FINER, GDE.STRING_EMPTY + (this.timeStamp - this.startTimeStamp) * 1.0);
						//System.out.println(StringHelper.intArrayToString(this.points));
						if (activeRecordSet != null)
							activeRecordSet.addPoints(this.points, (this.timeStamp - this.startTimeStamp) * 1.0);
					}
					catch (DataInconsitsentException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void characters(char ch[], int start, int length) throws SAXException {
				String values = new String(ch, start, length);
				if (!values.contains("\n") && !values.contains("\r")) { //$NON-NLS-1$ //$NON-NLS-2$
					if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, values);
					if (this.isDescription) {
						recordSetDescription = GDE.LINE_SEPARATOR + new String(ch, start, length);
						this.isDescription = false;
					}
					else if (this.isDescription2) {
						recordSetDescription = recordSetDescription.length() > 1
							? recordSetDescription + GDE.STRING_COLON + GDE.STRING_BLANK + new String(ch, start, length)
							: GDE.LINE_SEPARATOR + new String(ch, start, length);
						this.isDescription2 = false;
					}
					else if (this.isElevation) {
						this.tmpPoints.put("ele", new String(ch, start, length)); //<ele>12.863</ele> //$NON-NLS-1$
						this.isElevation = false;
					}
					else if (this.isTime) {
						String dateTime = new String(ch, start, length);//<time>2012-04-19T15:37:33Z</time>
						if (!this.isDateSet) {
							String strDate = dateTime.split("T")[0]; //$NON-NLS-1$
							this.date[0] = Integer.parseInt(strDate.substring(0, 4));
							this.date[1] = Integer.parseInt(strDate.substring(5, 7));
							this.date[2] = Integer.parseInt(strDate.substring(8, 10));
						}
						String strValueTime = dateTime.split("T|Z")[1]; //$NON-NLS-1$
						this.time[0] = Integer.parseInt(strValueTime.substring(0, 2));
						this.time[1] = Integer.parseInt(strValueTime.substring(3, 5));
						this.time[2] = Integer.parseInt(strValueTime.substring(6, 8));
						GregorianCalendar calendar = new GregorianCalendar(this.date[0], this.date[1] - 1, this.date[2], this.time[0], this.time[1], this.time[2]);
						this.timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.CHAR_DOT) + 1)) : 0);
						if (!this.isDateSet && this.isExtensionFirstCalled != null) {
							String description = activeRecordSet.getRecordSetDescription();
							activeRecordSet.setRecordSetDescription(description.substring(0, description.indexOf(GDE.CHAR_COLON) + 2) + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", this.startTimeStamp) + recordSetDescription); //$NON-NLS-1$
							activeRecordSet.setStartTimeStamp(this.startTimeStamp);
							this.isDateSet = true;
						}
						this.isTime = false;
					}
					else if (this.isNumSatelites) {
						this.tmpPoints.put("sat", new String(ch, start, length)); //<sat>10</sat> //$NON-NLS-1$
						this.isNumSatelites = false;
					}
					else if (this.isSpeed) {
						this.tmpPoints.put("speed", new String(ch, start, length)); //<speed>10.1</speed> //$NON-NLS-1$
						this.isSpeed = false;
					}
					else if (this.isExtension && this.extensionName.length() > 3) {
						if (isExtensionFirstCalled != null && isExtensionFirstCalled)
							this.extensionNames.add(this.extensionName);
						this.tmpPoints.put(this.extensionName, new String(ch, start, length)); //<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
					}
				}
			}
		};
		saxParser.parse(localUnixFullQualifiedPath, handler);

		return;
	}

}
