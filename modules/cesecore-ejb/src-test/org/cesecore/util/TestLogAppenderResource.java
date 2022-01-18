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
package org.cesecore.util;

import java.io.ByteArrayOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.rules.ExternalResource;

// TODO ECA-8963: Extract into a separate module ejbca-unittest, as it is common utility class that can be reused.
/**
 * This is a help class, implementing a @Rule to catch log4j logging messages.
 *
 */
public class TestLogAppenderResource extends ExternalResource {

    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private final Class<?> clazz;
    
    public TestLogAppenderResource(final Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void before() {
        addAppender();
    }

    @Override
    protected void after() {
        removeAppender();
    }

    public String getOutput() {
        return outContent.toString();
    }
    
    private void removeAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        config.removeLogger(clazz.getCanonicalName());
        ctx.updateLoggers();
    }
    
    private void addAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%level - %m%n").withConfiguration(config).build();
        
        OutputStreamAppender writerAppender = OutputStreamAppender.newBuilder().setName(clazz.getName()).setTarget(outContent)
                .setLayout(layout).build();

        writerAppender.start();
        config.addAppender(writerAppender);
        AppenderRef ref = AppenderRef.createAppenderRef(clazz.getName(), null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, clazz.getCanonicalName(), "true", refs, null, config, null);
                
        loggerConfig.addAppender(writerAppender, null, null);
        config.addLogger(clazz.getCanonicalName(), loggerConfig);
        ctx.updateLoggers();
    }

}
