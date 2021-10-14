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

import org.apache.commons.lang3.StringUtils;

/**
 * A wrapper for PersistenceUnitInfo to support runtime configuration in persistence.xml of:
 * <ul>
 *     <li><mapping-file/>: to support different orm files on top of database name configuration;</li>
 *     <li><property name="hibernate.dialect"/>: to support different dialects.</li>
 * </ul>
 */
public class RuntimePersistenceUnitInfo implements PersistenceUnitInfo {

    private final PersistenceUnitInfo persistenceUnitInfo;

    public RuntimePersistenceUnitInfo(final PersistenceUnitInfo persistenceUnitInfo) {
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
        final String hibernateDialect = DatabaseConfiguration.getHibernateDialect();
        System.out.println(hibernateDialect);
        if(StringUtils.isNotBlank(hibernateDialect)) {
            final Properties properties = new Properties(persistenceUnitInfo.getProperties());
            properties.setProperty(DatabaseConfiguration.PROPERTY_HIBERNATE_DIALECT, hibernateDialect);
            return properties;
        }
        return persistenceUnitInfo.getProperties();
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
}
