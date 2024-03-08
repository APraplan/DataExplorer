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
    
    Copyright (c) 2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.peaktech;

import java.io.FileNotFoundException;
import java.util.HashMap;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.exception.ApplicationConfigurationException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * VC820 device class
 * @author Winfried Brügmann
 */
public class PeakTechUSB extends PeakTech {
	
	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public PeakTechUSB(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PeakTechUSB(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * get measurement info (type, symbol, unit)
	 * @param buffer
	 * @return measurement unit as string
	 */
	public HashMap<String, String> getMeasurementInfo(byte[] buffer, HashMap<String, String> measurementInfo) {		
		//Byte 5 - 	Status Byte 3 µ,m,k,M,continuity,diode,%,Z4
		//Byte 6 -	Status Byte 4 V,A,Ω,hFE,Hz,F,°C,°F

		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "buffer : " + StringHelper.byte2Hex2CharString(buffer, buffer.length));
		}
		
		String unit = ""; //$NON-NLS-1$
		switch (buffer[5] & 0xFF) {
		case 128: //µ
			unit = "µ";
			break;
		case 64: //m
			unit = "m";
			break;
		case 32: //k
			unit = "k";
			break;
		case 16: //M
			unit = "M";
			break;
		case 2: //%
			unit = "%";
			break;
		default:	
		}
		
		switch (buffer[6] & 0xFF) {
		case 128: //V
			unit = unit + "V";
			break;
		case 64: //A
			unit = unit + "A";
			break;
		case 32: //Ω
			unit = unit + "Ω";
			break;
		case 16: //hFE
			unit = unit + "hFE";
			break;
		case 8: //Hz
			unit = unit + "Hz";
			break;
		case 4: //F
			unit = unit + "F";
			break;
		case 2: //°C
			unit = unit + "°C";
			break;
		case 1: //°F
			unit = unit + "°F";
			break;
		default:			
		}
		
		measurementInfo.put(PeakTech.INPUT_UNIT, unit);

		String typeSymbol = Messages.getString(MessageIds.GDE_MSGT1500);
		if (unit.contains("V")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1501);
		else if (unit.endsWith("A")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1503);
		else if (unit.endsWith("Ω")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1504);
		else if (unit.endsWith("F")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1505);
		else if (unit.endsWith("Hz")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1506);
		else if (unit.endsWith("°C")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1507);
		else if (unit.endsWith("%")) typeSymbol = Messages.getString(MessageIds.GDE_MSGT1537); //$NON-NLS-1$

		try {
			measurementInfo.put(PeakTech.INPUT_TYPE, typeSymbol.split(" ")[0]); //$NON-NLS-1$
			measurementInfo.put(PeakTech.INPUT_SYMBOL, typeSymbol.split(" ")[1]); //$NON-NLS-1$
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}

		return measurementInfo;
	}

	/**
	 * get the measurement mode
	 * @param buffer
	 * @return the measurement mode key
	 */
	public String getMode(byte[] buffer) {
		//Byte 3 - 	Status Byte 1 0,0,auto,AC,DC,rel,hold,bpn
		//Byte 4 - 	Status Byte 2 Z1,Z2,max,min,apo,batt,n,Z3
		String mode = "";
		if ((buffer[3] & 0x20) > 0)
			mode = Messages.getString(MessageIds.GDE_MSGT1511);
		else
			mode = Messages.getString(MessageIds.GDE_MSGT1510);
		
		switch (buffer[7] & 0xFF) {
		case 16: 
			mode = "AC";
			break;
		case 8: 
			mode = "DC";
			break;
		default:
		}
		return mode;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {	
		//Byte 0 - Sign & Decimal position)
		//Byte 1..2: - 7 segment display numbers
		//Byte 3 - 	Status Byte 1 0,0,auto,AC,DC,rel,hold,bpn
		//Byte 4 - 	Status Byte 2 Z1,Z2,max,min,apo,batt,n,Z3
		//Byte 5 - 	Status Byte 3 µ,m,k,M,continuity,diode,%,Z4
		//Byte 6 -	Status Byte 4 V,A,Ω,hFE,Hz,F,°C,°F
		//Byte 7 -	Bar graph
		
		//Byte 6 - 	0=xxxx, 1=x.xxx, 2=xx.xx, 3+4=xxx.x

		log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
		log.log(Level.OFF, String.format("Input: %c%c%c%c%c", (dataBuffer[0] & 0x40) > 0 ? '-' : ' ', dataBuffer[1] & 0xF0 >> 4, dataBuffer[1] & 0x0F, dataBuffer[2] & 0xF0 >> 4, dataBuffer[2] & 0x0F));

		points[0] = Integer.valueOf(String.format("%c%c%c%c%c", (dataBuffer[0] & 0x40) > 0 ? '-' : ' ', dataBuffer[1] & 0xF0 >> 4, dataBuffer[1] & 0x0F, dataBuffer[2] & 0xF0 >> 4, dataBuffer[2] & 0x0F)).intValue();
		switch (dataBuffer[0] & 0x0F) {
		default:
			points[0] *= 1000; 
			break;
		case 1: //1=x.xxx
			points[0] *= 100; 
			break;
		case 2: //2=xx.xx
			points[0] *= 10; 
			break;
		case 3: //3+4=xxx.x
		case 4: //3+4=xxx.x
			points[0] *= 1; 
			break;
		}			
		return points;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.getDialog().dataGatherThread = new GathererThreadRS232(this.application, this, this.serialPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.serialPort.isConnected()) {
								this.getDialog().dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
						if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.getDialog().dataGatherThread.stopDataGatheringThread(false);
				}
				if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
	}
}
