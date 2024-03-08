#!/bin/sh
######################################
# execute RXTX Post-Installation Steps to fix permissions
######################################

curruser=`sudo id -p | grep 'login' | sed 's/login.//'`

if [ ! -d /var/lock ]
then
sudo mkdir /var/lock
fi

sudo chgrp uucp /var/lock
sudo chmod 775 /var/lock

# for Mac OS lower than 10.5 use niutil instead 

#if [ ! `sudo niutil -readprop / /groups/uucp users | grep $curruser > /dev/null` ]
if [ ! `sudo dscl . -read / /groups/_uucp users | grep $curruser > /dev/null` ]
then
#  sudo niutil -mergeprop / /groups/uucp users $curruser
  sudo dscl . -append /groups/_uucp GroupMembership $curruser
fi 

sudo chmod g+w /var/spool/uucp/
