#!/bin/sh
#script to buid and update cp210x kernel module WB 19Feb2012
#usage makeCP210x.sh <kernel version> <minor> <usb copy dir> <bitmode>
#      makeCP210x.sh 3.0.0 16 /media/usbStick 64
#      call for instance from Downloads directory

#if [ ! $( id -u ) -eq 0 ]; then
#	echo "Please execute as root -> 'sudo -su -'"
#	exit 0
#fi

KERNEL_VERSION=$1
MINOR=$2
COPY_DIR=$3
BITMODE=$4
echo $0 $KERNEL_VERSION $MINOR $COPY_DIR $BITMODE

if [ ! $KERNEL_VERSION ]; then
	echo "kernel version is a required argument"
	echo "usage makeCP210x.sh <kernel version> <minor> <usb copy dir> <bitmode>" 
	exit 1
fi
if [ ! $MINOR ]; then
	echo "minor version is a required argument"
	echo "usage makeCP210x.sh <kernel version> <minor> <usb copy dir> <bitmode>" 
	exit 1
fi
if [ ! $COPY_DIR ]; then
	echo "copy directory is a required argument"
	echo "usage makeCP210x.sh <kernel version> <minor> <usb copy dir> <bitmode>" 
	exit 1
fi
if [ ! $BITMODE ]; then
	echo "bitmode is a required argument>"
	echo "usage makeCP210x.sh <kernel version> <minor> <usb copy dir> <bitmode>" 
	exit 1
fi

cd ~/Downloads
sudo apt-get install build-essential linux-source
sudo apt-get autoremove

#all required files are available now, copy to user directory for processing
if [ ! -d linux-source-${KERNEL_VERSION} ]; then
	echo "copy and extract sources"
	cp /usr/src/linux-source-${KERNEL_VERSION}.tar.bz2 .
	bunzip2 linux-source-${KERNEL_VERSION}.tar.bz2
	tar xvf linux-source-${KERNEL_VERSION}.tar
fi
 
echo "cd linux-source-${KERNEL_VERSION}/"
cd linux-source-${KERNEL_VERSION}/
#edit only when required
vi drivers/usb/serial/cp210x.c
pwd

echo "make oldconfig"
make oldconfig
echo "make prepare"
make prepare
echo "make scripts"
make scripts

echo "cp /usr/src/linux-headers-${KERNEL_VERSION}-${MINOR}-generic/Module.symvers ."
cp /usr/src/linux-headers-${KERNEL_VERSION}-${MINOR}-generic/Module.symvers .

echo "make M=drivers/usb/serial"
make M=drivers/usb/serial

echo "backup previous kernel module"
sudo mv /lib/modules/$(uname -r)/kernel/drivers/usb/serial/cp210x.ko /lib/modules/$(uname -r)/kernel/drivers/usb/serial/cp210x.ko.old
sudo cp drivers/usb/serial/cp210x.ko /lib/modules/$(uname -r)/kernel/drivers/usb/serial/

echo "cp drivers/usb/serial/cp210x.ko ${COPY_DIR}/cp210x.ko.${BITMODE}"
cp drivers/usb/serial/cp210x.ko ${COPY_DIR}/cp210x.ko.${BITMODE}

echo "remove and replace cp210x module"
sudo modprobe -r cp210x
sudo modprobe cp210x

echo "done!"
