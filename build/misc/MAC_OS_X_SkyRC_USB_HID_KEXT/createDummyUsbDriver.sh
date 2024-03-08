#!/bin/bash
# ***** script to install user space kernel extension to make SkyRC USB-HID adapter usable with MAC OS X ** 06 Mar 2019 WB *****
# execution: sudo ./createDummyUsbDriver.sh
cp -r SkyRC.kext /Library/Extensions/
cd /Library/Extensions
chown -R root:wheel SkyRC.kext
chmod -R 755 SkyRC.kext
kextcache -system-caches
echo The kernel extension does not contain any code, it only prevent system to allocate USB HID and enable permission for user context.
echo After SkyRC USB kernel extension installed please reboot the system before connecting a SKyRC USB device again.
echo MAC OS X >= 10.13 High Sierra kernel extensions might be blocked, reboot into recovery console "(push cmd+R while boot)" open terminal
echo Enter "spctl kext-consent disable" and "csrutil disable", reboot to activate
echo Open terminal enter "sudo kextutil -t -v 1 /Library/Extensions/SkyRC.kext" each time before USB cable connected. 
echo This will load none signed kext, ignore warnings regarding missing code signing error 
echo Use /Library/Extensions directory if /System/Library/Extensions is read-only file system "(Catalina)" or "sudo mount -rw /"
echo Alternative use Kext Utility or KextBeast to copy SkyRC.kext os search the Internet for installing 3rd party kext
echo Kext Utiliy may help installing kernel extension, search internet for download link
echo Unfortunately, "kext-dev-mode=1" and "rootless=0" these keys are no longer working for future versions of Mac OS X starting with El Capitan GM. 
echo You need disable SIP with Clover Bootloader.
echo To run Kext Utility, you need to run in terminal this command "sudo spctl --master-disable"
echo After starting Kext Utility just drag and drop the kernel extension complete directory tree to let it install
