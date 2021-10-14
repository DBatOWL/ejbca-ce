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

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * PersistenceProvider with support of runtime configuration, based on the HibernatePersistenceProvider.
 * @see org.hibernate.jpa.HibernatePersistenceProvider
 * @see RuntimePersistenceUnitInfo
 */
public class RuntimePersistenceProvider extends org.hibernate.jpa.HibernatePersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
        return super.createEntityManagerFactory(persistenceUnitName, properties);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo, Map map) {
        return super.createContainerEntityManagerFactory(new RuntimePersistenceUnitInfo(persistenceUnitInfo), map);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
        super.generateSchema(new RuntimePersistenceUnitInfo(persistenceUnitInfo), map);
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return super.generateSchema(persistenceUnitName, map);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return super.getProviderUtil();
    }
}
