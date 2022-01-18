/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Use {@link #add()} to add a handler to {@link java.util.logging.Logger} that logs to {@link org.apache.log4j.Logger}
 * 
 *
 */
public class Log4jHandler extends Handler {
	private static boolean isStarted = false;
	/**
	 * Add handler to java sun logging that logs to log4j
	 */
	public static void add() {
		if ( isStarted ) {
			return;
		}
		isStarted = true;
		final Level logLevel = Level.FINEST;
		final Logger rootLogger = Logger.getLogger("");
		final Handler[] handlers = rootLogger.getHandlers();
		for ( int i=0; i<handlers.length; i++ ) {
			rootLogger.removeHandler(handlers[i]);
		}
		rootLogger.setLevel(logLevel);
		final Handler handler = new Log4jHandler();
		handler.setLevel(logLevel);
		rootLogger.addHandler( handler );
	}


	/* (non-Javadoc)
	 * @see java.util.logging.Handler#close()
	 */
	public void close() throws SecurityException {
		// do nothing
	}
	/* (non-Javadoc)
	 * @see java.util.logging.Handler#flush()
	 */
	public void flush() {
		// do nothing
	}
	private org.apache.logging.log4j.Level translateLevel( Level level ) {
		if ( level.intValue() < Level.FINEST.intValue() ) {
			return org.apache.logging.log4j.Level.ALL;
		} else if ( level.intValue() <= Level.FINEST.intValue() ) {
			return org.apache.logging.log4j.Level.TRACE;
		} else if ( level.intValue() <= Level.FINER.intValue() ) {
			return org.apache.logging.log4j.Level.DEBUG;
		} else if ( level.intValue() <= Level.FINE.intValue() ) {
			return org.apache.logging.log4j.Level.DEBUG;
		} else if ( level.intValue() <= Level.INFO.intValue() ) {
			return org.apache.logging.log4j.Level.INFO;
		} else if ( level.intValue() <= Level.WARNING.intValue() ) {
			return org.apache.logging.log4j.Level.WARN;
		} else if ( level.intValue() <= Level.SEVERE.intValue() ) {
			return org.apache.logging.log4j.Level.FATAL;
		} else {
			return org.apache.logging.log4j.Level.OFF;
		}
	}
	/* (non-Javadoc)
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
    public void publish(LogRecord logRecord) {
        final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(logRecord.getSourceClassName());
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.print(logRecord.getSourceMethodName());
        printWriter.print(": ");
        printWriter.print(logRecord.getMessage());
        Object[] parameters = logRecord.getParameters();
        for (int i = 0; parameters != null && i < parameters.length; i++) {
            printWriter.println();
            printWriter.print("{" + i + "} = " + parameters[i]);
        }
        logger.log(translateLevel(logRecord.getLevel()), Log4jHandler.class.getCanonicalName(), stringWriter.toString(), logRecord.getThrown());
    }
}
