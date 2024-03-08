/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.exception;

/**
 * Exception class to be used if the port can not be opened due to internal errors, not configuration error
 * @author Winfried Brügmann
 */
public class PortInUseException extends RuntimeException {
	static final long serialVersionUID = 26031957;

	/**
	 * default constructor
	 */
	public PortInUseException() {
		super();
	}

	/**
	 * @param message
	 */
	public PortInUseException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public PortInUseException(String message, Throwable cause) {
		super(message, cause);
	}

}
