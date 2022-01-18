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
package org.ejbca.core.ejb.upgrade;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;

/**
 * Singleton responsible for keep track of a node-local post upgrade.
 * 
 * @version $Id$
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class UpgradeStatusSingletonBean implements UpgradeStatusSingletonLocal {

    /** Custom appender so that we can capture and display the log from the upgrade process. */
    private final Appender appender = new Appender() {

        @Override
        public String getName() {
            return UpgradeStatusSingletonBean.class.getSimpleName();
        }

        @Override
        public State getState() {
            return null;
        }
        @Override
        public void initialize() {
        }
        @Override
        public boolean isStarted() {
            return false;
        }
        @Override
        public boolean isStopped() {
            return false;
        }
        @Override
        public void start() {
        }
        @Override
        public void stop() {
        }
        @Override
        public void append(LogEvent event) {
            logged.add(event);
        }
        @Override
        public ErrorHandler getHandler() {
            return null;
        }
        @Override
        public Layout<? extends Serializable> getLayout() {
            return null;
        }
        @Override
        public boolean ignoreExceptions() {
            return false;
        }
        @Override
        public void setHandler(ErrorHandler handler) {
        }
    };

    private AtomicBoolean postUpgradeInProgress = new AtomicBoolean(false);

    /** Fixed size list (dropping oldest additions when running out of space) to prevent all memory from being consumed if attached process never detaches. */
    private List<LogEvent> logged = new LinkedList<LogEvent>() {
        private static final long serialVersionUID = 1L;
        private static final int MAX_ENTRIES_IN_LIST = 10000;

        @Override
        public boolean add(final LogEvent loggingEvent) {
            // Hard code a filter so we only keep DEBUG and above here in the in-memory buffer
            if (!loggingEvent.getLevel().isLessSpecificThan(Level.DEBUG)) {
                return false;
            }
            final boolean added = super.add(loggingEvent);
            while (added && size()>MAX_ENTRIES_IN_LIST) {
                super.remove();
            }
            return added;
        }  
    };
    
    @Override
    public boolean isPostUpgradeInProgress() {
        return postUpgradeInProgress.get();
    }

    @Override
    public boolean setPostUpgradeInProgressIfDifferent(boolean newValue) {
        logged.clear();
        return this.postUpgradeInProgress.compareAndSet(!newValue, newValue);
    }
    
    @Override
    public void resetPostUpgradeInProgress() {
        this.postUpgradeInProgress.set(false);
    }
    
    @Override
    public List<LogEvent> getLogged() {
        return logged;
    }
    
    @Override
    public void logAppenderAttach(final Logger log) {
        log.addAppender(appender);
    }

    @Override
    public void logAppenderDetach(final Logger log) {
        log.removeAppender(appender);
        
    }
}
