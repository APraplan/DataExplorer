#! /bin/sh
#replace cp210x module, cp210x_open - Unable to enable UART, timeout 3000 m  2011 May 15 WB
sudo mv /lib/modules/$(uname -r)/kernel/drivers/usb/serial/cp210x.ko /lib/modules/$(uname -r)/kernel/drivers/usb/serial/cp210x.ko.old
sudo cp cp210x.ko /lib/modules/$(uname -r)/kernel/drivers/usb/serial/
sudo modprobe -r cp210x
sudo modprobe cp210x
