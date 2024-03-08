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
package gde.device.wb;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.io.CSVSerialDataReaderWriter;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

public class AkkuMonitor extends CSV2SerialAdapter {

	public AkkuMonitor(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	public AkkuMonitor(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * import a CSV file, also called "OpenFormat" file
	 */
	@Override
	public void importCsvFiles() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1700));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					AkkuMonitor.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend;
								try {
									recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT)-4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
									Integer.valueOf(recordNameExtend);
								}
								catch (Exception e) {
									try {
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT)-3, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
										Integer.valueOf(recordNameExtend);
									}
									catch (Exception e1) {
										recordNameExtend = GDE.STRING_EMPTY;
									}
								}
								Integer channelConfigNumber = dialog != null && !dialog.isDisposed() ? dialog.getTabFolderSelectionIndex() + 1 : null;
								CSVSerialDataReaderWriter.read(selectedImportFile, AkkuMonitor.this, recordNameExtend, channelConfigNumber, 
										new AkkuMonitorParser(AkkuMonitor.this.getDataBlockTimeUnitFactor(), 
												AkkuMonitor.this.getDataBlockLeader(), AkkuMonitor.this.getDataBlockSeparator().value(), 
												AkkuMonitor.this.getDataBlockCheckSumType(), AkkuMonitor.this.getDataBlockSize(InputTypes.FILE_IO)));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					AkkuMonitor.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
}
