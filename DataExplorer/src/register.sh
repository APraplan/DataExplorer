#!/bin/bash
# ***** script to register DataExplorer mime type ** 22 Apr 2009 WB *****

xdg-mime install --novendor DataExplorer-mime.xml
xdg-icon-resource install --context mimetypes --size 48 DataExplorer.xpm application-x-DataExplorer
xdg-desktop-menu install --novendor DataExplorer.desktop DevicePropertiesEditor.desktop
xdg-mime default DataExplorer.desktop application/x-DataExplorer
