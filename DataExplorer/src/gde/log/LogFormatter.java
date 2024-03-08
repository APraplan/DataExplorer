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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import gde.GDE;

/**
 * class to format the trace log string
 * @author Winfried Brügmann
 */
public class LogFormatter extends Formatter {

	private static final String lineSep = System.getProperty("line.separator"); //$NON-NLS-1$
	Date date = new Date();
  private final static String format = "{0,date,yyyy-MM-dd HH:mm:ss.SSS} {1,number,000000} {2} {3}.{4}() - {5}" + lineSep; //$NON-NLS-1$
  private Object args[] = new Object[6];
  private MessageFormat formatter;
	/*
	SEVERE  	 The highest value; intended for extremely important messages (e.g. fatal program errors).
	WARNING 	Intended for warning messages.
	INFO 	Informational runtime messages.
	CONFIG 	Informational messages about configuration settings/setup.
	FINE 	Used for greater detail, when debugging/diagnosing problems.
	FINER 	Even greater detail.
	FINEST 	The lowest value; greatest detail.
	 */

	/**
	 * A Custom format implementation that is designed for brevity.
	 * supports these syntax options: log.log(Level.FINE, "message")   and   	log.log(Level.FINE, "message", anyObject)
	 * 
	 */
	@Override
	public synchronized String format(LogRecord logRecord) {
		//StringBuilder sb = new StringBuilder();
		// Minimize memory allocations here.
		this.date.setTime(logRecord.getMillis());
		this.args[0] = this.date;
		this.args[1] = logRecord.getThreadID();
		this.args[2] = String.format("%-7s",logRecord.getLevel());
		this.args[3] = logRecord.getLoggerName();
		this.args[4] = logRecord.getSourceMethodName();
		if (logRecord.getParameters() != null && logRecord.getParameters().length > 0) {
			if (logRecord.getParameters()[0] == null) {
				this.args[5] = logRecord.getMessage();
			} else if (logRecord.getParameters()[0] instanceof String) {
				this.args[5] = logRecord.getMessage() + GDE.STRING_BLANK + (String) logRecord.getParameters()[0];
			} else if (logRecord.getParameters()[0] instanceof Number) {
				this.args[5] = logRecord.getMessage() + GDE.STRING_BLANK + NumberFormat.getInstance().format(logRecord.getParameters()[0]);
			} else if (logRecord.getParameters()[0] instanceof Date) {
				this.args[5] = logRecord.getMessage() + GDE.STRING_BLANK + DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(logRecord.getParameters()[0]);
			} else {
				this.args[5] = logRecord.getMessage() + GDE.STRING_BLANK + logRecord.getParameters()[0].toString();
			}
		} else {
			this.args[5] = logRecord.getMessage();
		}
		StringBuffer text = new StringBuffer();
		if (this.formatter == null) {
			this.formatter = new MessageFormat(format);
		}
		this.formatter.format(this.args, text, null);
		if (logRecord.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				logRecord.getThrown().printStackTrace(pw);
				pw.close();
				text.append(sw.toString());
			}
			catch (Throwable ex) {
				//ignore
			}
		}
		return text.toString();
	}

}
