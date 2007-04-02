/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.model.ca.publisher;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionHome;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionRemote;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.ca.publisher.ActiveDirectoryPublisher;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.core.model.ca.publisher.CustomPublisherContainer;
import org.ejbca.core.model.ca.publisher.GeneralPurposeCustomPublisher;
import org.ejbca.core.model.ca.publisher.LdapPublisher;
import org.ejbca.core.model.ca.publisher.PublisherException;
import org.ejbca.core.model.ca.publisher.PublisherExistsException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;



/**
 * Tests Publishers.
 *
 * @version $Id: TestPublisher.java,v 1.6.6.1 2007-04-02 11:10:40 jeklund Exp $
 */
public class TestPublisher extends TestCase {
    
    static byte[] testcert = Base64.decode(("MIICWzCCAcSgAwIBAgIIJND6Haa3NoAwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAyMDEw"
            + "ODA5MTE1MloXDTA0MDEwODA5MjE1MlowLzEPMA0GA1UEAxMGMjUxMzQ3MQ8wDQYD"
            + "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB"
            + "hwKBgQCQ3UA+nIHECJ79S5VwI8WFLJbAByAnn1k/JEX2/a0nsc2/K3GYzHFItPjy"
            + "Bv5zUccPLbRmkdMlCD1rOcgcR9mmmjMQrbWbWp+iRg0WyCktWb/wUS8uNNuGQYQe"
            + "ACl11SAHFX+u9JUUfSppg7SpqFhSgMlvyU/FiGLVEHDchJEdGQIBEaOBgTB/MA8G"
            + "A1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUyxKILxFM"
            + "MNujjNnbeFpnPgB76UYwHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmsw"
            + "GwYDVR0RBBQwEoEQMjUxMzQ3QGFuYXRvbS5zZTANBgkqhkiG9w0BAQUFAAOBgQAS"
            + "5wSOJhoVJSaEGHMPw6t3e+CbnEL9Yh5GlgxVAJCmIqhoScTMiov3QpDRHOZlZ15c"
            + "UlqugRBtORuA9xnLkrdxYNCHmX6aJTfjdIW61+o/ovP0yz6ulBkqcKzopAZLirX+"
            + "XSWf2uI9miNtxYMVnbQ1KPdEAt7Za3OQR6zcS0lGKg==").getBytes());
    
    static byte[] testcacert = Base64.decode(("MIICLDCCAZWgAwIBAgIISDzEq64yCAcwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAxMTIw"
            + "NDA5MzI1N1oXDTAzMTIwNDA5NDI1N1owLzEPMA0GA1UEAxMGVGVzdENBMQ8wDQYD"
            + "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB"
            + "hwKBgQCnhOvkaj+9Qmt9ZseVn8Jhl6ewTrAOK3c9usxBhiGs+TalGjuAK37bbnbZ"
            + "rlzCZpEsjSZYgXS++3NttiDbPzATkV/c33uIzBHjyk8/paOmTrkIux8hbIYMce+/"
            + "WTYnAM3J41mSuDMy2yZxZ72Yntzqg4UUXiW+JQDkhGx8ZtcSSwIBEaNTMFEwDwYD"
            + "VR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUy5k/bKQ6TtpTWhsPWFzafOFgLmswHwYD"
            + "VR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmswDQYJKoZIhvcNAQEFBQADgYEA"
            + "gHzQLoqLobU43lKvQCiZbYWEXHTf3AdzUd6aMOYOM80iKS9kgrMsnKjp61IFCZwr"
            + "OcY1lOkpjADUTSqfVJWuF1z5k9c1bXnh5zu48LA2r2dlbHqG8twMQ+tPh1MYa3lV"
            + "ugWhKqArGEawICRPUZJrLy/eDbCgVB4QT3rC7rOJOH0=").getBytes());
    
    static byte[] testcrl = Base64.decode(("MIIDEzCCAnwCAQEwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UEAxMGVGVzdENBMQ8w"
            + "DQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFFw0wMjAxMDMxMjExMTFaFw0wMjAx"
            + "MDIxMjExMTFaMIIB5jAZAggfi2rKt4IrZhcNMDIwMTAzMTIxMDUxWjAZAghAxdYk"
            + "7mJxkxcNMDIwMTAzMTIxMDUxWjAZAgg+lCCL+jumXxcNMDIwMTAzMTIxMDUyWjAZ"
            + "Agh4AAPpzSk/+hcNMDIwMTAzMTIxMDUyWjAZAghkhx9SFvxAgxcNMDIwMTAzMTIx"
            + "MDUyWjAZAggj4g5SUqaGvBcNMDIwMTAzMTIxMDUyWjAZAghT+nqB0c6vghcNMDIw"
            + "MTAzMTE1MzMzWjAZAghsBWMAA55+7BcNMDIwMTAzMTE1MzMzWjAZAgg8h0t6rKQY"
            + "ZhcNMDIwMTAzMTE1MzMzWjAZAgh7KFsd40ICwhcNMDIwMTAzMTE1MzM0WjAZAggA"
            + "kFlDNU8ubxcNMDIwMTAzMTE1MzM0WjAZAghyQfo1XNl0EBcNMDIwMTAzMTE1MzM0"
            + "WjAZAggC5Pz7wI/29hcNMDIwMTAyMTY1NDMzWjAZAggEWvzRRpFGoRcNMDIwMTAy"
            + "MTY1NDMzWjAZAggC7Q2W0iXswRcNMDIwMTAyMTY1NDMzWjAZAghrfwG3t6vCiBcN"
            + "MDIwMTAyMTY1NDMzWjAZAgg5C+4zxDGEjhcNMDIwMTAyMTY1NDMzWjAZAggX/olM"
            + "45KxnxcNMDIwMTAyMTY1NDMzWqAvMC0wHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsP"
            + "WFzafOFgLmswCgYDVR0UBAMCAQQwDQYJKoZIhvcNAQEFBQADgYEAPvYDZofCOopw"
            + "OCKVGaK1aPpHkJmu5Xi1XtRGO9DhmnSZ28hrNu1A5R8OQI43Z7xFx8YK3S56GRuY"
            + "0EGU/RgM3AWhyTAps66tdyipRavKmH6MMrN4ypW/qbhsd4o8JE9pxxn9zsQaNxYZ"
            + "SNbXM2/YxkdoRSjkrbb9DUdCmCR/kEA=").getBytes());
    
    private static Logger log = Logger.getLogger(TestPublisher.class);
    private static Context ctx;
    private static IPublisherSessionRemote pub;

    private static final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    
    private String externalCommand	= "dir";
    private String externalCommand2	= "ls";
    private final String invalidOption		= " --------------:";

    /**
     * Creates a new TestPublisher object.
     *
     * @param name name
     */
    public TestPublisher(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        log.debug(">setUp()");
        ctx = getInitialContext();
        
        Object obj = ctx.lookup("PublisherSession");
        IPublisherSessionHome home = (IPublisherSessionHome) javax.rmi.PortableRemoteObject.narrow(obj,
                IPublisherSessionHome.class);
        pub = home.create();
        
        CertTools.installBCProvider();
        
        log.debug("<setUp()");
        
    }
    
    protected void tearDown() throws Exception {
    }
    
    private Context getInitialContext() throws NamingException {
        log.debug(">getInitialContext");
        Context ctx = new javax.naming.InitialContext();
        log.debug("<getInitialContext");
        
        return ctx;
    }
    
    /**
     * adds ldap publisher
     *
     * @throws Exception error
     */
    public void test01AddLDAPPublisher() throws Exception {
        log.debug(">test01AddLDAPPublisher()");
        boolean ret = false;
        try {
            LdapPublisher publisher = new LdapPublisher();
            publisher.setHostname("localhost");
            publisher.setDescription("Used in Junit Test, Remove this one");
            pub.addPublisher(admin, "TESTLDAP", publisher);
            ret = true;
        } catch (PublisherExistsException pee) {
        }
        
        assertTrue("Creating LDAP Publisher failed", ret);
        log.debug("<test01AddLDAPPublisher()");
    }
    
    /**
     * adds ad publisher
     *
     * @throws Exception error
     */
    public void test02AddADPublisher() throws Exception {
        log.debug(">test02AddADPublisher() ");
        boolean ret = false;
        try {
            ActiveDirectoryPublisher publisher = new ActiveDirectoryPublisher();
            publisher.setHostname("localhost");
            publisher.setDescription("Used in Junit Test, Remove this one");
            pub.addPublisher(admin, "TESTAD", publisher);
            ret = true;
        } catch (PublisherExistsException pee) {
        }
        
        assertTrue("Creating AD Publisher failed", ret);
        log.debug("<test02AddADPublisher() ");
    }
    
    /**
     * adds custom publisher
     *
     * @throws Exception error
     */
    public void test03AddCustomPublisher() throws Exception {
        log.debug(">test03AddCustomPublisher()");
        boolean ret = false;
        try {
            CustomPublisherContainer publisher = new CustomPublisherContainer();
            publisher.setClassPath("org.ejbca.core.model.ca.publisher.DummyCustomPublisher");
            publisher.setDescription("Used in Junit Test, Remove this one");
            pub.addPublisher(admin, "TESTDUMMYCUSTOM", publisher);
            ret = true;
        } catch (PublisherExistsException pee) {
        }
        
        assertTrue("Creating Custom Publisher failed", ret);
        
        log.debug("<test03AddCustomPublisher()");
    }
    
    /**
     * renames publisher
     *
     * @throws Exception error
     */
    public void test04RenamePublisher() throws Exception {
        log.debug(">test04RenamePublisher()");
        
        boolean ret = false;
        try {
            pub.renamePublisher(admin, "TESTDUMMYCUSTOM", "TESTNEWDUMMYCUSTOM");
            ret = true;
        } catch (PublisherExistsException pee) {
        }
        assertTrue("Renaming Custom Publisher failed", ret);
        
        
        log.debug("<test04RenamePublisher()");
    }
    
    /**
     * clones publisher
     *
     * @throws Exception error
     */
    public void test05ClonePublisher() throws Exception {
        log.debug(">test05ClonePublisher()");
        
        boolean ret = false;
        pub.clonePublisher(admin, "TESTNEWDUMMYCUSTOM", "TESTCLONEDUMMYCUSTOM");
        ret = true;
        assertTrue("Cloning Custom Publisher failed", ret);
        
        log.debug("<test05ClonePublisher()");
    }
    
    
    /**
     * edits publisher
     *
     * @throws Exception error
     */
    public void test06EditPublisher() throws Exception {
        log.debug(">test06EditPublisher()");
        
        boolean ret = false;
        
        BasePublisher publisher = pub.getPublisher(admin, "TESTCLONEDUMMYCUSTOM");
        publisher.setDescription(publisher.getDescription().toUpperCase());
        pub.changePublisher(admin, "TESTCLONEDUMMYCUSTOM", publisher);
        ret = true;
        
        assertTrue("Editing Custom Publisher failed", ret);
        
        
        log.debug("<test06EditPublisher()");
    }
    
    /**
     * stores a cert to the dummy publisher
     *
     * @throws Exception error
     */
    public void test07StoreCertToDummy() throws Exception {
        log.debug(">test07StoreCertToDummy()");
        X509Certificate cert = CertTools.getCertfromByteArray(testcert);
        ArrayList publishers = new ArrayList();
        publishers.add(new Integer(pub.getPublisherId(admin, "TESTNEWDUMMYCUSTOM")));
        
        boolean ret = pub.storeCertificate(new Admin(Admin.TYPE_INTERNALUSER), publishers, cert, "test05", "foo123", null, CertificateDataBean.CERT_ACTIVE, CertificateDataBean.CERTTYPE_ENDENTITY, -1, RevokedCertInfo.NOT_REVOKED, null);
        assertTrue("Storing certificate to dummy publisher failed", ret);
        log.debug("<test07StoreCertToDummyr()");
    }
    
    /**
     * stores a cert to the dummy publisher
     *
     * @throws Exception error
     */
    public void test08storeCRLToDummy() throws Exception {
        log.debug(">test08storeCRLToDummy()");
        
        ArrayList publishers = new ArrayList();
        publishers.add(new Integer(pub.getPublisherId(admin, "TESTNEWDUMMYCUSTOM")));
        boolean ret = pub.storeCRL(admin, publishers, testcrl, null, 1);
        assertTrue("Storing CRL to dummy publisher failed", ret);
        
        log.debug("<test08storeCRLToDummy()");
    }
    
    
    /**
     * removes all publishers
     *
     * @throws Exception error
     */
    public void test09removePublishers() throws Exception {
        log.debug(">test09removePublishers()");
        boolean ret = false;
        try {
            pub.removePublisher(admin, "TESTLDAP");
            pub.removePublisher(admin, "TESTAD");
            pub.removePublisher(admin, "TESTNEWDUMMYCUSTOM");
            pub.removePublisher(admin, "TESTCLONEDUMMYCUSTOM");
            ret = true;
        } catch (Exception pee) {
        }
        assertTrue("Removing Publisher failed", ret);
        
        log.debug("<test09removePublishers()");
    }

	/**
	 * Test normal operation of GeneralPurposeCustomPublisher.
	 *
	 * @throws Exception error
	 */
	public void test10GenPurpCustPubl() throws Exception {
	    log.debug(">test10GenPurpCustPubl()");
	    
	    GeneralPurposeCustomPublisher gpcPublisher = null;
	    Properties props = new Properties();
	
	    //Make sure an external command exists for testing purposes
	    boolean ret = false;
	    if ( !isValidCommand(externalCommand) ) {
	    	externalCommand = externalCommand2; 
	    }
	    if ( !isValidCommand(externalCommand) ) {
	    	ret = true; 
	    }
	    assertFalse("This test requires \"" + externalCommand + "\" to be available.", ret);
	    // Create
    	gpcPublisher = new GeneralPurposeCustomPublisher();
	    // Make sure it fails without a given external command
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.crlExternalCommandPropertyName, "");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Store CRL with GeneralPurposeCustomPublisher did not failed with invalid properties.", ret);
	    // Test function by calling a command that is available on most platforms 
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.crlExternalCommandPropertyName, externalCommand);
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertTrue("Store CRL with GeneralPurposeCustomPublisher failed.", ret);
	    log.debug("<test10GenPurpCustPubl()");
	} // test10GenPurpCustPubl

	/**
	 * Verify that GeneralPurposeCustomPublisher will fail on an error code from
	 * an external application. 
	 *
	 * @throws Exception error
	 */
	public void test11GenPurpCustPublErrorCode() throws Exception {
	    log.debug(">test11GenPurpCustPublErrorCode()");
	    
	    GeneralPurposeCustomPublisher gpcPublisher = null;
	    Properties props = new Properties();
	
	    //Make sure an external command exists for testing purposes
	    boolean ret = false;
	    if ( !isValidCommand(externalCommand) ) {
	    	externalCommand = externalCommand2; 
	    }
	    if ( !isValidCommand(externalCommand) ) {
	    	ret = true; 
	    }
	    assertFalse("This test requires \"" + externalCommand + "\" to be available.", ret);
	    // Create
    	gpcPublisher = new GeneralPurposeCustomPublisher();
	    // Test function by calling a command that is available on most platforms with invalid option
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.crlExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.crlFailOnErrorCodePropertyName, "true");
	        props.setProperty(GeneralPurposeCustomPublisher.crlFailOnStandardErrorPropertyName, "false");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Store CRL with GeneralPurposeCustomPublisher did not fail on errorcode.", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.certExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.certFailOnErrorCodePropertyName, "true");
	        props.setProperty(GeneralPurposeCustomPublisher.certFailOnStandardErrorPropertyName, "false");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Store cert with GeneralPurposeCustomPublisher did not fail on errorcode.", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.revokeExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.revokeFailOnErrorCodePropertyName, "true");
	        props.setProperty(GeneralPurposeCustomPublisher.revokeFailOnStandardErrorPropertyName, "false");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Revoke cert with GeneralPurposeCustomPublisher did not fail on errorcode.", ret);
	    log.debug("<test11GenPurpCustPublErrorCode()");
	} // test11GenPurpCustPublErrorCode
	    
	/**
	 * Verify that GeneralPurposeCustomPublisher will fail on output to standard
	 * error from an external application. 
	 *
	 * @throws Exception error
	 */
	public void test12GenPurpCustPublStandardError() throws Exception {
	    log.debug(">test12GenPurpCustPublStandardError()");
	    
	    GeneralPurposeCustomPublisher gpcPublisher = null;
	    Properties props = new Properties();
	
	    //Make sure an external command exists for testing purposes
	    boolean ret = false;
	    if ( !isValidCommand(externalCommand) ) {
	    	externalCommand = externalCommand2; 
	    }
	    if ( !isValidCommand(externalCommand) ) {
	    	ret = true; 
	    }
	    assertFalse("This test requires \"" + externalCommand + "\" to be available.", ret);
	    // Create
    	gpcPublisher = new GeneralPurposeCustomPublisher();
	    // Test function by calling a command that is available on most platforms with invalid option 
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.crlExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.crlFailOnErrorCodePropertyName, "false");
	        props.setProperty(GeneralPurposeCustomPublisher.crlFailOnStandardErrorPropertyName, "true");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Store CRL with GeneralPurposeCustomPublisher did not fail on standard error.", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.certExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.certFailOnErrorCodePropertyName, "false");
	        props.setProperty(GeneralPurposeCustomPublisher.certFailOnStandardErrorPropertyName, "true");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Store cert with GeneralPurposeCustomPublisher did not fail on standard error.", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.revokeExternalCommandPropertyName, externalCommand + invalidOption);
	        props.setProperty(GeneralPurposeCustomPublisher.revokeFailOnErrorCodePropertyName, "false");
	        props.setProperty(GeneralPurposeCustomPublisher.revokeFailOnStandardErrorPropertyName, "true");
	        gpcPublisher.init(props);
			ret = gpcPublisher.storeCRL(admin, testcrl, null, 1);
		} catch (PublisherException e) {
		}
	    assertFalse("Revoke cert with GeneralPurposeCustomPublisher did not fail on standard error.", ret);
	    log.debug("<test12GenPurpCustPublStandardError()");
	} // test12GenPurpCustPublStandardError

	/**
	 * Test that the GeneralPurposeCustomPublisher fails when the external executable file does not exist.
	 *  
	 * @throws Exception
	 */
	public void test13GenPurpCustPublConnection() throws Exception {
	    log.debug(">test13GenPurpCustPublConnection()");
	    GeneralPurposeCustomPublisher gpcPublisher = null;
	    Properties props = new Properties();
	    // Create
    	gpcPublisher = new GeneralPurposeCustomPublisher();
	    // Test connection separatly for all publishers with invalid filename 
	    boolean ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.crlExternalCommandPropertyName, "randomfilenamethatdoesnotexistandneverwill8998752");
	        gpcPublisher.init(props);
			gpcPublisher.testConnection(admin);
			ret = true;
		} catch (PublisherConnectionException e) {
		}
	    assertFalse("testConnection reported all ok, but commandfile does not exist!", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.certExternalCommandPropertyName, "randomfilenamethatdoesnotexistandneverwill8998752");
	        gpcPublisher.init(props);
			gpcPublisher.testConnection(admin);
			ret = true;
		} catch (PublisherConnectionException e) {
		}
	    assertFalse("testConnection reported all ok, but commandfile does not exist!", ret);
	    ret = false;
	    try {
	        props.setProperty(GeneralPurposeCustomPublisher.revokeExternalCommandPropertyName, "randomfilenamethatdoesnotexistandneverwill8998752");
	        gpcPublisher.init(props);
			gpcPublisher.testConnection(admin);
			ret = true;
		} catch (PublisherConnectionException e) {
		}
	    assertFalse("testConnection reported all ok, but commandfile does not exist!", ret);
	    log.debug("<test12GenPurpCustPublStandardError()");
	} // test13GenPurpCustPublConnection

	/**
	 * Tries to execute the argument and return true if no exception was thrown and the command returned 0.
	 * 
	 * @param externalCommandToTest The String to run.
	 * @return Returns false on error.
	 */
	private boolean isValidCommand(String externalCommandToTest) {
	    boolean ret = false;
		try {
			Process externalProcess = Runtime.getRuntime().exec( externalCommand );
			if ( externalProcess.waitFor() == 0 ) {
				ret = true;
			}
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}
		return ret;
	} // isValidCommand
} // TestPublisher
