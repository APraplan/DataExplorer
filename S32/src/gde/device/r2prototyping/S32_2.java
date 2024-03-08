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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.r2prototyping;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;

/**
 * S32_2 device class 1 Channels all data same timeline
 * @author Winfried Brügmann
 */
public class S32_2 extends S32 implements IDevice {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public S32_2(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public S32_2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}
	
	/**
	 * implementation returning device name which is the default directory name to store OSD files as well to search for
	 * @return the preferred directory to search and store for device specific files
	 */
	public String getFileBaseDir() {
		return this.getClass().getSuperclass().getSimpleName();
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3800), SWT.MULTI);

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					S32_2.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								CSVSerialDataReaderWriter.read(selectedImportFile, S32_2.this, recordNameExtend, null, 
										new DataParser(S32_2.this.getDataBlockTimeUnitFactor(), 
												S32_2.this.getDataBlockLeader(), S32_2.this.getDataBlockSeparator().value(), 
												S32_2.this.getDataBlockCheckSumType(), S32_2.this.getDataBlockSize(InputTypes.FILE_IO), true));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					S32_2.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	@Override
	public boolean recordSetNumberFollowChannel() {
		return false;
	}
}
