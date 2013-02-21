/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority						  *
 *																	   *
 *  This software is free software; you can redistribute it and/or	   *
 *  modify it under the terms of the GNU Lesser General Public		   *
 *  License as published by the Free Software Foundation; either		 *
 *  version 2.1 of the License, or any later version.					*
 *																	   *
 *  See terms of license at gnu.org.									 *
 *																	   *
 *************************************************************************/

package org.ejbca.core.protocol.cmp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStrictStyle;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509Principal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.ca.CaSessionTest;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileExistsException;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.keys.token.CryptoTokenManagementSessionTest;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.core.ejb.config.ConfigurationSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.core.protocol.unid.UnidFnrHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the unid-fnr plugin. Read the assert printout {@link #test01()} to understand how to set things up for the test.
 * 
 * @author primelars
 * @version $Id$
 */
public class CmpRAUnidTest extends CmpTestCase {

    private static final Logger log = Logger.getLogger(CmpRAUnidTest.class);
    private final AuthenticationToken admin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("CmpRAUnidTest"));

    private static final String PBEPASSWORD = "password";
    private static final String UNIDPREFIX = "1234-5678-";
    private static final String CPNAME = UNIDPREFIX + CmpRAUnidTest.class.getName();
    private static final String EEPNAME = UNIDPREFIX + CmpRAUnidTest.class.getName();

    /**
     * SUBJECT_DN of user used in this test, this contains special, escaped, characters to test that this works with CMP RA operations
     */
    private static final String FNR = "90123456789";
    private static final String LRA = "01234";
    private static final String SUBJECT_SN = FNR + '-' + LRA;
    private static final String SUBJECT_DN = "C=SE,SN=" + SUBJECT_SN + ",CN=unid-frn";

    private String issuerDN;
    private KeyPair keys;
    private int caid;
    private X509Certificate cacert;
    private CA testx509ca;

    private final CaSessionRemote caSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class);
    private final CertificateProfileSessionRemote certificateProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class);
    private final ConfigurationSessionRemote configurationSession = EjbRemoteHelper.INSTANCE.getRemoteSession(ConfigurationSessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private final EndEntityProfileSessionRemote endEntityProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);;

    @BeforeClass
    public static void beforeClass() {
        CryptoProviderTools.installBCProvider();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        issuerDN = "CN=TestCA";
        int keyusage = X509KeyUsage.digitalSignature + X509KeyUsage.keyCertSign + X509KeyUsage.cRLSign;
        testx509ca = CaSessionTest.createTestX509CA(issuerDN, null, false, keyusage);
        caid = testx509ca.getCAId();
        cacert = (X509Certificate) testx509ca.getCACertificate();
        caSession.addCA(admin, testx509ca);
        
        // Configure CMP for this test
        updatePropertyOnServer(CmpConfiguration.CONFIG_OPERATIONMODE, "ra");
        updatePropertyOnServer(CmpConfiguration.CONFIG_ALLOWRAVERIFYPOPO, "true");
        updatePropertyOnServer(CmpConfiguration.CONFIG_RESPONSEPROTECTION, "pbe");
        updatePropertyOnServer(CmpConfiguration.CONFIG_RA_AUTHENTICATIONSECRET, PBEPASSWORD);
        updatePropertyOnServer(CmpConfiguration.CONFIG_RA_CERTIFICATEPROFILE, "KeyId");
        updatePropertyOnServer(CmpConfiguration.CONFIG_RA_ENDENTITYPROFILE, "KeyId");
        updatePropertyOnServer(CmpConfiguration.CONFIG_RACANAME, testx509ca.getName());
        updatePropertyOnServer(CmpConfiguration.CONFIG_AUTHENTICATIONMODULE, CmpConfiguration.AUTHMODULE_REG_TOKEN_PWD + ";" + CmpConfiguration.AUTHMODULE_HMAC);
        updatePropertyOnServer(CmpConfiguration.CONFIG_AUTHENTICATIONPARAMETERS, "-;-");
        updatePropertyOnServer(CmpConfiguration.CONFIG_CERTREQHANDLER_CLASS, UnidFnrHandler.class.getName());
        // Configure a Certificate profile (CmpRA) using ENDUSER as template
        if (this.certificateProfileSession.getCertificateProfile(CPNAME) == null) {
            final CertificateProfile cp = new CertificateProfile(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER);
            try { // TODO: Fix this better
                this.certificateProfileSession.addCertificateProfile(this.admin, CPNAME, cp);
            } catch (CertificateProfileExistsException e) {
                log.error("Certificate profile exists: ", e);
            }
        }
        final int cpId = this.certificateProfileSession.getCertificateProfileId(CPNAME);
        if (this.endEntityProfileSession.getEndEntityProfile(EEPNAME) == null) {
            final EndEntityProfile eep = new EndEntityProfile(true);
            eep.setValue(EndEntityProfile.AVAILCERTPROFILES, 0, "" + cpId);
            try {
                this.endEntityProfileSession.addEndEntityProfile(this.admin, EEPNAME, eep);
            } catch (EndEntityProfileExistsException e) {
                log.error("Could not create end entity profile.", e);
            }
        }
        this.keys = KeyTools.genKeys("512", AlgorithmConstants.KEYALGORITHM_RSA);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        this.endEntityProfileSession.removeEndEntityProfile(this.admin, EEPNAME);
        this.certificateProfileSession.removeCertificateProfile(this.admin, CPNAME);
        
        CryptoTokenManagementSessionTest.removeCryptoToken(null, testx509ca.getCAToken().getCryptoTokenId());
        caSession.removeCA(admin, caid);
        
        assertTrue("Unable to clean up properly.", this.configurationSession.restoreConfiguration());
    }
    
    public String getRoleName() {
        return this.getClass().getSimpleName(); 
    }

    @Override
    protected void checkDN(String sExpected, X500Name actual) {
        final X500Name expected = new X500Name(sExpected);
        final ASN1ObjectIdentifier[] expectedOIDs = expected.getAttributeTypes();
        final ASN1ObjectIdentifier[] actualOIDs = actual.getAttributeTypes();
        assertEquals("Not the expected number of elements in the created certificate.", expectedOIDs.length, actualOIDs.length);
        String expectedValue, actualValue;
        for (int i = 0; i < expectedOIDs.length; i++) {
            final ASN1ObjectIdentifier oid = expectedOIDs[i];
            expectedValue = expected.getRDNs(oid)[0].getFirst().getValue().toString();
            actualValue = actual.getRDNs(oid)[0].getFirst().getValue().toString();
            if (!oid.equals(BCStrictStyle.SN)) {
                log.debug("Check that " + oid.getId() + " is OK. Expected '" + expectedValue + "'. Actual '" + actualValue + "'.");
                assertEquals("Not expected " + oid, expectedValue, actualValue);
                continue;
            }
            log.debug("Special handling of the SN " + oid.getId() + ". Input '" + expectedValue + "'. Transformed '" + actualValue
                    + "'.");
            final String expectedSNPrefix = UNIDPREFIX + LRA;
            final String actualSNPrefix = actualValue.substring(0, expectedSNPrefix.length());
            assertEquals("New serial number prefix not as expected.", expectedSNPrefix, actualSNPrefix);
            final String actualSNRandom = actualValue.substring(expectedSNPrefix.length());
            assertTrue("Random in serial number not OK: " + actualSNRandom, Pattern.compile("^\\w{6}$").matcher(actualSNRandom).matches());
        }
    }

    @Test
    public void test01() throws Exception {
        final Connection connection;
        final String host = "localhost";
        final String user = "uniduser";
        final String pass = "unidpass";
        final String name = "unid";
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/" + name, user, pass);
        } catch (SQLException e) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            pw.println();
            pw.println("You have not set up a unid-fnr DB properly to run the test.");
            pw.println("If you don't bother about it (don't if you don't know what it is) please just ignore this error.");
            pw.println("But if you want to run the test please make sure that the mysql unid-fnr DB is set up.");
            pw.println("Then execute next line at the mysql prompt:");
            pw.println("mysql> grant all on " + name + ".* to " + user + "@'" + host + "' identified by '" + pass + "';");
            pw.println("And then create the DB:");
            pw.println("$ mysqladmin -u" + host + " -u" + user + " -p" + pass + " create " + name + ";.");
            pw.println("These properties must the also be defined for the jboss data source. The name of the DS must be set in cmp.properties. Not that the datasource must be a 'no-tx-datasource', like OcspDS.");
            pw.println("You also have to set the path to the 'mysql.jar' as the 'mysql.lib' system property for the test.");
            pw.println("Example how to the test with this property:");
            pw.println("ant -Dmysql.lib=/usr/share/java/mysql.jar test:run");
            log.error(sw, e);
            assertTrue(sw.toString(), false);
            return;
        }
        try {
            doTest(connection);
        } finally {
            connection.close();
        }
    }

    private void doTest(Connection dbConn) throws Exception {

        final byte[] nonce = CmpMessageHelper.createSenderNonce();
        final byte[] transid = CmpMessageHelper.createSenderNonce();
        final int reqId;
        final String unid;
        {
            // In this test SUBJECT_DN contains special, escaped characters to verify
            // that that works with CMP RA as well
            final PKIMessage one = genCertReq(this.issuerDN, SUBJECT_DN, this.keys, this.cacert, nonce, transid, true, null, null, null, null, null, null);
            final PKIMessage req = protectPKIMessage(one, false, PBEPASSWORD, CPNAME, 567);
            assertNotNull(req);

            CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
            reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
            final ByteArrayOutputStream bao = new ByteArrayOutputStream();
            final DEROutputStream out = new DEROutputStream(bao);
            out.writeObject(req);
            final byte[] ba = bao.toByteArray();
            // Send request and receive response
            final byte[] resp = sendCmpHttp(ba, 200);
            checkCmpResponseGeneral(resp, this.issuerDN, SUBJECT_DN, this.cacert, nonce, transid, false, PBEPASSWORD);
            final X509Certificate cert = checkCmpCertRepMessage(SUBJECT_DN, this.cacert, resp, reqId);
            unid = (String) new X509Principal(cert.getSubjectX500Principal().getEncoded()).getValues(BCStrictStyle.SN).get(0);
            log.debug("Unid: " + unid);
        }
        {
            final PreparedStatement ps = dbConn.prepareStatement("select fnr from UnidFnrMapping where unid=?");
            ps.setString(1, unid);
            final ResultSet result = ps.executeQuery();
            assertTrue("Unid '" + unid + "' not found in DB.", result.next());
            final String fnr = result.getString(1);
            log.debug("FNR read from DB: " + fnr);
            assertEquals("Right FNR not found in DB.", FNR, fnr);
        }
        {
            // Send a confirm message to the CA
            final String hash = "foo123";
            final PKIMessage confirm = genCertConfirm(SUBJECT_DN, this.cacert, nonce, transid, hash, reqId);
            assertNotNull(confirm);
            final PKIMessage req1 = protectPKIMessage(confirm, false, PBEPASSWORD, CPNAME, 567);
            final ByteArrayOutputStream bao = new ByteArrayOutputStream();
            final DEROutputStream out = new DEROutputStream(bao);
            out.writeObject(req1);
            final byte[] ba = bao.toByteArray();
            // Send request and receive response
            final byte[] resp = sendCmpHttp(ba, 200);
            checkCmpResponseGeneral(resp, this.issuerDN, SUBJECT_DN, this.cacert, nonce, transid, false, PBEPASSWORD);
            checkCmpPKIConfirmMessage(SUBJECT_DN, this.cacert, resp);
        }
    }

}
