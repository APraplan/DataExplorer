package gde.device.conrad;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Sample serial port implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class VC800SerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME			= VC800SerialPort.class.getName();
	final static Logger	log							= Logger.getLogger($CLASS_NAME);

	boolean							isInSync				= false;
	final static int		xferErrorLimit	= 10;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public VC800SerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[14];
		byte[] answer = new byte[14];
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			
			answer = this.read(answer, 5000);
			// synchronize received data to begin of sent data 
			while ((answer[0]& 0xF0) != 0x10 && (answer[13]& 0xF0) != 0xE0) {
				this.isInSync = false;
				for (int i = 0; i < answer.length; i++) {
					if((answer[i]& 0xF0) == 0xE0){
						System.arraycopy(answer, i+1, data, 0, 13-i);
						answer = new byte[i+1];
						answer = this.read(answer, 3000);
						System.arraycopy(answer, 0, data, 14-i, i);
						this.isInSync = true;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if(this.isInSync)
					break;
				answer = new byte[14];
				answer = this.read(answer, 3000);
			}
			if ((answer[0]& 0xF0) != 0x10 || (answer[13]& 0xF0) != 0xE0) {
				VC800SerialPort.log.logp(java.util.logging.Level.WARNING, VC800SerialPort.$CLASS_NAME, $METHOD_NAME,	"=====> data start or end does not match, number of errors = " + this.getXferErrors());
				this.addXferError();
				if (this.getXferErrors() > 0 && this.getXferErrors() % VC800SerialPort.xferErrorLimit == 0) {
					VC800SerialPort.log.logp(java.util.logging.Level.WARNING, VC800SerialPort.$CLASS_NAME, $METHOD_NAME,	"Number of tranfer error exceed the acceptable limit! number errors = " + this.getXferErrors());
					application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,		new Object[] { "SerialPortException",  this.getXferErrors() }));
				}
				data = getData();
			}
			else {
				System.arraycopy(answer, 0, data, 0, 14);
			}
		}
		catch (Exception e) {
			if(!(e instanceof TimeOutException)) log.log(Level.SEVERE, e.getMessage());
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return data;
	}
}
