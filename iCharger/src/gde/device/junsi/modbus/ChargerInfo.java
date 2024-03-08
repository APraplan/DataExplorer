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
package gde.device.junsi.modbus;

import gde.io.DataParser;
import gde.utils.StringHelper;

public class ChargerInfo {

	private short							deviceID;
	private byte[]						deviceSN	= new byte[12];
	private short							softwareVersion;
	private short							hardwareVersion;
	private short							systemMemoryLength;
	private short							programMemoryLength;
	private short							status;								//Bit0-run flag, Bit1-error flag, Bit2-control status flag, Bit3-run status flag, Bit4-dialog box status flag, Bit5-cell voltage flag, Bit6-balance flag 
	private final static int	size = 24;						//size in byte / 2 size in short

	public ChargerInfo(final byte[] chargerInfoBuffer) {
		this.deviceID = DataParser.parse2Short(chargerInfoBuffer[0], chargerInfoBuffer[1]);
		System.arraycopy(chargerInfoBuffer, 2, this.deviceSN, 0, this.deviceSN.length);
		this.softwareVersion = DataParser.parse2Short(chargerInfoBuffer[14], chargerInfoBuffer[15]);
		this.hardwareVersion = DataParser.parse2Short(chargerInfoBuffer[16], chargerInfoBuffer[17]);
		this.systemMemoryLength = DataParser.parse2Short(chargerInfoBuffer[18], chargerInfoBuffer[19]);
		this.programMemoryLength = DataParser.parse2Short(chargerInfoBuffer[20], chargerInfoBuffer[21]);
		this.status = DataParser.parse2Short(chargerInfoBuffer[22], chargerInfoBuffer[23]);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(":\n");
		sb.append(String.format("deviceID \t\t= %d", this.deviceID)).append("\n");
		sb.append(String.format("deviceSN \t\t= %s", this.getDeviceSnString())).append("\n");
		sb.append(String.format("softwareVersion \t= %d", this.softwareVersion)).append("\n");
		//sb.append(String.format("softwareVersion \t= %s", this.getSoftwareVersionString())).append("\n");
		sb.append(String.format("hardwareVersion \t= %d", this.hardwareVersion)).append("\n");
		//sb.append(String.format("hardwareVersion \t= %s", this.getHardwareVersionString())).append("\n");
		sb.append(String.format("systemMemoryLength \t= %d", this.systemMemoryLength)).append("\n");
		sb.append(String.format("programMemoryLength \t= %d", this.programMemoryLength)).append("\n");
		sb.append(this.getStatusString());
		return sb.toString();
	}

	/**
	 * @return status as string Bit0-run flag, Bit1-error flag, Bit2-control status flag, Bit3-run status flag, Bit4-dialog box status flag, Bit5-cell voltage flag, Bit6-balance flag 
	 */
	public String getStatusString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("status \t\t\t= %s", StringHelper.printBinary((byte)(status & 0xFF), false)));
		//Bit0-run flag, Bit1-error flag, Bit2-control status flag, Bit3-run status flag, Bit4-dialog box status flag, Bit5-cell voltage flag, Bit6-balance flag 
		if ((this.status & 0x40) != 0)
			sb.append(" - balance");
		if ((this.status & 0x20) != 0)
			sb.append(" - cell-voltage");
		if ((this.status & 0x10) != 0)
			sb.append(" - dialog-box");
		if ((this.status & 0x08) != 0)
			sb.append(" - run-status");
		if ((this.status & 0x04) != 0)
			sb.append(" - control");
		if ((this.status & 0x02) != 0)
			sb.append(" - error");
		if ((this.status & 0x01) != 0)
			sb.append(" - run");
		return sb.toString();
	}
	
	public String getHardwareVersionString(boolean isDuo) {
		String hardwareVersionString = "V" + this.hardwareVersion;
		try {
			hardwareVersionString =	String.format("%s.%s%d", hardwareVersionString.substring(0, 2), hardwareVersionString.substring(2, 3), Integer.parseInt(hardwareVersionString.substring(3)));
		}
		catch (RuntimeException e) {
			// ignore
		}
		return hardwareVersionString;
	}

	public String getSoftwareVersionString(boolean isDuo) {
		String softwareVersionString = "V" + this.softwareVersion;
		try {
			softwareVersionString =	String.format("%s.%s%d", softwareVersionString.substring(0, 2), softwareVersionString.substring(2, 3), Integer.parseInt(softwareVersionString.substring(3)));
		}
		catch (RuntimeException e) {
			// ignore
		}
		return softwareVersionString;
	}
	
	public String getSystemInfo(boolean isDuo) {
		return String.format("Firmware:%s; Hardware:%s; SN:%s", this.getSoftwareVersionString(isDuo), this.getHardwareVersionString(isDuo), this.getDeviceSnString());
	}

	/**
	 * @return the size
	 */
	public static int getSize() {
		return size;
	}

	/**
	 * @return the deviceID
	 */
	public short getDeviceID() {
		return deviceID;
	}

	/**
	 * @return the deviceSN
	 */
	public byte[] getDeviceSN() {
		return deviceSN;
	}

	/**
	 * @return the deviceSN
	 */
	public String getDeviceSnString() {
		return new String(deviceSN).trim();
	}

	/**
	 * @return the softwareVersion
	 */
	public short getSoftwareVersion() {
		return softwareVersion;
	}

	/**
	 * @return the hardwareVersion
	 */
	public short getHardwareVersion() {
		return hardwareVersion;
	}

	/**
	 * @return the systemMemoryLength
	 */
	public short getSystemMemoryLength() {
		return systemMemoryLength;
	}

	/**
	 * @return the programMemoryLength
	 */
	public short getProgramMemoryLength() {
		return programMemoryLength;
	}

	/**
	 * @return the status
	 */
	public short getStatus() {
		return status;
	}

}
