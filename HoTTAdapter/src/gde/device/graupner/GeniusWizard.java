/**
 * 
 */
package gde.device.graupner;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

import gde.GDE;
import gde.data.Channel;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.graupner.hott.MessageIds;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.WaitTimer;

/**
 * Graupner Genius Wizard log data base class
 */
public class GeniusWizard extends HoTTAdapter implements IDevice {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public GeniusWizard(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public GeniusWizard(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}


	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		if (dataBuffer.length == this.getDataBlockSize(InputTypes.FILE_IO)) {
			//0=Voltage 1=VoltageMin 2=Current 3=CurrentMax 4=RPM 5=RPM_Max 6=Capacity 7=Temp 8=TempMax 9=TempMoter 10=TempMoterMax 11=Throttle
			points[0] = DataParser.parse2Short(dataBuffer, 9) * 1000;
			points[1] = DataParser.parse2Short(dataBuffer, 11) * 1000;
			points[2] = DataParser.parse2Short(dataBuffer, 17) * 1000;
			points[3] = DataParser.parse2Short(dataBuffer, 19) * 1000;
			points[4] = DataParser.parse2Short(dataBuffer, 21) * 1000;
			points[5] = DataParser.parse2Short(dataBuffer, 23) * 1000;
			points[6] = DataParser.parse2Short(dataBuffer, 13) * 1000;
			points[7] = (dataBuffer[15] & 0xFF) * 1000;
			points[8] = (dataBuffer[16] & 0xFF) * 1000;
			points[9] = (dataBuffer[25] & 0xFF) * 1000;
			points[10] = (dataBuffer[26] & 0xFF) * 1000;
			points[11] = DataParser.parse2Short(dataBuffer, 32) * 1000;
		}
		return points;
	}

	/**
	 * import device specific *.bin data files
	 */
	@Override
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2440), "LogData");

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					GeniusWizard.this.application.setPortConnected(true);
					boolean isInitialSwitched = false;

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.CHAR_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_LOG;
						}
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							Integer channelConfigNumber = GeniusWizard.this.application.getActiveChannelNumber();
							channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
							try {
								GeniusWizardLogReader.read(selectedImportFile); //, HoTTAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
								if (!isInitialSwitched) {
									Channel selectedChannel = GeniusWizard.this.channels.get(1);
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									if (GeniusWizard.this.application.getActiveChannel().getActiveRecordSet() != null) {
										selectedChannel.switchRecordSet(GeniusWizard.this.application.getActiveChannel().getActiveRecordSet().getName());
									}
									isInitialSwitched = true;
								}
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally  {
					GeniusWizard.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
}
