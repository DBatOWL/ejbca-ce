/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.cesecore.util.log;

import java.io.PrintStream;
import java.util.Date;

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;

/**
 * The purpose of this errorhandler is that we can still respond with InternalServer error if and error occurs, but repeated errors will only be
 * logged once.
 * 
 */
public class ProbableErrorHandler implements ErrorHandler {
    private static Date lastFailure = null;

    private static final String ERROR_PREFIX = "log4j error: ";

    boolean firstTime = true;

    static PrintStream output = System.err;
    
    @Override
    public void error(String arg0) {
        if (firstTime) {
            output.println(ERROR_PREFIX + arg0);
            firstTime = false;
        }
        lastFailure = new Date();
    }

    /**
     * Returns true if an error writing to the log files have happened since 'date'.
     * 
     * @param date see if an error happened later than this date
     * @return true if an error has happened, false if logging works fine.
     */
    public static boolean hasFailedSince(Date date) {
        return (lastFailure != null && lastFailure.after(date));
    }

    @Override
    public void error(String msg, Throwable t) {
        error(msg, t);
        lastFailure = new Date();          
    }

    @Override
    public void error(String msg, LogEvent event, Throwable t) {
        if (firstTime) {
            output.println(ERROR_PREFIX + msg);
            t.printStackTrace(output);
            firstTime = false;
        }
        error(msg, event, t);
        lastFailure = new Date();        
    }
}
