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
package org.cesecore.persistence;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.cesecore.config.DatabaseConfiguration;
import org.junit.Before;
import org.junit.Test;

import static javax.persistence.SharedCacheMode.UNSPECIFIED;
import static javax.persistence.ValidationMode.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A regression test for RuntimePersistenceUnitInfoUnit to validate override of configuration:
 * <ul>
 *     <li>database.name;</li>
 *     <li>hibernate.dialect.</li>
 * </ul>
 * These properties depend on runtime configuration.
 */
public class RuntimePersistenceUnitInfoUnitTest {

    private static final String PERSISTENCE_UNIT_NAME = "PersistenceUnitName";
    private static final String PERSISTENCE_PROVIDER_CLASS_NAME = "PersistenceProviderClassName";
    private static final String PERSISTENCE_PROVIDER_NEW_CLASS_NAME = "ServerPersistenceProviderClassName";
    private static final PersistenceUnitTransactionType PERSISTENCE_UNIT_TRANSACTION_TYPE = PersistenceUnitTransactionType.JTA;
    private static final String JTA_DATASOURCE_NAME = "JtaDataSource";
    private static final String NON_JTA_DATASOURCE_NAME = "NonJtaDataSource";
    private static final List<String> MAPPING_FILE_NAMES = Arrays.asList("orm-my-db-1.xml", "orm-my-db-2.xml");
    private static final String JAR_FILE_URL = "jar:file:/c://my.jar!/";
    private static final String PERSISTENCE_UNIT_ROOT_URL = "jar:file:/c://root.jar!/";
    private static final List<String> MANAGED_CLASS_NAMES = Arrays.asList("MyClass", "MyClass1");
    private static final SharedCacheMode SHARED_CACHE_MODE = UNSPECIFIED;
    private static final ValidationMode VALIDATION_MODE = NONE;
    private static final String HIBERNATE_DIALECT = "MyNewDialect";
    private static final String PERSISTENCE_XML_SCHEMA_VERSION = "0.1.0.2";
    //
    private PersistenceUnitInfo originalPersistenceUnitInfo;

    /**
     * A test class implementing DataSource.
     */
    private static class TestDataSource implements DataSource {

        private final String name;

        public TestDataSource(final String name) {
            this.name = name;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    /**
     * A test class implementing PersistenceUnitInfo with fixed values.
     */
    private static class TestPersistenceUnitInfo implements PersistenceUnitInfo {

        @Override
        public String getPersistenceUnitName() {
            return PERSISTENCE_UNIT_NAME;
        }

        @Override
        public String getPersistenceProviderClassName() {
            return PERSISTENCE_PROVIDER_CLASS_NAME;
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return PERSISTENCE_UNIT_TRANSACTION_TYPE;
        }

        @Override
        public DataSource getJtaDataSource() {
            return new TestDataSource(JTA_DATASOURCE_NAME);
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return new TestDataSource(NON_JTA_DATASOURCE_NAME);
        }

        @Override
        public List<String> getMappingFileNames() {
            return MAPPING_FILE_NAMES;
        }

        @Override
        public List<URL> getJarFileUrls() {
            try {
                return Collections.singletonList(new URL(JAR_FILE_URL));
            } catch (MalformedURLException ignored) {
            }
            return null;
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            try {
                return new URL(PERSISTENCE_UNIT_ROOT_URL);
            } catch (MalformedURLException ignored) {
            }
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return MANAGED_CLASS_NAMES;
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return true;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return SHARED_CACHE_MODE;
        }

        @Override
        public ValidationMode getValidationMode() {
            return VALIDATION_MODE;
        }

        @Override
        public Properties getProperties() {
            final Properties properties = new Properties();
            properties.setProperty(RuntimePersistenceUnitInfo.PROPERTY_HIBERNATE_DIALECT, HIBERNATE_DIALECT);
            return properties;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return PERSISTENCE_XML_SCHEMA_VERSION;
        }

        @Override
        public ClassLoader getClassLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        public void addTransformer(ClassTransformer classTransformer) {
        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return this.getClass().getClassLoader();
        }
    }

    @Before
    public void setUp() {
        originalPersistenceUnitInfo = new TestPersistenceUnitInfo();
    }

    @Test
    public void shouldOverridePersistenceProviderClassName() {
        // given
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME, originalPersistenceUnitInfo
        );
        // then
        assertNotNull("PersistenceProviderClassName null", actualPersistenceUnitInfo.getPersistenceProviderClassName());
        assertEquals("PersistenceProviderClassName mismatch", PERSISTENCE_PROVIDER_NEW_CLASS_NAME, actualPersistenceUnitInfo.getPersistenceProviderClassName());
    }

    @Test
    public void shouldOverrideMappingFileNames() {
        // given
        System.setProperty(DatabaseConfiguration.CONFIG_DATABASE_NAME, "db");
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME, originalPersistenceUnitInfo
        );
        // then
        assertNotNull("MappingFileNames null", actualPersistenceUnitInfo.getMappingFileNames());
        assertTrue("MappingFileNames mismatch", actualPersistenceUnitInfo.getMappingFileNames()
                .contains("META-INF/orm-ejbca-db.xml"));
    }

    @Test
    public void shouldOverrideMappingFileNamesIgnoringCase() {
        // given
        System.setProperty(DatabaseConfiguration.CONFIG_DATABASE_NAME, "DbD");
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME, originalPersistenceUnitInfo
        );
        // then
        assertNotNull("MappingFileNames null", actualPersistenceUnitInfo.getMappingFileNames());
        assertTrue("MappingFileNames mismatch", actualPersistenceUnitInfo.getMappingFileNames()
                .contains("META-INF/orm-ejbca-dbd.xml"));
    }

    @Test
    public void shouldLeavePropertyHibernateDialect() {
        // given
        System.setProperty(DatabaseConfiguration.CONFIG_HIBERNATE_DIALECT, "");
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME, originalPersistenceUnitInfo
        );
        // then
        assertNotNull("Properties null", actualPersistenceUnitInfo.getProperties());
        assertNotNull("hibernate.dialect null", actualPersistenceUnitInfo.getProperties().getProperty(RuntimePersistenceUnitInfo.PROPERTY_HIBERNATE_DIALECT));
        assertEquals("hibernate.dialect mismatch", HIBERNATE_DIALECT, actualPersistenceUnitInfo.getProperties().getProperty(RuntimePersistenceUnitInfo.PROPERTY_HIBERNATE_DIALECT));
    }

    @Test
    public void shouldOverridePropertyHibernateDialect() {
        // given
        System.setProperty(DatabaseConfiguration.CONFIG_HIBERNATE_DIALECT, "AwesomeDialect");
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME, originalPersistenceUnitInfo
        );
        // then
        assertNotNull("Properties null", actualPersistenceUnitInfo.getProperties());
        assertNotNull("hibernate.dialect null", actualPersistenceUnitInfo.getProperties().getProperty(RuntimePersistenceUnitInfo.PROPERTY_HIBERNATE_DIALECT));
        assertEquals("hibernate.dialect mismatch", "AwesomeDialect", actualPersistenceUnitInfo.getProperties().getProperty(RuntimePersistenceUnitInfo.PROPERTY_HIBERNATE_DIALECT));
    }

    /**
     * Checks for consistency of values.
     */
    @Test
    public void shouldLeaveUnchanged() throws Exception {
        // given
        // when
        final RuntimePersistenceUnitInfo actualPersistenceUnitInfo = new RuntimePersistenceUnitInfo(
                PERSISTENCE_PROVIDER_NEW_CLASS_NAME ,originalPersistenceUnitInfo
        );
        // then
        assertEquals("PersistenceUnitName mismatch", PERSISTENCE_UNIT_NAME, actualPersistenceUnitInfo.getPersistenceUnitName());
        assertEquals("PersistenceProviderClassName mismatch", PERSISTENCE_PROVIDER_NEW_CLASS_NAME, actualPersistenceUnitInfo.getPersistenceProviderClassName());
        assertEquals("TransactionType mismatch", PERSISTENCE_UNIT_TRANSACTION_TYPE, actualPersistenceUnitInfo.getTransactionType());
        assertEquals("JtaDataSource mismatch", JTA_DATASOURCE_NAME,actualPersistenceUnitInfo.getJtaDataSource().toString());
        assertEquals("NonJtaDataSource mismatch", NON_JTA_DATASOURCE_NAME, actualPersistenceUnitInfo.getNonJtaDataSource().toString());
        assertNotNull("JarFileUrls null", actualPersistenceUnitInfo.getJarFileUrls());
        assertTrue("JarFileUrls mismatch", actualPersistenceUnitInfo.getJarFileUrls().contains(new URL(JAR_FILE_URL)));
        assertEquals("PersistenceUnitRootUrl mismatch", new URL(PERSISTENCE_UNIT_ROOT_URL), actualPersistenceUnitInfo.getPersistenceUnitRootUrl());
        assertEquals("ManagedClassNames mismatch", MANAGED_CLASS_NAMES, actualPersistenceUnitInfo.getManagedClassNames());
        assertTrue("ExcludeUnlistedClasses mismatch", actualPersistenceUnitInfo.excludeUnlistedClasses());
        assertEquals("SharedCacheMode mismatch", SHARED_CACHE_MODE, actualPersistenceUnitInfo.getSharedCacheMode());
        assertEquals("ValidationMode mismatch", VALIDATION_MODE, actualPersistenceUnitInfo.getValidationMode());
        assertEquals("PersistenceXMLSchemaVersion mismatch", PERSISTENCE_XML_SCHEMA_VERSION, actualPersistenceUnitInfo.getPersistenceXMLSchemaVersion());
        assertEquals("ClassLoader mismatch", this.getClass().getClassLoader(), actualPersistenceUnitInfo.getClassLoader());
        assertEquals("NewTempClassLoader mismatch", this.getClass().getClassLoader(), actualPersistenceUnitInfo.getNewTempClassLoader());
    }
}
