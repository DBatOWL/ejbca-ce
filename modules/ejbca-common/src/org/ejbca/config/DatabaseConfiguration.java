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

package org.ejbca.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses embedded or overridden database.properties for info.
 */
public class DatabaseConfiguration {

	public static final String CONFIG_DATASOURCENAME = "datasource.jndi-name";
    public static final String CONFIG_DATABASENAME = "database.name";
    public static final String PROPERTY_HIBERNATE_DIALECT = "hibernate.dialect";

	public static String getFullDataSourceJndiName() {
		return InternalConfiguration.getDataSourceJndiNamePrefix() + EjbcaConfigurationHolder.getString(CONFIG_DATASOURCENAME);
	}

    /**
     * Returns the database name in priority (higher to lower):
     * <ul>
     *     <li>database.name as environment variable;</li>
     *     <li>database.name as property in database.properties;</li>
     *     <li>otherwise - hsqldb.</li>
     * </ul>
     * @return the database name.
     */
    public static String getDatabaseName() {
        final String databaseNameFromEnv = System.getProperty(CONFIG_DATABASENAME);
        final String databaseNameFromProperties = EjbcaConfigurationHolder.getString(CONFIG_DATABASENAME);
        if(StringUtils.isNotBlank(databaseNameFromEnv)) {
            return databaseNameFromEnv;
        }
        if (StringUtils.isBlank(databaseNameFromProperties)) {
            return "hsqldb";
        }
        return databaseNameFromProperties;
    }

    /**
     * Returns the hibernate dialect for database in priority (higher to lower):
     * <ul>
     *     <li>hibernate.dialect as environment variable;</li>
     *     <li>hibernate.dialect as property in database.properties;</li>
     *     <li>otherwise - null.</li>
     * </ul>
     * @return the hibernate dialect.
     */
    public static String getHibernateDialect() {
        final String hibernateDialectFromEnv = System.getProperty(PROPERTY_HIBERNATE_DIALECT);
        final String hibernateDialectFromProperties = EjbcaConfigurationHolder.getString(PROPERTY_HIBERNATE_DIALECT);
        if(StringUtils.isNotBlank(hibernateDialectFromEnv)) {
            return hibernateDialectFromEnv;
        }
        return hibernateDialectFromProperties;
    }
}
