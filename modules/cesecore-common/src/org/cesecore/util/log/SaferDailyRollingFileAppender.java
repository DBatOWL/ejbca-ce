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

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * The purpose of this extension is to notify the client of the this log appender that it isn't possible to log anymore.
 */
@Plugin(name = SaferDailyRollingFileAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SaferDailyRollingFileAppender extends NonFinalRollingFileAppender {
    
    public static final String PLUGIN_NAME = "SaferDailyRollingFileAppender";

    private static final Logger LOGGER = LogManager.getLogger(SaferDailyRollingFileAppender.class.getName());
    protected final static StatusLogger log = StatusLogger.getLogger();
    
    private static SaferAppenderListener subscriber;

    /** Sets the SaferAppenderListener that will be informed if a logging error occurs. */
    
    public static void addSubscriber(SaferAppenderListener pSubscriber) {
        subscriber = pSubscriber;
    }
    
    /** Constructor from superclass. */ 
    public SaferDailyRollingFileAppender(
            final String name, 
            final Layout<? extends Serializable> layout, 
            final Filter filter,
            final RollingFileManager manager, 
            final String fileName, 
            final String filePattern,
            final boolean ignoreExceptions, 
            final boolean immediateFlush, 
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, manager, fileName, filePattern, ignoreExceptions, immediateFlush, advertiser, properties);
    }
    
    @Override
    public void append(final LogEvent event) {
        super.append(event);
        File logfile;
          try {
              logfile = new File(super.getFileName());
              if (subscriber != null) {
                  if (logfile.canWrite()) {
                      subscriber.setCanlog(true);
                  } else {
                      subscriber.setCanlog(false);
                  }
              }
          } catch (Exception e) {
              if (subscriber != null) {
                  subscriber.setCanlog(false);
              }
          }
    }

 // EJBCAINTER-323 Must be done with the RollingFileManager
//    public void setFile(final String filename) {
//        constructPath(filename);
//    }

    private void constructPath(final String filename) {
        File dir;
        try {
            URL url = new URL(filename.trim());
            dir = new File(url.getFile()).getParentFile();
        } catch (MalformedURLException e) {
            dir = new File(filename.trim()).getParentFile();
        }
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                log.info("Failed to create directory structure: '" + dir + "'.");
            }
        }
    }
}
