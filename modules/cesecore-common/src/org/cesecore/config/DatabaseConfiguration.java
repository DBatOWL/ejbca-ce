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
package org.cesecore.config;

import org.apache.commons.lang.StringUtils;

/**
 * Parses embedded or overridden database.properties for info.
 */
public class DatabaseConfiguration {

    public static final String CONFIG_DATABASE_NAME = "database.name";
    public static final String ENV_RUNTIME_DATABASE_NAME = "runtime.database.name";
    public static final String CONFIG_DEFAULT_HIBERNATE_DIALECT_PREFIX = "hibernate.dialect.";
    public static final String ENV_RUNTIME_HIBERNATE_DIALECT = "runtime.hibernate.dialect";

    /**
     * Returns the database name in priority (higher to lower):
     * <ul>
     *     <li>runtime.database.name as environment variable;</li>
     *     <li>database.name as property in database.properties;</li>
     *     <li>otherwise - h2 (see defaultvalues.properties).</li>
     * </ul>
     * @return the database name.
     */
    public static String getDatabaseName() {
        // get from -Druntime.database.name=
        final String databaseNameFromEnv = System.getProperty(ENV_RUNTIME_DATABASE_NAME);
        if(StringUtils.isNotBlank(databaseNameFromEnv)) {
            return databaseNameFromEnv;
        }
        // get from database.properties or fallback to defaultvalues.properties
        return ConfigurationHolder.getString(CONFIG_DATABASE_NAME);
    }

    /**
     * Returns hibernate dialect for database in priority (higher to lower):
     * <ul>
     *     <li>runtime.hibernate.dialect as environment variable;</li>
     *     <li>hibernate.dialect as property in database.properties;</li>
     *     <li>otherwise - org.hibernate.dialect.H2Dialect (see defaultvalues.properties).</li>
     * </ul>
     * @return hibernate dialect.
     */
    public static String getHibernateDialect() {
        // get from -Druntime.hibernate.dialect=
        final String hibernateDialectFromEnv = System.getProperty(ENV_RUNTIME_HIBERNATE_DIALECT);
        if(StringUtils.isNotBlank(hibernateDialectFromEnv)) {
            return hibernateDialectFromEnv;
        }
        // get from database.properties or fallback to defaultvalues.properties
        return ConfigurationHolder.getString(CONFIG_DEFAULT_HIBERNATE_DIALECT_PREFIX + getDatabaseName());
    }

}
