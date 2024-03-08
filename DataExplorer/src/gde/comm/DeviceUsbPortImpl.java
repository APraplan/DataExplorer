package gde.comm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;

import org.usb4java.BufferUtils;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.dialog.UsbDeviceSelectionDialog;
import gde.utils.WaitTimer;
import gnu.io.SerialPort;

public class DeviceUsbPortImpl implements IDeviceCommPort {
	private final static Logger					log							= Logger.getLogger(DeviceUsbPortImpl.class.getName());

	final protected DeviceConfiguration	deviceConfig;
	final protected DataExplorer				application;
	final Settings											settings;

	boolean															isConnected			= false;
	int																	asyncReceived		= 0;
	boolean															isAsyncReceived	= false;
	int																	endpointTransferType = LibUsb.TRANSFER_TYPE_INTERRUPT; //check interface descriptor

	Set<UsbDevice>											usbDevices			= new HashSet<UsbDevice>();
	Set<Device>													libUsbDevices		= new HashSet<Device>();
	static Device												selectedUsbDevice = null;

	/**
	 * normal constructor to be used within DataExplorer
	 * @param currentDeviceConfig
	 * @param currentApplication
	 */
	public DeviceUsbPortImpl(DeviceConfiguration currentDeviceConfig, DataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
		this.settings = Settings.getInstance();
	}

	/**
	 * constructor for test purpose only, do not use within DataExplorer
	 */
	public DeviceUsbPortImpl() {
		this.deviceConfig = null;
		this.application = null;
		this.settings = null;
	}

	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		// Auto-generated method stub
		return null;
	}

	public void close() {
		// Auto-generated method stub

	}

	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public void write(byte[] writeBuffer) throws IOException {
		// Auto-generated method stub

	}

	public void write(byte[] writeBuffer, long gap_ms) throws IOException {
		// Auto-generated method stub

	}

	public int cleanInputStream() throws IOException {
		// Auto-generated method stub
		return 0;
	}

	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		// Auto-generated method stub
		return 0;
	}

	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	public int getXferErrors() {
		// Auto-generated method stub
		return 0;
	}

	public void addXferError() {
		// Auto-generated method stub

	}

	public int getTimeoutErrors() {
		// Auto-generated method stub
		return 0;
	}

	public void addTimeoutError() {
		// Auto-generated method stub

	}

	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		// Auto-generated method stub
		return false;
	}

	public int getAvailableBytes() throws IOException {
		// Auto-generated method stub
		return 0;
	}

	/**
	 * find USB device to be identified by vendor ID and product ID
	 * @param vendorId
	 * @param productId
	 * @return
	 * @throws UsbException
	 */
	@Override
	public Set<UsbDevice> findUsbDevices(final short vendorId, final short productId) throws UsbException {
		usbDevices.clear();
		UsbServices services = UsbHostManager.getUsbServices();
		return findDevices(services.getRootUsbHub(), vendorId, productId);
	}

	/**
	 * find USB device starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	@Override
	public Set<UsbDevice> findDevices(UsbHub hub, short vendorId, short productId) {
		for (Object tmpDevice : hub.getAttachedUsbDevices()) {
			if (tmpDevice instanceof UsbDevice) {
				UsbDevice device = (UsbDevice) tmpDevice;
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, device.toString());
					log.log(Level.FINER, device.getUsbDeviceDescriptor().toString());
				}
				UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
				if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, device.toString());
						log.log(Level.FINE, device.getUsbDeviceDescriptor().toString());
					}
					usbDevices.add(device);
				}
				else if (device.isUsbHub()) {
					usbDevices.addAll(findDevices((UsbHub) device, vendorId, productId));
				}
			}
		}
		return usbDevices;
	}

	public Set<Device> findDevices(final short vendorId, final short productId, final String productString) throws LibUsbException {
		libUsbDevices.clear();
		// Read the USB device list
		DeviceList list = new DeviceList();
		Context context = new Context();
		int result = LibUsb.init(context);
		if (result < 0) {
			throw new LibUsbException("Unable to initialize libusb", result);
		}
		result = LibUsb.getDeviceList(context, list);
		if (result < 0) throw new LibUsbException("Unable to get device list", result);

		try {
			// Iterate over all devices and scan for the right one
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result != LibUsb.SUCCESS)
					throw new LibUsbException("Unable to read device descriptor", result);
				else if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
					if (productString == null || productString.length() == 0) {
						libUsbDevices.add(device);
					} else { //find device with matching product string
						DeviceHandle handle = new DeviceHandle();
						result = LibUsb.open(device, handle);
						if (result < 0) {
							log.log(Level.WARNING, String.format("Unable to open device: %s. " + "Continuing without device handle.", LibUsb.strError(result)));
							handle = null;
						}
						String[] descriptorStringArray = descriptor.dump(handle).split("\\n");
						for (String descriptorString : descriptorStringArray) {
							if (descriptorString.contains("iProduct") && descriptorString.contains(productString)) 
								libUsbDevices.add(device);
						}
					}
				}
			}
		}
		finally {
			// Ensure the allocated device list is freed
			LibUsb.freeDeviceList(list, true);
		}
		return libUsbDevices;
	}

	/**
	 * find all USB devices recursive over USB hubs
	 * @param hub
	 */
	public void findUsbDevices(UsbHub hub) {
		for (Object tmpDevice : hub.getAttachedUsbDevices()) {
			if (tmpDevice instanceof UsbDevice) {
				UsbDevice usbDevice = (UsbDevice) tmpDevice;
				log.log(Level.INFO, usbDevice.toString());
				log.log(Level.INFO, usbDevice.getUsbDeviceDescriptor().toString());
				if (usbDevice.isUsbHub()) {
					findUsbDevices((UsbHub) usbDevice);
				}
			}
		}
	}

	/**
	 * dump information for all USB devices
	 * @throws UsbException
	 */
	public void dumpUsbDevices() throws UsbException {
		log.log(Level.INFO, "Use UsbHostManager.getUsbServices()\n");
		UsbServices services = UsbHostManager.getUsbServices();
		UsbHub hub = services.getRootUsbHub();
		findUsbDevices(hub);
	}

	/**
	 * dump information for all USB devices using LibUsb
	 * @throws UsbException
	 */
	public void dumpLibUsbDevices() throws LibUsbException {
		log.log(Level.INFO, "Use LibUsb.getDeviceList()\n");
		DeviceList list = new DeviceList();
		Context context = new Context();

		int result = LibUsb.init(context);
		if (result < 0) throw new LibUsbException("Unable to initialize libusb", result);

		result = LibUsb.getDeviceList(context, list);
		if (result < 0) throw new LibUsbException("Unable to get device list", result);

		try {
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result == LibUsb.SUCCESS) {
					log.log(Level.INFO, device.toString());
					// Try to open the device. This may fail because user has no
					// permission to communicate with the device. This is not
					// important for the dumps, we are just not able to resolve string
					// descriptor numbers to strings in the descriptor dumps.
					DeviceHandle handle = new DeviceHandle();
					result = LibUsb.open(device, handle);
					if (result < 0) {
						log.log(Level.WARNING, String.format("Unable to open device: %s. " + "Continuing without device handle.", LibUsb.strError(result)));
						handle = null;
					}

					log.log(Level.INFO, descriptor.dump(handle));

					// Close the device if it was opened
					if (handle != null) {
						LibUsb.close(handle);
					}
				}
			}
		}
		finally {
			LibUsb.freeDeviceList(list, true);
		}
	}

	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	public void dumpUsbDevices(final short vendorId, final short productId) throws UsbException {
		Set<UsbDevice> myUsbDevices = findUsbDevices(vendorId, productId);
		for (UsbDevice myUsbDevice : myUsbDevices) {
			log.log(Level.FINE, myUsbDevice.toString());
			log.log(Level.FINE, myUsbDevice.getUsbDeviceDescriptor().toString());
		}

		Set<Device> myLibUsbDevices = findDevices(vendorId, productId, new String());
		for (Device myLibUsbDevice : myLibUsbDevices) {
			DeviceDescriptor descriptor = new DeviceDescriptor();
			int result = LibUsb.getDeviceDescriptor(myLibUsbDevice, descriptor);
			if (result == LibUsb.SUCCESS) {
				DeviceHandle handle = new DeviceHandle();
				result = LibUsb.open(myLibUsbDevice, handle);
				if (result < 0) {
					log.log(Level.WARNING, String.format("Unable to open device: %s. " + "Continuing without device handle.", LibUsb.strError(result)));
					handle = null;
				}
				log.log(Level.FINE, myLibUsbDevice.toString());
				log.log(Level.FINE, String.format("Device %03d/%03d",  LibUsb.getBusNumber(myLibUsbDevice), LibUsb.getDeviceAddress(myLibUsbDevice)));
				log.log(Level.FINE, descriptor.dump(handle));
				
				if (handle != null) {
					LibUsb.close(handle);
				}
			}
		}
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public synchronized UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		
		if (log.isLoggable(Level.FINE)) {
			dumpUsbDevices();
		}
		else if (log.isLoggable(Level.INFO)) {
			dumpUsbDevices();
			dumpLibUsbDevices();
		}
		
		byte ifaceId = activeDevice.getUsbInterface();
		UsbInterface usbInterface = null;
		Set<UsbDevice> myLsbDevices = findUsbDevices(activeDevice.getUsbVendorId(), activeDevice.getUsbProductId());
		if (myLsbDevices.size() == 0) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0050));
		} else if (myLsbDevices.size() == 1) {
			for (UsbDevice usbDevice : myLsbDevices) {
				if (usbDevice == null) 
					throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
				usbInterface = claimUsbInterface(usbDevice, ifaceId);
			}
		} else {
			for (UsbDevice usbDevice : myLsbDevices) {
				UsbInterface tmpUsbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(ifaceId);
				if (tmpUsbInterface!= null && !(tmpUsbInterface.isActive() || tmpUsbInterface.isClaimed())) {
					usbInterface = claimUsbInterface(usbDevice, ifaceId);
					break;
				}
			}
		}
		if (usbInterface == null) {
			throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
		}
		log.log(Level.FINE, "interface claimed");
		this.isConnected = true;
		if (this.application != null) this.application.setPortConnected(true);
		return usbInterface;
	}

	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws LibUsbException
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public synchronized DeviceHandle openLibUsbPort(final IDevice activeDevice) throws LibUsbException, UsbClaimException, UsbException {
		DeviceHandle libUsbDeviceHandle = new DeviceHandle();
		byte ifaceId = activeDevice.getUsbInterface();

		if (log.isLoggable(Level.FINE)) {
			dumpLibUsbDevices();
		}
		
    // Initialize the libusb context
    int result = LibUsb.init(null);
    if (result != LibUsb.SUCCESS) {
        throw new LibUsbException("Unable to initialize libusb", result);
    }

		Set<Device> myLibUsbDevices = findDevices(activeDevice.getUsbVendorId(), activeDevice.getUsbProductId(), activeDevice.getUsbProductString());
		if (myLibUsbDevices.size() == 0) {
			throw new UsbException(String.format("%s\n===>>>  %s", Messages.getString(MessageIds.GDE_MSGE0050), activeDevice.getUsbProductString()));
		}
		else if (myLibUsbDevices.size() == 1) {
			for (Device libUsbDevice : myLibUsbDevices) {
				claimLibUsbDevice(libUsbDevice, libUsbDeviceHandle, ifaceId);
			}
		}
		else {
			Map<String, Device> usbDevices = new HashMap<String, Device>();
			for (Device libUsbDevice : myLibUsbDevices) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(libUsbDevice, descriptor);
				if (result != LibUsb.SUCCESS)
					throw new LibUsbException("Unable to read device descriptor", result);

				DeviceHandle handle = new DeviceHandle();
				result = LibUsb.open(libUsbDevice, handle);
				if (result < 0) {
					log.log(Level.WARNING, String.format("Unable to open device: %s. " + "Continuing without device handle.", LibUsb.strError(result)));
					handle = null;
				}
				String descriptorString = descriptor.dump(handle);
				if (descriptorString.contains("iProduct") && descriptorString.contains(activeDevice.getUsbProductString()) && descriptorString.contains("iSerial")) {
					String usbDeviceString = descriptorString.substring(descriptorString.lastIndexOf("Junsi"), descriptorString.indexOf("USB HID") + 7) 
							+ " SN/" + descriptorString.substring(descriptorString.indexOf("iSerial") + 27, descriptorString.indexOf("iSerial") + 37);
					usbDevices.put(usbDeviceString, libUsbDevice);
				}				
				LibUsb.close(handle);
			}
			if (usbDevices.size() > 0 && selectedUsbDevice == null) {
				//several identical USB HID  found, manual select usbDevice
				selectedUsbDevice = (Device) new UsbDeviceSelectionDialog(usbDevices).open();
				claimLibUsbDevice(selectedUsbDevice, libUsbDeviceHandle, ifaceId);
			}
			else if (usbDevices.size() > 0 && selectedUsbDevice != null) {
				//several identical USB HID  found, selected usbDevice available for this GDE instance
				claimLibUsbDevice(selectedUsbDevice, libUsbDeviceHandle, ifaceId);
			}
			else
				throw new UsbException(String.format("%s\n===>>>  %s", Messages.getString(MessageIds.GDE_MSGE0050), activeDevice.getUsbProductString()));
		}
		log.log(Level.FINE, "interface claimed");
		this.isConnected = true;
		if (this.application != null) this.application.setPortConnected(true);
		return libUsbDeviceHandle;
	}

	private void claimLibUsbDevice(Device libUsbDevice, DeviceHandle libUsbDeviceHandle, byte ifaceId) throws UsbException, UsbClaimException {
		int result;
		if (libUsbDevice == null) throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
		result = LibUsb.open(libUsbDevice, libUsbDeviceHandle);
		if (result < 0) {
			LibUsb.close(libUsbDeviceHandle);
			log.log(Level.SEVERE, String.format("Unable to open device: %s.", LibUsb.strError(result)));
			throw new UsbClaimException(new LibUsbException(result).getMessage());
		}
		
		if (GDE.IS_LINUX) {
			result = LibUsb.kernelDriverActive(libUsbDeviceHandle, ifaceId);

			if (result == 1) {
				result = LibUsb.detachKernelDriver(libUsbDeviceHandle, ifaceId);

				if (result != LibUsb.SUCCESS) {
					throw new UsbClaimException(new LibUsbException(result).getMessage());
				}
			}
		}
		result = LibUsb.claimInterface(libUsbDeviceHandle, ifaceId);
		if (result < 0) {
			LibUsb.close(libUsbDeviceHandle);
			log.log(Level.SEVERE, String.format("Unable to claim device: %s.", LibUsb.strError(result)));
			throw new UsbClaimException(new LibUsbException(result).getMessage());
		}
	}

	/**
	 * @param usbDevice
	 * @param ifaceId
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	private UsbInterface claimUsbInterface(UsbDevice usbDevice, byte ifaceId) throws UsbClaimException, UsbException {
		UsbInterface usbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(ifaceId);
		log.log(Level.FINE, usbInterface.toString());
		if (GDE.IS_LINUX) {
			usbInterface.claim(new UsbInterfacePolicy() {
				@Override
				public boolean forceClaim(UsbInterface usbInterface_) {
					return true;
				}
			});
		}
		else {
			usbInterface.claim();
		}
		return usbInterface;
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public synchronized void closeUsbPort(UsbInterface usbInterface) throws UsbClaimException, UsbException {
		this.isConnected = false;
		WaitTimer.delay(200); // enable gatherer thread to stop execution
		if (usbInterface != null && usbInterface.isClaimed()) {
			usbInterface.release();
			log.log(Level.FINE, "interface released");
		}
		if (this.application != null) {
			this.application.setSerialTxOff();
			this.application.setSerialRxOff();
			this.application.setPortConnected(false);
		}
	}

	/**
	 * release or close the given interface
	 * @param usbDeviceHandle
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public synchronized void closeLibUsbPort(DeviceHandle usbDeviceHandle, boolean cacheSelectedUsbDevice) throws UsbClaimException, UsbException {
		this.isConnected = false;
		WaitTimer.delay(200); // enable gatherer thread to stop execution
		if (usbDeviceHandle != null) {
			LibUsb.close(usbDeviceHandle);
			LibUsb.exit(null);
			log.log(Level.FINE, "interface released");
		}
		if (this.application != null) {
			this.application.setSerialTxOff();
			this.application.setSerialRxOff();
			this.application.setPortConnected(false);
			
			if (!cacheSelectedUsbDevice) selectedUsbDevice = null;
		}
	}

	/**
	 * write a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data
	 * @return number of bytes sent
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public synchronized int write(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int sent = 0;
		UsbPipe pipe = endpoint.getUsbPipe();
		pipe.open();
		try {
			sent = pipe.syncSubmit(data);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sent + " bytes sent");
		}
		finally {
			pipe.close();
		}
		return sent;
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public synchronized int read(final UsbInterface iface, final byte endpointAddress, byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int received = 0;
		UsbPipe pipe = endpoint.getUsbPipe();
		pipe.open();
		try {
			received = pipe.syncSubmit(data);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, received + " bytes received");
		}
		finally {
			pipe.close();
		}
		return received;
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @param timeout_msec
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public synchronized int read(final UsbInterface iface, final byte endpointAddress, final byte[] data, final int timeout_msec)
			throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int waitTime_msec = timeout_msec;
		int waitDelay_msec = timeout_msec / 100;
		asyncReceived = 0;
		isAsyncReceived = false;
		final UsbPipe pipe = endpoint.getUsbPipe();
		UsbPipeListener usbPipeListener = new UsbPipeListener() {

			public void errorEventOccurred(UsbPipeErrorEvent evt) {
				log.log(Level.WARNING, evt.toString());
			}

			public void dataEventOccurred(UsbPipeDataEvent evt) {
				byte[] tmpData = evt.getData();
				isAsyncReceived = true;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, (asyncReceived = tmpData.length) + " bytes received");
			}
		};
		try {
			pipe.open();
			pipe.addUsbPipeListener(usbPipeListener);
			pipe.asyncSubmit(data);

			while (isConnected && !isAsyncReceived && waitTime_msec > 0) {
				WaitTimer.delay(waitDelay_msec);
				waitTime_msec -= waitDelay_msec;
			}
		}
		finally {
			pipe.removeUsbPipeListener(usbPipeListener);
			if (pipe.isOpen()) {
				pipe.abortAllSubmissions();
				pipe.close();
			}
		}
		return asyncReceived;
	}
	
  /**
   * Writes some data byte array to the device.
   * @param handle The device handle.
   * @param outEndpoint The end point address
   * @param data the byte array for data with length as size to be send 
   * @param timeout_ms the time out in milli seconds
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public void write(final DeviceHandle handle, final byte outEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {

      ByteBuffer buffer = BufferUtils.allocateByteBuffer(data.length);
      buffer.put(data);      
      IntBuffer transferred = BufferUtils.allocateIntBuffer();
      
      int result = this.endpointTransferType == LibUsb.TRANSFER_TYPE_BULK 
      		? LibUsb.bulkTransfer(handle, outEndpoint, buffer, transferred, timeout_ms)
      				: LibUsb.interruptTransfer(handle, outEndpoint, buffer, transferred, timeout_ms);
      if (result != LibUsb.SUCCESS) {
      	switch (result) {
      	case LibUsb.ERROR_TIMEOUT:
          throw new TimeOutException(new LibUsbException(result).getMessage());
      	case LibUsb.ERROR_ACCESS:
      	case LibUsb.ERROR_IO:
      	case LibUsb.ERROR_NO_DEVICE:
          throw new UsbDisconnectedException(new LibUsbException(result).getMessage());
        default:
        	throw new IllegalStateException(new LibUsbException(result).getMessage());
      	}
      }
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, transferred.get() + " bytes sent");
  }

  /**
   * Reads some data with length from the device
   * @param handle The device handle.
   * @param inEndpoint The end point address
   * @param data the byte array for data with length as size to be received 
   * @param timeout_ms the time out in milli seconds
   * @return The number of bytes red
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public int read(final DeviceHandle handle, final byte inEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {
  	int readBytes = 0;
      ByteBuffer buffer = BufferUtils.allocateByteBuffer(data.length).order(ByteOrder.LITTLE_ENDIAN);
      IntBuffer transferred = BufferUtils.allocateIntBuffer();
      
      int result = this.endpointTransferType == LibUsb.TRANSFER_TYPE_BULK 
      		? LibUsb.bulkTransfer(handle, inEndpoint, buffer, transferred, timeout_ms)
      				: LibUsb.interruptTransfer(handle, inEndpoint, buffer, transferred, timeout_ms);
      if (result != LibUsb.SUCCESS) {
        readBytes = transferred.get();
        if (readBytes == 0 || readBytes != (buffer.get(0) & 0xFF))
        	switch (result) {
        	case LibUsb.ERROR_TIMEOUT:
            throw new TimeOutException(new LibUsbException(result).getMessage());
        	case LibUsb.ERROR_ACCESS:
        	case LibUsb.ERROR_IO:
        	case LibUsb.ERROR_NO_DEVICE:
            throw new UsbDisconnectedException(new LibUsbException(result).getMessage());
          default:
          	throw new IllegalStateException(new LibUsbException(result).getMessage());
        	}

          buffer.get(data, 0, readBytes);
        
  			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, readBytes + " bytes received");
        return readBytes;
      }
      readBytes = transferred.get();
      buffer.get(data, 0, readBytes);
      
      
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, readBytes + " bytes received");
      return readBytes;
  }
}
