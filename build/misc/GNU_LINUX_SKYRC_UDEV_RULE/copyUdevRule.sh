#! /bin/sh
#copy udev rule into udev rules directory and make it active
sudo cp ./50-SkyRC-Charger.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules
echo "un-plug and plug USB connector again, better reboot the system"
