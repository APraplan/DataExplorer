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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.base;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.log.LogFormatter;

import junit.framework.TestCase;

/**
 * Provide logging settings.
 * Inherit from this class if no DataExplorer functions or Settings are needed.
 * @author Thomas Eickert (USER)
 */
public class BasicTestCase extends TestCase {
	protected Logger rootLogger;

	protected final String				tmpDir			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) ? System.getProperty("java.io.tmpdir")
			: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;

	Handler												ch					= new ConsoleHandler();
	LogFormatter									lf					= new LogFormatter();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.rootLogger = Logger.getLogger("");
		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			this.rootLogger.removeHandler(handler);
		}
		this.rootLogger.setLevel(Level.WARNING); // applies to test method logging
		this.rootLogger.addHandler(this.ch);
		this.ch.setFormatter(this.lf);
		this.ch.setLevel(Level.FINER); // applies to console logging in the classes to be tested

		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());
	}

}
