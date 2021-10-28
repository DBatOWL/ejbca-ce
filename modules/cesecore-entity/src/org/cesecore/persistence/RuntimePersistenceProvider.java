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

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.apache.commons.collections.CollectionUtils;

/**
 * A persistence provider wrapper to configure the actual PersistenceProvider with runtime values.
 * <br/>
 * This solution assumes usage of two persistence providers:
 * <ul>
 *     <li>The default PersistenceProvider supplied by application server, activated by "default" unused persistent unit;</li>
 *     <li>RuntimePersistenceProvider - a persistence provider wrapper, with ability to reconfigure the default provider
 *     and reuse it for the actual persistence unit.</li>
 * </ul>
 * The configuration of persistence.xml may look like:
 * <pre>
 * &lt;persistence version="2.1"
 *     xmlns="http://xmlns.jcp.org/xml/ns/persistence"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
 *                         http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"&gt;
 *
 *     &lt;!-- A default persistence unit to activate the default PersistenceProvider of the application server (eg.
 *     HibernatePersistenceProvider) --&gt;
 *     &lt;persistence-unit name="default" transaction-type="JTA"&gt;
 *         &lt;jta-data-source&gt;java:/EjbcaDS&lt;/jta-data-source&gt;
 *         &lt;mapping-file&gt;META-INF/orm-default.xml&lt;/mapping-file&gt;
 *         &lt;exclude-unlisted-classes&gt;true&lt;/exclude-unlisted-classes&gt;
 *         &lt;properties&gt;
 *             &lt;property name="hibernate.hbm2ddl.auto" value="none"/&gt;
 *         &lt;/properties&gt;
 *     &lt;/persistence-unit&gt;
 *
 *
 *     &lt;persistence-unit name="ejbca" transaction-type="JTA"&gt;
 *         &lt;!-- Persistence provider to support runtime override --&gt;
 *         &lt;provider&gt;org.ejbca.config.RuntimePersistenceProvider&lt;/provider&gt;
 *         &lt;jta-data-source&gt;java:/EjbcaDS&lt;/jta-data-source&gt;
 *         &lt;!-- Specify variable here that can be substituted at runtime. --&gt;
 *         &lt;mapping-file&gt;META-INF/orm-ejbca-h2.xml&lt;/mapping-file&gt;
 *         &lt;properties&gt;
 *             &lt;!-- Specify variable here that can be substituted at runtime. --&gt;
 *             &lt;property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/&gt;
 *             &lt;property name="hibernate.hbm2ddl.auto" value="update"/&gt;
 *             &lt;property name="hibernate.query.jpaql_strict_compliance" value="true"/&gt;
 *         &lt;/properties&gt;
 *     &lt;/persistence-unit&gt;
 * &lt;/persistence&gt;
 * </pre>
 * @see RuntimePersistenceUnitInfo
 */
public class RuntimePersistenceProvider implements PersistenceProvider {

    private static final PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();

    @Override
    public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
        return getPersistenceProvider().createEntityManagerFactory(persistenceUnitName, properties);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo, Map map) {
        final PersistenceProvider persistenceProvider = getPersistenceProvider();
        return persistenceProvider.createContainerEntityManagerFactory(
                new RuntimePersistenceUnitInfo(persistenceProvider.getClass().getName(), persistenceUnitInfo),
                map
        );
    }

    @Override
    public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
        getPersistenceProvider().generateSchema(persistenceUnitInfo, map);
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return getPersistenceProvider().generateSchema(persistenceUnitName, map);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return getPersistenceProvider().getProviderUtil();
    }

    /**
     * Returns the first possible PersistenceProvider excluding itself.
     * @return the first possible PersistenceProvider.
     * @throws PersistenceException In case of missing PersistenceProvider.
     */
    private PersistenceProvider getPersistenceProvider() {
        final List<PersistenceProvider> providers = resolver.getPersistenceProviders();
        if(CollectionUtils.isNotEmpty(providers)) {
            final String thisProviderName = this.getClass().getName();
            for (PersistenceProvider provider : providers) {
                final String providerName = provider.getClass().getName();
                if(!thisProviderName.equals(providerName)) {
                    return provider;
                }
            }
        }
        throw new PersistenceException("Cannot find the PersistenceProvider to use, check for PersistenceProvider activation.");
    }
}
