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

    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.device;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;

import gde.Analyzer;
import gde.GDE;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;

/**
 * Devices with history support implementations.
 * @author Thomas Eickert
 */
public interface IHistoDevice { //todo merging with IDevice later

	/**
	 * @return true if the device supports a native file import for histo purposes
	 */
	boolean isHistoImportSupported();

	/**
	 * @return the device's native file extentions if the device supports histo imports (e.g. '.bin' or '.log')
	 */
	List<String> getSupportedImportExtentions();

	/**
	 * Create recordSet and add record data size points from binary file to each measurement.
	 * It is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * Do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * Reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param inputStream for loading the log data
	 * @param truss references the requested vault for feeding with the results (vault might be without measurements, settlements and scores)
	 * @param analyzer defines the the requested device, channel, object
	 * @throws DataInconsitsentException
	 * @throws DataTypeException
	 * @throws IOException
	 */
	public void getRecordSetFromImportFile(Supplier<InputStream> inputStream, VaultCollector truss, Analyzer analyzer) throws DataInconsitsentException, IOException, DataTypeException;

	/**
	 * Add record data points from file stream to each measurement.
	 * It is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * Do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * Reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param recordSet is the target object holding the records (curves) which include measurement curves and calculated curves
	 * @param dataBuffer holds rows for each time step (i = recordDataSize) with measurement data (j = recordNamesLength equals the number of measurements)
	 * @param recordDataSize is the number of time steps
	 * @param analyzer defines the the requested device, channel, object
	 */
	void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] data_in, int recordDataSize, int[] maxPoints, int[] minPoints, Analyzer analyzer) throws DataInconsitsentException;

		/**
	 * Import device specific *.bin data files
	 * @param filePath
	 */
	void importDeviceData(Path filePath);

	/**
	 * Function to calculate values for inactive records, data not readable from device.
	 * Extracted from makeInActiveDisplayable which performs activities related to the UI.
	 */
	void calculateInactiveRecords(RecordSet recordSet);

	/**
	 * Collect the settings relevant for the values inserted in the histo vault.
	 * @return the settings which determine the measurement values returned by the reader
	 */
	default String getReaderSettingsCsv() {
		return GDE.STRING_EMPTY;
	}

	/**
	 * Device specific array of active sensors.
	 * The zero position is reserved for the receiver and is mandatory.
	 * The array length and order is specific to the device (i.e. has at least one element).
	 * @param sensorSignature is a csv list of valid sensor values (i.e. sensor names)
	 * @return a set with bits representing the sensor type ordinal numbers (true if the sensor type is active)
	 */
	BitSet getActiveSensors(String sensorSignature);
}
