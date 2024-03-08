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

import java.util.Arrays;

import gde.io.DataParser;
import gde.log.Level;

public class ChargerMemoryHead {
	//public final int[][] MEM_HEAD_DEFAULT	 = new int[][] {7,{0,1,2,3,4,5,6}};

	short								count;
	byte[]							index;	//0-LIST_MEM_MAX

	/**
	 * constructor to create instance from received data
	 * @param memoryHeadBuffer filled by Modbus communication
	 */
	public ChargerMemoryHead(final byte[] memoryHeadBuffer, boolean isDuoOrDX) {
		this.index = new byte[ChargerMemoryHead.getMaxListIndex(isDuoOrDX)];

		this.count = DataParser.parse2Short(memoryHeadBuffer[0], memoryHeadBuffer[1]);
		for (int i = 0; i < this.count; ++i) {
			this.index[i] = memoryHeadBuffer[2 + i];
		}
		if (ChargerDialog.log.isLoggable(Level.INFO)) 
			ChargerDialog.log.log(Level.INFO, String.format("memoryHead index: %s", this.toString())); //$NON-NLS-1$
	}
	
	/**
	 * @param isDuoOrDX
	 * @return maximal list index, size of program memory
	 */
	public static int getMaxListIndex(boolean isDuoOrDX) {
		return isDuoOrDX ? 64 : 32;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(" : \n");
		sb.append(String.format("program memory count = %d", this.count)).append("\n");
		for (int i = 0; i < this.count; ++i) {
			sb.append(this.index[i]).append(", ");
		}
		sb.append("\n");
		return sb.toString();
	}

	public byte[] getAsByteArray(boolean isDuoOrDX) {
		byte[] memHeadBuffer = new byte[ChargerMemoryHead.getSize(isDuoOrDX)];
		memHeadBuffer[0] = (byte) (this.count & 0xFF);
		memHeadBuffer[1] = (byte) (this.count >> 8);
		for (int i = 0; i < this.count; i++) {
			memHeadBuffer[2 + i] = this.index[i];
		}
		return memHeadBuffer;
	}

	final static int size = 17 * 2; //size in byte

	public static int getSize(boolean isDuoOrDX) {
		return (isDuoOrDX ? 64 : 32) + 2 ;
	}

	public short getCount() {
		return this.count;
	}

	public void setCount(short count) {
		this.count = count;
	}

	public byte[] getIndex() {
		return this.index;
	}

	/**
	 * @return next unused index of memory head
	 */
	public short getNextFreeIndex() {
		short nextFreeIndex = -1;
		//find free index in between
		byte[] sortedIndex = new byte[this.count];
		System.arraycopy(this.index, 0, sortedIndex, 0, this.count);
		Arrays.sort(sortedIndex);
		for (int i = 0; i < this.count - 1; ++i) {
			if (sortedIndex[i] + 1 != sortedIndex[i + 1]) {
				nextFreeIndex = (short) (sortedIndex[i] + 1);
				break;
			}
		}
		if (nextFreeIndex == -1) {
			//find next free index, at the end
			int n = this.count;
			int sum = n * (n + 1) / 2;
			int restSum = 0;
			for (int i = 0; i < this.count; i++) {
				restSum += this.index[i];
			}
			nextFreeIndex = (short) (sum - restSum);
		}
		if (ChargerDialog.log.isLoggable(Level.INFO)) 
			ChargerDialog.log.log(Level.INFO, String.format("memoryHead next free index: %s", nextFreeIndex)); //$NON-NLS-1$
		return nextFreeIndex;
	}
	
	/**
	 * @param batTypeOrdinal
	 * @param addFreeIndex
	 * @return the updated memory head index array where the next free index is inserted after index of battery type ordinal
	 */
	public byte[] addIndexAfter(byte batTypeOrdinal, int addFreeIndex) {
		byte[] updatedIndex = new byte[this.count + 1];
		for (int i = 0, j = 0; i < this.count + 1;) {
			updatedIndex[i++] = this.index[j++];
			if (updatedIndex[i - 1] == batTypeOrdinal) 
				updatedIndex[i++] = (byte) addFreeIndex;
		}
		return updatedIndex;
	}

	public byte[] removeIndex(byte removeIndex) {
		byte[] updatedIndex = new byte[this.count - 1];
		for (int i = 0, j = 0; i < this.count - 1;) {
			if (this.index[j++] != removeIndex) 
				updatedIndex[i++] = this.index[j - 1];
		}
		return updatedIndex;
	}

	public void setIndex(byte[] index) {
		this.index = index;
	}
}
