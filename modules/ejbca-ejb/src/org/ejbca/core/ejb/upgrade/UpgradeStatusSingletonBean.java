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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Logger;

/**
 * Singleton responsible for keep track of a node-local post upgrade.
 * 
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class UpgradeStatusSingletonBean implements UpgradeStatusSingletonLocal {

    private AtomicBoolean postUpgradeInProgress = new AtomicBoolean(false);

    private UpgradeListAppender appender = UpgradeListAppender.createAppender("UpgradeEventListAppender", null);

    @Override
    public boolean isPostUpgradeInProgress() {
        return postUpgradeInProgress.get();
    }

    @Override
    public boolean setPostUpgradeInProgressIfDifferent(boolean newValue) {
        appender.getLogged().clear();
        return this.postUpgradeInProgress.compareAndSet(!newValue, newValue);
    }

    @Override
    public void resetPostUpgradeInProgress() {
        this.postUpgradeInProgress.set(false);
    }

    @Override
    public List<LogEvent> getLogged() {
        return appender.getLogged();
    }

    @Override
    public void logAppenderAttach(final Logger log) {
        appender.start();
        ((org.apache.logging.log4j.core.Logger) log).addAppender(appender);
    }

    @Override
    public void logAppenderDetach(final Logger log) {
        appender.stop();
        ((org.apache.logging.log4j.core.Logger) log).removeAppender(appender);
    }
}
