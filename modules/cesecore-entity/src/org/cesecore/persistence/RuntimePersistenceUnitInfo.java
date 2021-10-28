/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.persistence;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.cesecore.config.DatabaseConfiguration;

/**
 * A wrapper for PersistenceUnitInfo to support runtime configuration in persistence.xml of:
 * <ul>
 *     <li>database name;</li>
 *     <li><mapping-file/>: to support different orm files on top of database name configuration;</li>
 *     <li><property name="hibernate.dialect"/>: to support different dialects.</li>
 * </ul>
 * @see PersistenceUnitInfo
 */
public class RuntimePersistenceUnitInfo implements PersistenceUnitInfo {

    /**
     * The name of Hibernate dialect property to use under &lt;properties/&gt; block.
     */
    public static final String PROPERTY_HIBERNATE_DIALECT = "hibernate.dialect";
    /**
     * Java Transaction API (JTA) property to use under &lt;properties/&gt; block.
     */
    public static final String PROPERTY_HIBERNATE_TRANSACTION_JTA_PLATFORM = "hibernate.transaction.jta.platform";

    private final String persistenceProviderClassName;
    private final PersistenceUnitInfo persistenceUnitInfo;

    /**
     * Constructor.
     *
     * @param persistenceProviderClassName Original persistence provider class name (unwrapped).
     * @param persistenceUnitInfo Original persistence unit info.
     */
    public RuntimePersistenceUnitInfo(
            final String persistenceProviderClassName, final PersistenceUnitInfo persistenceUnitInfo
    ) {
        this.persistenceProviderClassName = persistenceProviderClassName;
        this.persistenceUnitInfo = persistenceUnitInfo;
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitInfo.getPersistenceUnitName();
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persistenceUnitInfo.getPersistenceProviderClassName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return persistenceUnitInfo.getTransactionType();
    }

    @Override
    public DataSource getJtaDataSource() {
        return persistenceUnitInfo.getJtaDataSource();
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return persistenceUnitInfo.getNonJtaDataSource();
    }

    @Override
    public List<String> getMappingFileNames() {
        return Collections.singletonList(
                "META-INF/orm-ejbca-" +
                DatabaseConfiguration.getDatabaseName().toLowerCase() +
                ".xml"
        );
    }

    @Override
    public List<URL> getJarFileUrls() {
        return persistenceUnitInfo.getJarFileUrls();
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitInfo.getPersistenceUnitRootUrl();
    }

    @Override
    public List<String> getManagedClassNames() {
        return persistenceUnitInfo.getManagedClassNames();
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return persistenceUnitInfo.excludeUnlistedClasses();
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return persistenceUnitInfo.getSharedCacheMode();
    }

    @Override
    public ValidationMode getValidationMode() {
        return persistenceUnitInfo.getValidationMode();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties(persistenceUnitInfo.getProperties());
        final String hibernateDialect = DatabaseConfiguration.getHibernateDialect();
        if(StringUtils.isNotBlank(hibernateDialect)) {
            properties.setProperty(PROPERTY_HIBERNATE_DIALECT, hibernateDialect);
        }
        final String hibernateTransactionJtaPlatform = getHibernateTransactionJtaPlatform();
        if(StringUtils.isNotBlank(hibernateTransactionJtaPlatform)) {
            properties.setProperty(PROPERTY_HIBERNATE_TRANSACTION_JTA_PLATFORM, getHibernateTransactionJtaPlatform());
        }
        return properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return persistenceUnitInfo.getPersistenceXMLSchemaVersion();
    }

    @Override
    public ClassLoader getClassLoader() {
        return persistenceUnitInfo.getClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer classTransformer) {
        persistenceUnitInfo.addTransformer(classTransformer);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return persistenceUnitInfo.getNewTempClassLoader();
    }

    /**
     * Returns the full class name for JTA platform on the top of PersistenceProviderClassName.
     * @return the full class name for JTA platform or null.
     */
    private String getHibernateTransactionJtaPlatform() {
        // HibernatePersistence, HibernatePersistenceProvider
        if(persistenceProviderClassName.contains("HibernatePersistence")) {
            return "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform";
        }
        return null;
    }
}
