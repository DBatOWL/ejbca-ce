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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * This plugin is used to keep track of the upgrade logs in the GUI
 */
@Plugin(name = "UpgradeListAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class UpgradeListAppender extends AbstractAppender {
    
    private List<LogEvent> logList;
    
    protected UpgradeListAppender(String name, Filter filter) {
        super(name, filter, null, false, null);
        logList = Collections.synchronizedList(new LinkedList<LogEvent>() {
            private static final long serialVersionUID = 1L;
            private static final int MAX_ENTRIES_IN_LIST = 10000;

            @Override
            public boolean add(final LogEvent loggingEvent) {
                // Hard code a filter so we only keep DEBUG and above here in the in-memory buffer
                if (!loggingEvent.getLevel().isMoreSpecificThan(Level.DEBUG)) {
                    return false;
                }
                final boolean added = super.add(loggingEvent);
                while (added && size() > MAX_ENTRIES_IN_LIST) {
                    super.remove();
                }
                return added;
            }
        });
    }

    @PluginFactory
    public static UpgradeListAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") Filter filter) {
        return new UpgradeListAppender(name, filter);
    }

    @Override
    public void append(LogEvent event) {
        if (!event.getLevel().isMoreSpecificThan(Level.INFO)) {
            error("Unable to log less than WARN level.");
            return;
        }
        logList.add(event);
    }

    public List<LogEvent> getLogged() {
        return logList;
    }
}