/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wstech;

import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.messages.Messages;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;

/**
 * Class to implement WSTech DataVario device properties extending the CSV2SerialAdapter class
 * @author Winfried Brügmann
 */
public class LinkVario extends DataVario {
	
	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public LinkVario(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wstech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1858), Messages.getString(MessageIds.GDE_MSGT1858));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public LinkVario(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wstech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1858), Messages.getString(MessageIds.GDE_MSGT1858));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}
	
	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device 
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.  
	 */
	@Override
	public CTabItem getUtilityDeviceTabItem() {
		return new VarioToolTabItem(this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount(), this, false);
	}	
	
//	Zeit in s; Empfänger-Spannung in mV; Höhe in dm; Motor-Strom in 0,1 A; Motor-Spannung in mV;
//	verbrauchte Motorakku-Kapazität in mAh; Pitot-Geschwindigkeit in km/h; Temperatur in 0,1 Grad
//	Celsius; GPS-Länge und –Breite in Grad u. Minuten mit vier Nachkommastellen; GPS-Höhe
//	(Meereshöhe) in m; GPS-Geschwindigkeit in km/h ( Groundspeed ); Steigen in 2 cm/s; Pulslänge des
//	LinkVario Steuerkanals in μs; 0 [CR LF]
	
//	Zeit in s; Empfänger-Spannung in mV; Höhe in dm; Motor-Strom in 0,1 A; Motor-Spannung in mV;
//	verbrauchte Motorakku-Kapazität in mAh; Pitot-Geschwindigkeit in km/h; Temperatur in 0,1 Grad
//	Celsius; GPS-Länge und –Breite in Grad u. Minuten mit vier Nachkommastellen; GPS-Höhe
//	(Meereshöhe) in m; GPS-Geschwindigkeit in km/h ( Groundspeed ); Steigen in 2 cm/s; LQI-Wert in %;
//	0 [CR LF]
//	Achtung: Der
}
