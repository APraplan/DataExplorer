package gde.device.unitrend;

import java.io.IOException;
import java.util.logging.Logger;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Sample serial port implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class UniTrendSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME			= UniTrendSerialPort.class.getName();
	final static Logger	log							= Logger.getLogger(UniTrendSerialPort.$CLASS_NAME);

	final byte					endByte;
	final byte					endByte_1;
	final int						timeout;
	final int						dataLength;

	boolean							isInSync				= true;
	final static int		xferErrorLimit	= 10;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar
	 */
	public UniTrendSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.endByte = this.device.getDataBlockEnding()[this.device.getDataBlockEnding().length - 1];
		this.endByte_1 = this.device.getDataBlockEnding().length == 2 ? this.device.getDataBlockEnding()[0] : 0x00;
		this.dataLength = Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
		this.timeout = this.device.getDeviceConfiguration().getReadTimeOut();
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[this.dataLength];
		byte[] answer = new byte[this.dataLength];
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}

			answer = this.read(answer, 5000);
			if (log.isLoggable(Level.FINER)) 
				log.log(Level.FINER, StringHelper.byte2Hex4CharString(answer, answer.length));
			// synchronize received data to begin of sent data
			while ((answer[this.dataLength - 1] & 0xFF) != this.endByte && (answer[this.dataLength - 2] & 0xFF) != this.endByte_1) {
				this.isInSync = false;
				log.log(Level.WARNING, "Answer needs synchronization, does not end with CRLF");
				log.log(Level.WARNING, StringHelper.byte2Hex4CharString(answer, answer.length));

				for (int i = 1; i < answer.length; i++) {
					if ((answer[i] & 0xFF) == this.endByte && (answer[i - 1] & 0xFF) == this.endByte_1) {
						log.log(Level.WARNING, String.format("CRLF found at position %d", i));
						System.arraycopy(answer, i + 1, data, 0, this.dataLength - 1 - i);
						answer = new byte[i + 1];
						answer = this.read(answer, 3000);
						log.log(Level.WARNING, "Reading missing bytes to get in sync");
						log.log(Level.WARNING, StringHelper.byte2Hex4CharString(answer, answer.length));
						System.arraycopy(answer, 0, data, this.dataLength - 1 - i, i + 1);
						this.isInSync = true;
						log.logp(Level.WARNING, UniTrendSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				//check special case 0A 31.....30 0D || 00 31.....30 0D
				if ((answer[0] & 0xFF) == this.endByte || (answer[answer.length - 1] & 0xFF) == this.endByte_1) { 
					log.log(Level.WARNING, "LF found at position 0");
					System.arraycopy(answer, 1, data, 0, this.dataLength - 2);
					answer = new byte[1];
					answer = this.read(answer, 3000);
					log.log(Level.WARNING, "Reading missing byte to get in sync");
					log.log(Level.WARNING, StringHelper.byte2Hex4CharString(answer, answer.length));
					data[this.dataLength - 1] = answer[0];
					this.isInSync = true;
					log.logp(Level.WARNING, UniTrendSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
					log.log(Level.WARNING, StringHelper.byte2Hex4CharString(data, data.length));
				}
				
				if (this.isInSync)  {
					//read fresh data again
					answer = new byte[this.dataLength];
					answer = this.read(answer, 3000);
					if (log.isLoggable(Level.FINER)) 
						log.log(java.util.logging.Level.FINER, StringHelper.byte2Hex4CharString(answer, answer.length));
					break;
				}
//				else {
//					this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { "SerialPortException", "synchronization" }));
//					throw new SerialPortException("Check serial port, data can not get in sync!");
//				}
			}
			if ((answer[this.dataLength - 1] & 0xFF) != this.endByte && (answer[this.dataLength - 2] & 0xFF) != this.endByte_1) {
				log.logp(Level.WARNING, UniTrendSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> data end does not match, number of errors = " + this.getXferErrors());
				this.addXferError();
				if (this.getXferErrors() > 0 && this.getXferErrors() % UniTrendSerialPort.xferErrorLimit == 0) {
					log.logp(Level.WARNING, UniTrendSerialPort.$CLASS_NAME, $METHOD_NAME,
							"Number of tranfer error exceed the acceptable limit! number errors = " + this.getXferErrors());
					this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { "SerialPortException", this.getXferErrors() }));
					throw new SerialPortException(Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { "SerialPortException", this.getXferErrors() }));
				}
				WaitTimer.delay(1000);
				data = getData();
			}
			else {
				System.arraycopy(answer, 0, data, 0, this.dataLength);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) log.log(Level.SEVERE, e.getMessage());
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return data;
	}
}
