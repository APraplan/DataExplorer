/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.log;

import static gde.log.Level.TIME;
import static java.util.logging.Level.OFF;

import java.util.function.Supplier;

/**
 * Class shadows the simple name of the superclass.
 * Add TIME and OFF levels.
 */
public class Logger extends java.util.logging.Logger {

	public static Logger getLogger(String name) {
		Logger logger = new Logger(name, null);
		logger.setParent(java.util.logging.Logger.getLogger(name));
		return logger;
	}

	public Logger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	/**
	 * Log a message, which is only to be constructed if the logging level is such that the message will actually be logged.
	 * If the logger is currently enabled for the given message level then the message is constructed by invoking the provided
	 * supplier function and forwarded to all the registered output Handler objects.
	 * </br>Remark: The log message shows this method as the calling method. See {@code off} method for a solution.
	 * @param msgSupplier - A function, which when called, produces the desired log message
	 */
	public void time(Supplier<String> msgSupplier) {
		this.log(TIME, msgSupplier);
	}

	/**
	 * Log a message, which is only to be constructed if the logging level is such that the message will actually be logged.
	 * If the logger is currently enabled for the given message level then the message is constructed by invoking the provided
	 * supplier function and forwarded to all the registered output Handler objects.
	 * @param msgSupplier - A function, which when called, produces the desired log message
	 */
	public void off(Supplier<String> msgSupplier) {
		StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
		this.logp(OFF, stackTraceElement.getClassName(), stackTraceElement.getMethodName(), msgSupplier);
	}

}
