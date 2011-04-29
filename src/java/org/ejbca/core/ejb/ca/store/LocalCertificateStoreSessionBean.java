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

package org.ejbca.core.ejb.ca.store;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.config.EjbcaConfiguration;
import org.ejbca.config.ProtectConfiguration;
import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.JNDINames;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocal;
import org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocalHome;
import org.ejbca.core.ejb.log.ILogSessionLocal;
import org.ejbca.core.ejb.log.ILogSessionLocalHome;
import org.ejbca.core.ejb.protect.TableProtectSessionLocal;
import org.ejbca.core.ejb.protect.TableProtectSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.authorization.AuthenticationFailedException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.certificateprofiles.CACertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfileExistsException;
import org.ejbca.core.model.ca.certificateprofiles.EndUserCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.HardTokenAuthCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.HardTokenAuthEncCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.HardTokenEncCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.HardTokenSignCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.OCSPSignerCertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.RootCACertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.ServerCertificateProfile;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.ca.store.CertReqHistory;
import org.ejbca.core.model.ca.store.CertificateInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.protect.TableVerifyResult;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.cvc.PublicKeyEC;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.JDBCUtil;
import org.ejbca.util.StringTools;
import org.ejbca.util.keystore.KeyTools;

/**
 * Stores certificate and CRL in the local database using Certificate and CRL Entity Beans.
 * Uses JNDI name for datasource as defined in env 'Datasource' in ejb-jar.xml.
 *
 * @ejb.bean display-name="CertificateStoreSB"
 * name="CertificateStoreSession"
 * jndi-name="CertificateStoreSession"
 * view-type="both"
 * type="Stateless"
 * transaction-type="Container"
 *
 * @ejb.transaction type="Supports"
 *
 * @weblogic.enable-call-by-reference True
 *
 * @ejb.env-entry description="JDBC datasource to be used"
 * name="DataSource"
 * type="java.lang.String"
 * value="${datasource.jndi-name-prefix}${datasource.jndi-name}"
 *
 * @ejb.ejb-external-ref description="The Certificate entity bean used to store and fetch certificates"
 * view-type="local"
 * ref-name="ejb/CertificateDataLocal"
 * type="Entity"
 * home="org.ejbca.core.ejb.ca.store.CertificateDataLocalHome"
 * business="org.ejbca.core.ejb.ca.store.CertificateDataLocal"
 * link="CertificateData"
 *
 * @ejb.ejb-external-ref description="The CertReqHistoryData Entity bean"
 * view-type="local"
 * ref-name="ejb/CertReqHistoryDataLocal"
 * type="Entity"
 * home="org.ejbca.core.ejb.ca.store.CertReqHistoryDataLocalHome"
 * business="org.ejbca.core.ejb.ca.store.CertReqHistoryDataLocal"
 * link="CertReqHistoryData"
 *
 * @ejb.ejb-external-ref description="The CertificateProfileData Entity bean"
 * view-type="local"
 * ref-name="ejb/CertificateProfileDataLocal"
 * type="Entity"
 * home="org.ejbca.core.ejb.ca.store.CertificateProfileDataLocalHome"
 * business="org.ejbca.core.ejb.ca.store.CertificateProfileDataLocal"
 * link="CertificateProfileData"
 * 
 * @ejb.ejb-external-ref description="The Log session bean"
 * view-type="local"
 * ref-name="ejb/LogSessionLocal"
 * type="Session"
 * home="org.ejbca.core.ejb.log.ILogSessionLocalHome"
 * business="org.ejbca.core.ejb.log.ILogSessionLocal"
 * link="LogSession"
 *
 * @ejb.ejb-external-ref description="The CAAdmin Session Bean"
 *   view-type="local"
 *   ref-name="ejb/CAAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal"
 *   link="CAAdminSession"
 *
 * @ejb.ejb-external-ref description="The Authorization session bean"
 * view-type="local"
 * ref-name="ejb/AuthorizationSessionLocal"
 * type="Session"
 * home="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome"
 * business="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal"
 * link="AuthorizationSession"
 *
 * @ejb.ejb-external-ref description="Publishers are configured to store certificates and CRLs in additional places from the main database.
 * Publishers runs as local beans"
 * view-type="local"
 * ref-name="ejb/PublisherSessionLocal"
 * type="Session"
 * home="org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocalHome"
 * business="org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocal"
 * link="PublisherSession"
 *
 * @ejb.ejb-external-ref
 *   description="The table protection session bean"
 *   view-type="local"
 *   ref-name="ejb/TableProtectSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.protect.TableProtectSessionLocalHome"
 *   business="org.ejbca.core.ejb.protect.TableProtectSessionLocal"
 *   link="TableProtectSession"
 *   
 * @ejb.home extends="javax.ejb.EJBHome"
 * local-extends="javax.ejb.EJBLocalHome"
 * local-class="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome"
 * remote-class="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome"
 *
 * @ejb.interface extends="javax.ejb.EJBObject"
 * local-extends="javax.ejb.EJBLocalObject"
 * local-class="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal"
 * remote-class="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote"
 * 
 * @jboss.method-attributes
 *   pattern = "get*"
 *   read-only = "true"
 *
 * @jboss.method-attributes
 *   pattern = "find*"
 *   read-only = "true"
 *   
 * @jboss.method-attributes
 *   pattern = "list*"
 *   read-only = "true"
 *   
 * @jboss.method-attributes
 *   pattern = "is*"
 *   read-only = "true"
 *   
 * @jboss.method-attributes
 *   pattern = "exists*"
 *   read-only = "true"
 * 
 * @version $Id$
 * 
 */
public class LocalCertificateStoreSessionBean extends BaseSessionBean {

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
    /**
     * The home interface of Certificate entity bean
     */
    private CertificateDataLocalHome certHome = null;

    /**
     * The home interface of Certificate Type entity bean
     */
    private CertificateProfileDataLocalHome certprofilehome = null;
    
    /**
     * The home interface of CertReqHistory entity bean
     */
    private CertReqHistoryDataLocalHome certReqHistoryHome = null;

    /**
     * The local interface of the log session bean
     */
    private ILogSessionLocal logsession = null;

    /**
     * The local interface of the authorization session bean
     */
    private IAuthorizationSessionLocal authorizationsession = null;

    /** The come interface of the protection session bean */
    private TableProtectSessionLocalHome protecthome = null;
    
    /** If protection of database entries are enabled of not, default not */
    private boolean protect = ProtectConfiguration.getCertProtectionEnabled();
    
    /** help variable used to control that profiles update (read from database) isn't performed to often. */
    private static volatile long lastProfileCacheUpdateTime = -1;
    /** Cache of mappings between profileId and profileName */
    private static volatile HashMap profileIdNameMapCache = null;
    /** Cache of mappings between profileName and profileId */
    private static volatile Map profileNameIdMapCache = null;
    /** Cache of end entity profiles, with Id as keys */
    private static volatile Map profileCache = null;

    /**
     * The local interface of the publisher session bean
     */
    private IPublisherSessionLocal publishersession = null;

    final private CertificateDataUtil.Adapter adapter;
    
    public LocalCertificateStoreSessionBean() {
        super();
        adapter = new MyAdapter();
    }
    
    /**
     * Default create for SessionBean without any creation Arguments.
     *
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {
        certHome = (CertificateDataLocalHome) getLocator().getLocalHome(CertificateDataLocalHome.COMP_NAME);
        certReqHistoryHome = (CertReqHistoryDataLocalHome) getLocator().getLocalHome(CertReqHistoryDataLocalHome.COMP_NAME);
        certprofilehome = (CertificateProfileDataLocalHome) getLocator().getLocalHome(CertificateProfileDataLocalHome.COMP_NAME);
        if (protect) {
        	protecthome = (TableProtectSessionLocalHome) getLocator().getLocalHome(TableProtectSessionLocalHome.COMP_NAME);
        }

    }

    /**
     * Gets connection to log session bean
     */
    protected ILogSessionLocal getLogSession() {
        if (logsession == null) {
            try {
                ILogSessionLocalHome home = (ILogSessionLocalHome) getLocator().getLocalHome(ILogSessionLocalHome.COMP_NAME);
                logsession = home.create();
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
        return logsession;
    } //getLogSession


    /**
     * Gets connection to authorization session bean
     *
     * @return Connection
     */
    private IAuthorizationSessionLocal getAuthorizationSession() {
        if (authorizationsession == null) {
            try {
                IAuthorizationSessionLocalHome home = (IAuthorizationSessionLocalHome) getLocator().getLocalHome(IAuthorizationSessionLocalHome.COMP_NAME);
                authorizationsession = home.create();
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
        return authorizationsession;
    } //getAuthorizationSession

    /**
     * Gets connection to publisher session bean
     *
     * @return Connection
     */
    private IPublisherSessionLocal getPublisherSession() {
        if (publishersession == null) {
            try {
                IPublisherSessionLocalHome home = (IPublisherSessionLocalHome) getLocator().getLocalHome(IPublisherSessionLocalHome.COMP_NAME);
                publishersession = home.create();
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
        return publishersession;
    } //getPublisherSession


    /**
     * Stores a certificate.
     *
     * @param incert   The certificate to be stored.
     * @param cafp     Fingerprint (hex) of the CAs certificate.
     * @param username username of end entity owning the certificate.
     * @param status   Status of the certificate (from CertificateData).
     * @param type     Type of certificate (CERTTYPE_ENDENTITY etc from CertificateDataBean).
     * @param certificateProfileId the certificate profile id this cert was issued under
     * @param tag a custom string tagging this certificate for some purpose
     * @return true if storage was successful.
     * @throws CreateException if the certificate can not be stored in the database
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public boolean storeCertificate(Admin admin, Certificate incert, String username, String cafp,
                                    int status, int type, int certificateProfileId, String tag, long updateTime) throws CreateException {
    	if (log.isTraceEnabled()) {
            log.trace(">storeCertificate(" + username + ", " + cafp + ", " + status + ", " + type + ")");
    	}
        // Strip dangerous chars
        username = StringTools.strip(username);

        // We need special handling here of CVC certificate with EC keys, because they lack EC parameters in all certs except the Root certificate (CVCA)
    	PublicKey pubk = incert.getPublicKey();
    	if ((pubk instanceof PublicKeyEC)) {
    		PublicKeyEC pkec = (PublicKeyEC) pubk;
    		// The public key of IS and DV certificate (CVC) do not have any parameters so we have to do some magic to get a complete EC public key
    		ECParameterSpec spec = pkec.getParams();
    		if (spec == null) {
    			// We need to enrich this public key with parameters
    			try {
    				if (cafp != null) {
    					String cafingerp = cafp;
    					CertificateDataLocal cacert = certHome.findByPrimaryKey(new CertificateDataPK(cafp));
    					String nextcafp = cacert.getCaFingerprint();
    					int bar = 0; // never go more than 5 rounds, who knows what strange things can exist in the CAFingerprint column, make sure we never get stuck here
    					while ((!StringUtils.equals(cafingerp, nextcafp)) && (bar++ < 5)) {
    						cacert = certHome.findByPrimaryKey(new CertificateDataPK(nextcafp));
    						cafingerp = nextcafp;
    						nextcafp = cacert.getCaFingerprint();
    					}
						// We found a root CA certificate, hopefully ?
						PublicKey pkwithparams = cacert.getCertificate().getPublicKey();
						pubk = KeyTools.getECPublicKeyWithParams(pubk, pkwithparams);
    				}
				} catch (FinderException e) {
					log.info("Can not find CA certificate with fingerprint: "+cafp);
				} catch (Exception e) {
					// This catches NoSuchAlgorithmException, NoSuchProviderException and InvalidKeySpecException and possibly something else (NPE?)
					// because we want to continue anyway
					if (log.isDebugEnabled()) {
						log.debug("Can not enrich EC public key with missing parameters: ", e);
					}
				}
    		}
    	} // finished with ECC key special handling
    	
        CertificateDataPK pk = new CertificateDataPK();
        pk.fingerprint = CertTools.getFingerprintAsString(incert);
    	// Create the certificate in one go with all parameters at once. This is important so the persistence layer only creates *one* single
    	// insert statement. If we do a home.create and the some setXX, it will create one insert and one update statement to the database.
        final CertificateDataLocal data1 = certHome.create(incert, pubk, username, cafp, status, type, certificateProfileId, tag, updateTime);
        String msg = intres.getLocalizedMessage("store.storecert");            	
        getLogSession().log(admin, incert, LogConstants.MODULE_CA, new java.util.Date(), username, incert, LogConstants.EVENT_INFO_STORECERTIFICATE, msg);
        if (protect) {
        	CertificateInfo entry = new CertificateInfo(pk.fingerprint, cafp, data1.getSerialNumber(), data1.getIssuerDN(), data1.getSubjectDN(), status, type, data1.getExpireDate(), data1.getRevocationDate(), data1.getRevocationReason(), username, tag, certificateProfileId, updateTime);
        	TableProtectSessionLocal protect = protecthome.create();
        	protect.protect(entry);
        }
        log.trace("<storeCertificate()");
        return true;
    } // storeCertificate

    /**
     * Lists fingerprint (primary key) of ALL certificates in the database.
     * NOTE: Caution should be taken with this method as execution may be very
     * heavy indeed if many certificates exist in the database (imagine what happens if
     * there are millinos of certificates in the DB!).
     * Should only be used for testing purposes.
     *
     * @param admin    Administrator performing the operation
     * @param issuerdn the dn of the certificates issuer.
     * @return Collection of fingerprints, i.e. Strings, reverse ordered by expireDate where last expireDate is first in array.
     * @ejb.interface-method
     */
    public Collection listAllCertificates(Admin admin, String issuerdn) {
    	trace(">listAllCertificates()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String dn = CertTools.stringToBCDNString(StringTools.strip(issuerdn));
        try {
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select fingerprint, expireDate from CertificateData where issuerDN=? ORDER BY expireDate DESC");
            ps.setString(1, dn);
            result = ps.executeQuery();
            ArrayList vect = new ArrayList();
            while (result.next()) {
                vect.add(result.getString(1));
            }
            trace("<listAllCertificates()");
            return vect;
        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, result);
        }
    } // listAllCertificates

    /**
     * Lists RevokedCertInfo of ALL revoked certificates (status = CertificateDataBean.CERT_REVOKED) in the database from a certain issuer. 
     * NOTE: Caution should be taken with this method as execution may be very heavy indeed if many certificates exist in the database (imagine what happens if there are millinos of certificates in the DB!). 
     * Should only be used for testing purposes.
     * @param admin Administrator performing the operation
     * @param issuerdn the dn of the certificates issuer.
     * @param lastbasecrldate a date (Date.getTime()) of last base CRL or -1 for a complete CRL
     * @return Collection of RevokedCertInfo, reverse ordered by expireDate where last expireDate is first in array.
     *
     * @ejb.interface-method
     */
    public Collection listRevokedCertInfo(Admin admin, String issuerdn, long lastbasecrldate) {
    	trace(">listRevokedCertInfo()");

    	Connection con = null;
    	PreparedStatement ps = null;
    	ResultSet result = null;
    	String dn = CertTools.stringToBCDNString(StringTools.strip(issuerdn));
    	try {
    		// TODO:
    		// This should only list a few thousand certificates at a time, in case there
    		// are really many revoked certificates after some time...
    		con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
    		String sql = "select fingerprint, serialNumber, expireDate, revocationDate, revocationReason from CertificateData where issuerDN=? and status=?";
    		// For delta CRLs we must select both revoked certificates, and certificates that are active because they have been un-revoked
    		String deltaCRLSql = "select fingerprint, serialNumber, expireDate, revocationDate, revocationReason from CertificateData where issuerDN=? and revocationDate>? and (status=? or (status=? and revocationReason=?))";
    		if (lastbasecrldate > 0) {
    			sql = deltaCRLSql;
    		}
    		if (log.isDebugEnabled()) {
        		log.debug("Executing SQL: "+sql);    			
    		}
    		ps = con.prepareStatement(sql);
    		ps.setString(1, dn);
    		if (lastbasecrldate > 0) {
    			ps.setLong(2, lastbasecrldate);
    			ps.setInt(3, SecConst.CERT_REVOKED);
    			ps.setInt(4, SecConst.CERT_ACTIVE);
    			ps.setInt(5, RevokedCertInfo.REVOKATION_REASON_REMOVEFROMCRL);
    		} else {
    			ps.setInt(2, SecConst.CERT_REVOKED);            	
    		}
    		result = ps.executeQuery();
    		trace("listRevokedCertInfo(): query done");
    		ArrayList vect = new ArrayList();
    		while (result.next()) {
    			String fp = result.getString(1);
    			BigInteger serNo = new BigInteger(result.getString(2));
    			long exptime = result.getLong(3);
    			Date expDate = null;
    			if (exptime > 0) {
    				expDate = new Date(exptime);
    			}
    			long revtime = result.getLong(4);
    			Date revDate = null;
    			if (revtime > 0) {
    				revDate = new Date(revtime);            	
    			}
    			int revReason = result.getInt(5);
    			RevokedCertInfo certinfo = new RevokedCertInfo(fp, serNo, revDate, revReason, expDate);
    			// Add to the result
    			vect.add(certinfo);
    		}
    		trace("<listRevokedCertInfo()");
    		return vect;
    	} catch (Exception e) {
    		throw new EJBException(e);
    	} finally {
    		JDBCUtil.close(con, ps, result);
    	}
    } // listRevokedCertInfo

    /**
     * Lists certificates for a given subject signed by the given issuer.
     *
     * @param admin     Administrator performing the operation
     * @param subjectDN the DN of the subject whos certificates will be retrieved.
     * @param issuerDN  the dn of the certificates issuer.
     * @return Collection of Certificates (java.security.cert.Certificate) in no specified order or an empty Collection.
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method
     */
    public Collection findCertificatesBySubjectAndIssuer(Admin admin, String subjectDN, String issuerDN) {
    	if (log.isTraceEnabled()) {
        	log.trace(">findCertificatesBySubjectAndIssuer(), dn='" + subjectDN + "' and issuer='" + issuerDN + "'");
    	}
        // First make a DN in our well-known format
        String dn = StringTools.strip(subjectDN);
        dn = CertTools.stringToBCDNString(dn);
        String issuerdn = StringTools.strip(issuerDN);
        issuerdn = CertTools.stringToBCDNString(issuerdn);
        debug("Looking for cert with (transformed)DN: " + dn);
        try {
            Collection coll = certHome.findBySubjectDNAndIssuerDN(dn, issuerdn);
            Collection ret = new ArrayList();
            if (coll != null) {
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    ret.add(((CertificateDataLocal) iter.next()).getCertificate());
                }
            }
        	if (log.isTraceEnabled()) {
                log.trace("<findCertificatesBySubjectAndIssuer(), dn='" + subjectDN + "' and issuer='" + issuerDN + "'");
        	}
            return ret;
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        }
    } //findCertificatesBySubjectAndIssuer
    private Set getSet(Collection coll) {
        final Set ret = new HashSet();
        if (coll != null) {
            Iterator iter = coll.iterator();
            while (iter.hasNext()) {
                ret.add(((CertificateDataLocal) iter.next()).getUsername());
            }
        }
        return ret;
    }
    /**
     * @param admin
     * @param issuerDN
     * @param subjectDN
     * @return set of users with certificates with specified subject DN issued by specified issuer.
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method
     */
    public Set findUsernamesByIssuerDNAndSubjectDN(Admin admin, String issuerDN, String subjectDN) {
        if (log.isTraceEnabled()) {
            log.trace(">findCertificatesBySubjectAndIssuer(), issuer='" + issuerDN + "'");
        }
        // First make a DN in our well-known format
        final String transformedIssuerDN = CertTools.stringToBCDNString(StringTools.strip(issuerDN));
        final String transformedSubjectDN = CertTools.stringToBCDNString(StringTools.strip(subjectDN));
        if ( log.isDebugEnabled() ) {
            log.debug("Looking for user with a certificate with issuer DN(transformed) '" + transformedIssuerDN + "' and subject DN(transformed) '"+transformedSubjectDN+"'.");
        }
        try {
            return getSet(this.certHome.findBySubjectDNAndIssuerDN(transformedSubjectDN, transformedIssuerDN));
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("<findCertificatesBySubjectAndIssuer(), issuer='" + issuerDN + "'");
            }
        }
    }
    /**
     * @param admin
     * @param issuerDN
     * @param subjectKeyId
     * @return set of users with certificates with specified key issued by specified issuer.
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method
     */
    public Set findUsernamesByIssuerDNAndSubjectKeyId(Admin admin, String issuerDN, byte subjectKeyId[]) {
        if (log.isTraceEnabled()) {
            log.trace(">findCertificatesBySubjectAndIssuer(), issuer='" + issuerDN + "'");
        }
        // First make a DN in our well-known format
        final String transformedIssuerDN = CertTools.stringToBCDNString(StringTools.strip(issuerDN));
        final String sSubjectKeyId = new String(Base64.encode(subjectKeyId, false));
        if ( log.isDebugEnabled() ) {
            log.debug("Looking for user with a certificate with issuer DN(transformed) '" + transformedIssuerDN + "' and SubjectKeyId '"+sSubjectKeyId+"'.");
        }
        try {
        	return getSet(this.certHome.findByIssuerDNAndSubjectKeyId(transformedIssuerDN, sSubjectKeyId));
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("<findCertificatesBySubjectAndIssuer(), issuer='" + issuerDN + "'");
            }
        }
    }
    /**
     * Lists certificates for a given subject.
     *
     * @param admin     Administrator performing the operation
     * @param subjectDN the DN of the subject whos certificates will be retrieved.
     * @return Collection of Certificates (java.security.cert.Certificate) in no specified order or an empty Collection.
     * @ejb.interface-method
     */
    public Collection findCertificatesBySubject(Admin admin, String subjectDN) {
    	if (log.isTraceEnabled()) {
        	log.trace(">findCertificatesBySubjectAndIssuer(), dn='" + subjectDN + "'");
    	}
        // First make a DN in our well-known format
        String dn = StringTools.strip(subjectDN);
        dn = CertTools.stringToBCDNString(dn);
        debug("Looking for cert with (transformed)DN: " + dn);
        try {
            Collection coll = certHome.findBySubjectDN(dn);
            Collection ret = new ArrayList();
            if (coll != null) {
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    ret.add(((CertificateDataLocal) iter.next()).getCertificate());
                }
            }
        	if (log.isTraceEnabled()) {
                log.trace("<findCertificatesBySubject(), dn='" + subjectDN + "'");
        	}
            return ret;
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        }
    } //findCertificatesBySubject

    /**
     * @ejb.interface-method
     */
    public Collection findCertificatesByExpireTime(Admin admin, Date expireTime) {
    	if (log.isTraceEnabled()) {
        	log.trace(">findCertificatesByExpireTime(), time=" + expireTime);
    	}
        // First make expiretime in well know format
        debug("Looking for certs that expire before: " + expireTime);

        try {
            Collection coll = certHome.findByExpireDate(expireTime.getTime());
            Collection ret = new ArrayList();
            if (coll != null) {
            	if (log.isDebugEnabled()) {
                	log.debug("Found "+coll.size()+" certificates that expire before "+expireTime);            		
            	}
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    ret.add(((CertificateDataLocal) iter.next()).getCertificate());
                }
            }
        	if (log.isTraceEnabled()) {
                log.trace("<findCertificatesByExpireTime(), time=" + expireTime);
        	}
            return ret;
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        }
    }

    //findCertificatesByExpireTime

    /**
     * Finds usernames of users having certificate(s) expiring within a specified time and that has
     * status "active" or "notifiedaboutexpiration".
     * @see org.ejbca.core.model.SecConst#CERT_ACTIVE
     * @see org.ejbca.core.model.SecConst#CERT_NOTIFIEDABOUTEXPIRATION
     *
     * @ejb.interface-method
     */
    public Collection findCertificatesByExpireTimeWithLimit(Admin admin, Date expiretime) {
    	if (log.isTraceEnabled()) {
        	trace(">findCertificatesByExpireTimeWithLimit: "+expiretime);    		
    	}

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        ArrayList returnval = new ArrayList();
        long currentdate = new Date().getTime();

        try {
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("SELECT DISTINCT username FROM CertificateData WHERE expireDate>=? AND expireDate<? AND (status=? OR status=?)");
            ps.setLong(1, currentdate);
            ps.setLong(2, expiretime.getTime());
            ps.setInt(3, SecConst.CERT_ACTIVE);
            ps.setInt(4, SecConst.CERT_NOTIFIEDABOUTEXPIRATION);
            result = ps.executeQuery();
            while (result.next() && returnval.size() <= SecConst.MAXIMUM_QUERY_ROWCOUNT + 1) {
                if (result.getString(1) != null && !result.getString(1).equals("")) {
                    returnval.add(result.getString(1));
                }
            }
        	if (log.isTraceEnabled()) {
        		trace("<findCertificatesByExpireTimeWithLimit()");
        	}
            return returnval;
        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, result);
        }
    } //findCertificatesByExpireTimeWithLimit

    /**
     * Finds a certificate specified by issuer DN and serial number.
     *
     * @param admin    Administrator performing the operation
     * @param issuerDN issuer DN of the desired certificate.
     * @param serno    serial number of the desired certificate!
     * @return Certificate if found or null
     * @ejb.interface-method
     */
    public Certificate findCertificateByIssuerAndSerno(Admin admin, String issuerDN, BigInteger serno) {
    	return CertificateDataUtil.findCertificateByIssuerAndSerno(admin, issuerDN, serno, certHome, adapter);
    } //findCertificateByIssuerAndSerno

    /**
     * Implements ICertificateStoreSession::findCertificatesByIssuerAndSernos.
     * <p/>
     * The method retrives all certificates from a specific issuer
     * which are identified by list of serial numbers. The collection
     * will be empty if the issuerDN is <tt>null</tt>/empty
     * or the collection of serial numbers is empty.
     *
     * @param admin
     * @param issuerDN the subjectDN of a CA certificate
     * @param sernos a collection of certificate serialnumbers
     * @return Collection a list of certificates; never <tt>null</tt>
     * @ejb.interface-method
     */
    public Collection findCertificatesByIssuerAndSernos(Admin admin, String issuerDN, Collection sernos) {
    	trace(">findCertificateByIssuerAndSernos()");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        ArrayList vect = null;

        if (null == admin) {
            throw new IllegalArgumentException();
        }


        if (null == issuerDN || issuerDN.length() <= 0
                || null == sernos || sernos.isEmpty()) {
            return new ArrayList();
        }

        String dn = CertTools.stringToBCDNString(issuerDN);
        debug("Looking for cert with (transformed)DN: " + dn);

        try {

            final StringBuffer sb = new StringBuffer();
            {
                Iterator iter = sernos.iterator();
                while (iter.hasNext()) {
                    sb.append(", '");
                    // Make sure this is really a BigInteger passed in as (untrusted param)
                    BigInteger serno = (BigInteger) iter.next();
                    sb.append(serno.toString());
                    sb.append("'");
                }
            }
            /*
             * to save the repeating if-statement in the above
             * Closure not to add ', ' as the first characters
             * in the StringBuffer we remove the two chars here :)
             */
            sb.delete(0, ", ".length());
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("SELECT DISTINCT fingerprint"
                    + " FROM CertificateData WHERE"
                    + " issuerDN = ?"
                    + " AND serialNumber IN (" + sb.toString() + ")");
            ps.setString(1, dn);
            result = ps.executeQuery();

            vect = new ArrayList();
            while (result.next()) {
                Certificate cert = findCertificateByFingerprint(admin, result.getString(1));
                if (cert != null) {
                    vect.add(cert);
                }
            }

            trace("<findCertificateByIssuerAndSernos()");
            return vect;
        } catch (Exception fe) {
            throw new EJBException(fe);
        } finally {
            JDBCUtil.close(con, ps, result);
        }
    } // findCertificateByIssuerAndSernos

    /**
     * Finds certificate(s) for a given serialnumber.
     *
     * @param admin Administrator performing the operation
     * @param serno the serialnumber of the certificate(s) that will be retrieved
     * @return Certificate or null if none found.
     * @ejb.interface-method
     */
    public Collection findCertificatesBySerno(Admin admin, BigInteger serno) {
    	if (log.isTraceEnabled()) {
        	log.trace(">findCertificatesBySerno(),  serno=" + serno);
    	}
        try {
            Collection coll = certHome.findBySerialNumber(serno.toString());
            ArrayList ret = new ArrayList();

            if (coll != null) {
                Iterator iter = coll.iterator();

                while (iter.hasNext()) {
                    ret.add(((CertificateDataLocal) iter.next()).getCertificate());
                }
            }

        	if (log.isTraceEnabled()) {
                log.trace("<findCertificatesBySerno(), serno=" + serno);
        	}

            return ret;
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        }
    } // findCertificateBySerno

    /**
     * Finds username for a given certificate serial number.
     *
     * @param admin Administrator performing the operation
     * @param serno the serialnumber of the certificate to find username for.
     * @return username or null if none found.
     * @ejb.interface-method
     */
    public String findUsernameByCertSerno(Admin admin, BigInteger serno, String issuerdn) {
    	if (log.isTraceEnabled()) {
    		log.trace(">findUsernameByCertSerno(), serno: " + serno.toString(16) + ", issuerdn: " + issuerdn);    		
    	}
        String dn = CertTools.stringToBCDNString(issuerdn);
        try {
            Collection coll = certHome.findByIssuerDNSerialNumber(dn, serno.toString());
            String ret = null;

            if (coll != null) {
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    ret = ((CertificateDataLocal) iter.next()).getUsername();
                }
            }
        	if (log.isTraceEnabled()) {
                log.trace("<findUsernameByCertSerno(), ret=" + ret);
        	}
            return ret;
        } catch (javax.ejb.FinderException fe) {
            throw new EJBException(fe);
        }
    } // findUsernameByCertSerno

    /**
     * Finds certificate(s) for a given username.
     *
     * @param admin Administrator performing the operation
     * @param username the username of the certificate(s) that will be retrieved
     * @return Collection of Certificates ordered by expire date, with last expire date first, or null if none found.
     * @ejb.interface-method
     */
    public Collection findCertificatesByUsername(Admin admin, String username) {
    	return CertificateDataUtil.findCertificatesByUsername(admin, username, certHome, adapter);
    }

    /**
     * Finds certificate(s) for a given username and status.
     *
     * @param admin Administrator performing the operation
     * @param username the username of the certificate(s) that will be retrieved
     * @param status the status of the CertificateDataBean.CERT_ constants
     * @return Collection of Certificates ordered by expire date, with last expire date first, or empty list if user can not be found
     * @ejb.interface-method
     */
    public Collection findCertificatesByUsernameAndStatus(Admin admin, String username, int status) {
    	if (log.isTraceEnabled()) {
        	log.trace(">findCertificatesByUsername(),  username=" + username);
    	}
        ArrayList ret = new ArrayList();
        try {
            // Strip dangerous chars
            username = StringTools.strip(username);
            // This method on the entity bean does the ordering in the database
            Collection coll = certHome.findByUsernameAndStatus(username, status);
            if (coll != null) {
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    ret.add(((CertificateDataLocal) iter.next()).getCertificate());
                }
            }
        } catch (javax.ejb.FinderException e) {	// Ignore
        }
    	if (log.isTraceEnabled()) {
            log.trace("<findCertificatesByUsername(), username=" + username);
    	}
        return ret;
    } // findCertificatesByUsernameAndStatus

    /** Gets certificate info, which is basically all fields except the certificate itself. 
     * Note: this methid should not be used within a transaction where the reading of this info might depend on something stored earlier in the transaction. 
     * This is because this method uses direct SQL.
     * 
     * @return CertificateInfo or null if certificate does not exist.
     * @ejb.interface-method
     */
    public CertificateInfo getCertificateInfo(Admin admin, String fingerprint) {
    	trace(">getCertificateInfo()");
        CertificateInfo ret = null;

        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
    	try {
            // Now go on with the real select
	    	con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
	    	StringBuilder sql = new StringBuilder();
	    	sql.append("select issuerDN,subjectDN,cAFingerprint,status,type,serialNumber,expireDate,revocationDate,revocationReason,username,tag,certificateProfileId,updateTime from CertificateData where fingerprint=?");
	    	if (log.isDebugEnabled()) {
	    		log.debug("Executing SQL: "+sql.toString());    			
			}
	    	ps = con.prepareStatement(sql.toString());
	    	ps.setString(1, fingerprint);
			
			rs = ps.executeQuery();
			// This query can only return one row (fingerprint is the primary key)
			if (rs.next()) {
				String cafp = rs.getString(3);
				String serno = rs.getString(6);
				String issuerDN = rs.getString(1);
				String subjectDN = rs.getString(2);
				int status = rs.getInt(4);
				int type = rs.getInt(5);
				long expireDate = rs.getLong(7);
				long revocationDate = rs.getLong(8);
				int revocationReason = rs.getInt(9);
				String username = rs.getString(10);
				String tag = rs.getString(11);
				int cProfId = rs.getInt(12);
				long updateTime = rs.getLong(13);
	            ret = new CertificateInfo(fingerprint, cafp, serno, issuerDN, subjectDN, status, 
	            		type, expireDate, revocationDate, revocationReason, username, tag, cProfId, updateTime);				
			}
        } catch (SQLException e) {
        	String msg = intres.getLocalizedMessage("store.errorcertinfo", fingerprint);            	
            log.error(msg);
            throw new EJBException(e);
        } finally {
        	JDBCUtil.close(con, ps, rs);
        }
        trace("<getCertificateInfo()");
        return ret;
    } // getCertificateInfo

    /** Finds a certificate based on fingerprint. 
     * You can get fingerprint by for example "String fingerprint = CertTools.getFingerprintAsString(certificate);"
     * @return Certificate or null if it can not be found.
     * @ejb.interface-method
     */
    public Certificate findCertificateByFingerprint(Admin admin, String fingerprint) {
        return CertificateDataUtil.findCertificateByFingerprint(admin, fingerprint, certHome, adapter);
    } // findCertificateByFingerprint

    /**
     * Lists all active (status = 20) certificates of a specific type and if
     * given from a specific issuer.
     * <p/>
     * The type is the bitwise OR value of the types listed
     * int {@link org.ejbca.core.ejb.ca.store.CertificateDataBean}:<br>
     * <ul>
     * <li><tt>CERTTYPE_ENDENTITY</tt><br>
     * An user or machine certificate, which identifies a subject.
     * </li>
     * <li><tt>CERTTYPE_CA</tt><br>
     * A CA certificate which is <b>not</b> a root CA.
     * </li>
     * <li><tt>CERTTYPE_ROOTCA</tt><br>
     * A Root CA certificate.
     * </li>
     * </ul>
     * <p/>
     * Usage examples:<br>
     * <ol>
     * <li>Get all root CA certificates
     * <p/>
     * <code>
     * ...
     * ICertificateStoreSessionRemote itf = ...
     * Collection certs = itf.findCertificatesByType(adm,
     * CertificateDataBean.CERTTYPE_ROOTCA,
     * null);
     * ...
     * </code>
     * </li>
     * <li>Get all subordinate CA certificates for a specific
     * Root CA. It is assumed that the <tt>subjectDN</tt> of the
     * Root CA certificate is located in the variable <tt>issuer</tt>.
     * <p/>
     * <code>
     * ...
     * ICertificateStoreSessionRemote itf = ...
     * Certficate rootCA = ...
     * String issuer = rootCA.getSubjectDN();
     * Collection certs = itf.findCertificatesByType(adm,
     * CertificateDataBean.CERTTYPE_SUBCA,
     * issuer);
     * ...
     * </code>
     * </li>
     * <li>Get <b>all</b> CA certificates.
     * <p/>
     * <code>
     * ...
     * ICertificateStoreSessionRemote itf = ...
     * Collection certs = itf.findCertificatesByType(adm,
     * CertificateDataBean.CERTTYPE_SUBCA
     * + CERTTYPE_ROOTCA,
     * null);
     * ...
     * </code>
     * </li>
     * </ol>
     *
     * @param admin
     * @param issuerDN get all certificates issued by a specific issuer.
     *                 If <tt>null</tt> or empty return certificates regardless of
     *                 the issuer.
     * @param type     CERTTYPE_* types from CertificateDataBean
     * @return Collection Collection of Certificate, never <tt>null</tt>
     * @ejb.interface-method
     */
    public Collection findCertificatesByType(Admin admin, int type, String issuerDN) {
        return CertificateDataUtil.findCertificatesByType(admin, type, issuerDN, certHome, adapter);
    } // findCertificatesByType

    /** Method that sets status CertificateDataBean.CERT_ARCHIVED on the certificate data, only used for testing.
     * Can only be performed by an Admin.TYPE_INTERNALUSER. 
     * Normally ARCHIVED is set by the CRL creation job, after a certificate has expired and been added to a CRL 
     * (expired certificates that are revoked must be present on at least one CRL).
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void setArchivedStatus(Admin admin, String fingerprint) throws AuthorizationDeniedException {
    	if (admin.getAdminType() != Admin.TYPE_INTERNALUSER) {
    		throw new AuthorizationDeniedException("Unauthorized");
    	}
    	try {
    		CertificateDataPK revpk = new CertificateDataPK();
    		revpk.fingerprint = fingerprint;
    		CertificateDataLocal rev = certHome.findByPrimaryKey(revpk); 
    		rev.setStatus(SecConst.CERT_ARCHIVED);
    		if (log.isDebugEnabled()) {
    			log.debug("Set status ARCHIVED for certificate with fp: "+fingerprint+", revocation reason is: "+rev.getRevocationReason());
    		}
    	} catch (FinderException e) {
    		String msg = intres.getLocalizedMessage("store.errorcertinfo", fingerprint);            	
    		getLogSession().log(admin, 0, LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_UNKNOWN, msg);
    		throw new EJBException(e);
    	}
    }
    
    
    /**
     * Set the status of certificate with given serno to revoked, or unrevoked (re-activation).
     *
     * Re-activating (unrevoking) a certificate have two limitations.
     * 1. A password (for for example AD) will not be restored if deleted, only the certificate and certificate status and associated info will be restored
     * 2. ExtendedInformation, if used by a publisher will not be used when re-activating a certificate 
     *
     * The method leaves up to the caller to find the correct publishers and userDataDN.
     * 
     * @param admin      Administrator performing the operation
     * @param issuerdn   Issuer of certificate to be removed.
     * @param serno      the serno of certificate to revoke.
     * @param publishers and array of publiserids (Integer) of publishers to revoke the certificate in.
     * @param reason     the reason of the revokation. (One of the RevokedCertInfo.REVOKATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate, the object could be taken from user data instead.
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void setRevokeStatus(Admin admin, String issuerdn, BigInteger serno, Collection publishers, int reason, String userDataDN) {
    	setRevokeStatus (admin, issuerdn, serno, new Date (), publishers, reason, userDataDN);
    }
    /**
     * Set the status of certificate with given serno to revoked, or unrevoked (re-activation).
     *
     * Re-activating (unrevoking) a certificate have two limitations.
     * 1. A password (for for example AD) will not be restored if deleted, only the certificate and certificate status and associated info will be restored
     * 2. ExtendedInformation, if used by a publisher will not be used when re-activating a certificate 
     *
     * The method leaves up to the caller to find the correct publishers and userDataDN.
     * 
     * @param admin      Administrator performing the operation
     * @param issuerdn   Issuer of certificate to be removed.
     * @param serno      the serno of certificate to revoke.
     * @param revokedate when it was revoked
     * @param publishers and array of publiserids (Integer) of publishers to revoke the certificate in.
     * @param reason     the reason of the revokation. (One of the RevokedCertInfo.REVOKATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate, the object could be taken from user data instead.
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void setRevokeStatus(Admin admin, String issuerdn, BigInteger serno, Date revokedate, Collection publishers, int reason, String userDataDN) {
    	if (log.isTraceEnabled()) {
        	log.trace(">setRevokeStatus(),  issuerdn=" + issuerdn + ", serno=" + serno.toString(16)+", reason="+reason);
    	}
        Certificate certificate = null;
        try {
            certificate = (Certificate) this.findCertificateByIssuerAndSerno(admin, issuerdn, serno);
	        setRevokeStatus(admin, certificate, publishers, revokedate, reason, userDataDN);
        } catch (FinderException e) {
        	String msg = intres.getLocalizedMessage("store.errorfindcertserno", serno.toString(16));            	
            getLogSession().log(admin, issuerdn.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_REVOKEDCERT, msg);
            throw new EJBException(e);
        }
    	if (log.isTraceEnabled()) {
            log.trace("<setRevokeStatus(),  issuerdn=" + issuerdn + ", serno=" + serno.toString(16)+", reason="+reason);
    	}
    } // setRevokeStatus

    /**
     * Helper method to set the status of certificate to revoked or active. Re-activating (unrevoking) a certificate have two limitations.
     * 1. A password (for for example AD) will not be restored if deleted, only the certificate and certificate status and associated info will be restored
     * 2. ExtendedInformation, if used by a publisher will not be used when re-activating a certificate 
     *
     * The method leaves up to the caller to find the correct publishers and userDataDN.
     * 
     * @param admin      Administrator performing the operation
     * @param certificate the certificate to revoke or activate.
     * @param publishers and array of publiserids (Integer) of publishers to revoke/re-publish the certificate in.
     * @param revokedate when it was revoked.
     * @param reason     the reason of the revokation. (One of the RevokedCertInfo.REVOKATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate use object from user data instead.
     * @throws FinderException 
     */
    private void setRevokeStatus(Admin admin, Certificate certificate, Collection publishers, Date revokedate, int reason, String userDataDN) throws FinderException {
    	if (certificate == null) {
    		return;
    	}
    	if (log.isTraceEnabled()) {
        	log.trace(">private setRevokeStatus(Certificate),  issuerdn=" + CertTools.getIssuerDN(certificate) + ", serno=" + CertTools.getSerialNumberAsString(certificate));
    	}
    	CertificateDataPK revpk = new CertificateDataPK();
    	revpk.fingerprint = CertTools.getFingerprintAsString(certificate);
    	CertificateDataLocal rev = certHome.findByPrimaryKey(revpk); 
    	String username = rev.getUsername();
    	String cafp = rev.getCaFingerprint();
    	int type = rev.getType();
    	Date now = new Date();
    	String serialNo = CertTools.getSerialNumberAsString(certificate); // for logging
    	
    	// A normal revocation
    	if ( (rev.getStatus() != SecConst.CERT_REVOKED) 
    			&& (reason != RevokedCertInfo.NOT_REVOKED) && (reason != RevokedCertInfo.REVOKATION_REASON_REMOVEFROMCRL) ) {
    		rev.setStatus(SecConst.CERT_REVOKED);
    		rev.setRevocationDate(revokedate);
    		rev.setUpdateTime(now.getTime());
    		rev.setRevocationReason(reason);            	  
    		String msg = intres.getLocalizedMessage("store.revokedcert", new Integer(reason));            	
    		getLogSession().log(admin, certificate, LogConstants.MODULE_CA, new java.util.Date(), null, certificate, LogConstants.EVENT_INFO_REVOKEDCERT, msg);
    		// Revoke in all related publishers
    		getPublisherSession().revokeCertificate(admin, publishers, certificate, username, userDataDN, cafp, type, reason, revokedate.getTime(), rev.getTag(), rev.getCertificateProfileId(), now.getTime());
        // Unrevoke, can only be done when the certificate was previously revoked with reason CertificateHold
    	} else if ( ((reason == RevokedCertInfo.NOT_REVOKED) || (reason == RevokedCertInfo.REVOKATION_REASON_REMOVEFROMCRL)) 
    			&& (rev.getRevocationReason() == RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD) ) {
    		// Only allow unrevocation if the certificate is revoked and the revocation reason is CERTIFICATE_HOLD
    		int status = SecConst.CERT_ACTIVE;
    		rev.setStatus(status);
    		long revocationDate = -1L; // A null Date to setRevocationDate will result in -1 stored in long column
    		rev.setRevocationDate(null);
    		rev.setUpdateTime(now.getTime());
    		int revocationReason = RevokedCertInfo.NOT_REVOKED;
    		rev.setRevocationReason(revocationReason);
    		// Republish the certificate if possible
    		// If it is not possible, only log error but continue the operation of not revoking the certificate
    		try {
    			// Republishing will not restore a password, for example in AD, it will only re-activate the certificate.
    			String password = null;
    			boolean published = publishersession.storeCertificate(admin, publishers, certificate, username, password, userDataDN,
    					cafp, status, type, revocationDate, revocationReason, rev.getTag(), rev.getCertificateProfileId(), now.getTime(), null);
    			if ( !published ) {
    				throw new Exception("Unrevoked cert:" + serialNo + " reason: " + reason + " Could not be republished.");
    			}                	  
    			String msg = intres.getLocalizedMessage("store.republishunrevokedcert", new Integer(reason));            	
    			getLogSession().log(admin, CertTools.getIssuerDN(certificate).hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, certificate, LogConstants.EVENT_INFO_NOTIFICATION, msg);
    		} catch (Exception ex) {
    			// We catch the exception thrown above, to log the message, but it is only informational, so we dont re-throw anything
    			getLogSession().log(admin, CertTools.getIssuerDN(certificate).hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, certificate, LogConstants.EVENT_INFO_NOTIFICATION, ex.getMessage());
    		}
    	} else {
    		String msg = intres.getLocalizedMessage("store.ignorerevoke", serialNo, new Integer(rev.getStatus()), new Integer(reason));            	
    		getLogSession().log(admin, CertTools.getIssuerDN(certificate).hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, certificate, LogConstants.EVENT_INFO_NOTIFICATION, msg);
    	}
    	// Update database protection
    	if (protect) {
    		CertificateInfo entry = new CertificateInfo(rev.getFingerprint(), rev.getCaFingerprint(), rev.getSerialNumber(), rev.getIssuerDN(), rev.getSubjectDN(), rev.getStatus(), rev.getType(), rev.getExpireDate(), rev.getRevocationDate(), rev.getRevocationReason(), username, rev.getTag(), rev.getCertificateProfileId(), rev.getUpdateTime());
    		TableProtectSessionLocal protect;
    		try {
    			protect = protecthome.create();
    			protect.protect(entry);
    		} catch (CreateException e) {
    			String msg = intres.getLocalizedMessage("protect.errorcreatesession");            	
    			error(msg, e);
    		}
    	}
    	if (log.isTraceEnabled()) {
        	log.trace("<private setRevokeStatus(),  issuerdn=" + CertTools.getIssuerDN(certificate) + ", serno=" + CertTools.getSerialNumberAsString(certificate));
    	}
    } // setRevokeStatus

    /**
     * Revokes a certificate (already revoked by the CA), in the database. Also handles re-activation of suspended certificates.
     *
     * Re-activating (unrevoking) a certificate have two limitations.
     * 1. A password (for for example AD) will not be restored if deleted, only the certificate and certificate status and associated info will be restored
     * 2. ExtendedInformation, if used by a publisher will not be used when re-activating a certificate 
     * 
     * The method leaves up to the caller to find the correct publishers and userDataDN.
     *
     * @param admin      Administrator performing the operation
     * @param cert       The DER coded Certificate that has been revoked.
     * @param publishers and array of publiserids (Integer) of publishers to revoke the certificate in.
     * @param reason     the reason of the revokation. (One of the RevokedCertInfo.REVOKATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate use object from user data instead.
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void revokeCertificate(Admin admin, Certificate cert, Collection publishers, int reason, String userDataDN) {
        if (cert instanceof X509Certificate) {
            setRevokeStatus(admin, CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert), publishers, reason, userDataDN);
        }
    } //revokeCertificate

    /**
     * Method revoking all certificates generated by the specified issuerdn. Sets revokedate to current time.
     * Should only be called by CAAdminBean when a CA is about to be revoked.
     * 
     * TODO: Does not publish revocations to publishers!!!
     *
     * @param admin    the administrator performing the event.
     * @param issuerdn the dn of CA about to be revoked
     * @param reason   the reason of revokation.
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void revokeAllCertByCA(Admin admin, String issuerdn, int reason) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        int temprevoked = 0;
        int revoked = 0;

        String bcdn = CertTools.stringToBCDNString(issuerdn);

        final String firstsqlstatement = "UPDATE CertificateData SET status=?" +
                " WHERE issuerDN=? AND status = ? ";
        final String secondsqlstatement = "UPDATE CertificateData SET status=?, revocationDate=?, revocationReason=?" +
                " WHERE issuerDN=? AND status <> ?";

        long currentdate = new Date().getTime();

        try {
            // First SQL statement, changing all temporaty revoked certificates to permanently revoked certificates
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement(firstsqlstatement);
            ps.setInt(1, SecConst.CERT_REVOKED); // first statusfield
            ps.setString(2, bcdn); // issuerdn field
            ps.setInt(3, SecConst.CERT_TEMP_REVOKED); // second statusfield
            temprevoked = ps.executeUpdate();

            // Second SQL statement, revoking all non revoked certificates.
            ps2 = con.prepareStatement(secondsqlstatement);
            ps2.setInt(1, SecConst.CERT_REVOKED); // first statusfield
            ps2.setLong(2, currentdate); // revokedate field
            ps2.setInt(3, reason); // revokation reason
            ps2.setString(4, bcdn); // issuer dn
            ps2.setInt(5, SecConst.CERT_REVOKED); // second statusfield

            revoked = ps2.executeUpdate();

    		String msg = intres.getLocalizedMessage("store.revokedallbyca", issuerdn, new Integer(revoked + temprevoked), new Integer(reason));            	
            getLogSession().log(admin, bcdn.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_REVOKEDCERT, msg);
        } catch (Exception e) {
    		String msg = intres.getLocalizedMessage("store.errorrevokeallbyca", issuerdn);            	
            getLogSession().log(admin, bcdn.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_REVOKEDCERT, msg, e);
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, null);
            JDBCUtil.close(ps2);
        }
    } // revokeAllCertByCA

    /**
     * Method that checks if a users all certificates have been revoked.
     *
     * @param admin    Administrator performing the operation
     * @param username the username to check for.
     * @return returns true if all certificates are revoked.
     * @ejb.interface-method
     */
    public boolean checkIfAllRevoked(Admin admin, String username) {
        boolean returnval = true;
        Certificate certificate = null;
        // Strip dangerous chars
        username = StringTools.strip(username);
        Collection certs = findCertificatesByUsername(admin, username);
        // Revoke all certs
        if (!certs.isEmpty()) {
        	Iterator j = certs.iterator();
        	while (j.hasNext()) {
        		certificate = (Certificate) j.next();
        		String fingerprint = CertTools.getFingerprintAsString(certificate);
        		CertificateInfo info = getCertificateInfo(admin, fingerprint);
        		if (info != null) {
            		if (protect) {
            			TableProtectSessionLocal protect;
            			try {
            				protect = protecthome.create();
            				// The verify method will log failed verifies itself
            				TableVerifyResult res = protect.verify(info);
            				if (res.getResultCode() != TableVerifyResult.VERIFY_SUCCESS) {
            					//error("Verify failed, but we go on anyway.");
            				}
            			} catch (CreateException e) {
            				String msg = intres.getLocalizedMessage("protect.errorcreatesession");            	
            				error(msg, e);
            			}
            		}
            		if (info.getStatus() != SecConst.CERT_REVOKED) {
            			returnval = false;
            		}        			
        		}
        	}
        }
        return returnval;
    }

    /**
     * Checks if a certificate is revoked.
     *
     * @param issuerDN the DN of the issuer.
     * @param serno    the serialnumber of the certificate that will be checked
     * @return true if the certificate is revoked or can not be found in the database, false if it exists and is not revoked.
     * @ejb.interface-method
     */
    public boolean isRevoked(String issuerDN, BigInteger serno) {
        if (adapter.getLogger().isTraceEnabled()) {
            adapter.getLogger().trace(">isRevoked(), dn:" + issuerDN + ", serno=" + serno.toString(16));
        }
        // First make a DN in our well-known format
        String dn = CertTools.stringToBCDNString(issuerDN);

        boolean ret = false;
        try {
            Collection coll = certHome.findByIssuerDNSerialNumber(dn, serno.toString());
            if ( (coll != null) && (coll.size() > 0) ) {
                if (coll.size() > 1) {
                    String msg = intres.getLocalizedMessage("store.errorseveralissuerserno", issuerDN, serno.toString(16));             
                    //adapter.log(admin, issuerDN.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_DATABASE, msg);
                    adapter.error(msg);
                }
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    CertificateDataLocal data = (CertificateDataLocal) iter.next();
                    if (protecthome != null) {
                        CertificateDataUtil.verifyProtection(data, protecthome, adapter);
                    }
                    // if any of the certificates with this serno is revoked, return true
                    if (data.getStatus() == SecConst.CERT_REVOKED) {
                    	ret = true;
                    	break;
                    }
                }
            } else {
                // If there are no certificates with this serial number, return true (=revoked). Better safe than sorry!
            	ret = true;
            	if (adapter.getLogger().isTraceEnabled()) {
            		adapter.getLogger().trace("isRevoked() did not find certificate with dn "+dn+" and serno "+serno.toString(16));
            	}
            }
        } catch (Exception e) {
            throw new EJBException(e);
        }
        if (adapter.getLogger().isTraceEnabled()) {
            adapter.getLogger().trace("<isRevoked() returned " + ret);
        }
        return ret;
    } //isRevoked

    /**
     * Get status fast.
     * 
     * @param issuerDN
     * @param serno
     * @return the status of the certificate
     * @ejb.interface-method
     */
    public CertificateStatus getStatus(String issuerDN, BigInteger serno) {
        return CertificateDataUtil.getStatus(issuerDN, serno, certHome, protecthome, adapter);
    }

    /**
     * Method that authenticates a certificate by checking validity and lookup if certificate is revoked.
     *
     * @param certificate the certificate to be authenticated.
     * @param requireAdminCertificateInDatabase if true the certificate has to exist in the database
     * @throws AuthenticationFailedException if authentication failed.
     * @ejb.interface-method
     */
    public void authenticate(X509Certificate certificate, boolean requireAdminCertificateInDatabase) throws AuthenticationFailedException {
        // Check Validity
        try {
            certificate.checkValidity();
        } catch (Exception e) {
        	String msg = intres.getLocalizedMessage("authentication.certexpired", CertTools.getNotAfter(certificate).toString());            	
            throw new AuthenticationFailedException(msg);
        }
        if (requireAdminCertificateInDatabase) {
            // TODO: Verify Signature on cert? Not really needed since it's one of ou certs in the database.
            // Check if certificate is revoked.
            boolean isRevoked = isRevoked(CertTools.getIssuerDN(certificate),CertTools.getSerialNumber(certificate));
            if (isRevoked) {
                // Certificate revoked or missing in the database
            	String msg = intres.getLocalizedMessage("authentication.revokedormissing");            	
                throw new AuthenticationFailedException(msg);
            }
        } else {
        	// TODO: We should check the certificate for CRL or OCSP tags and verify the certificate status
        }
    }

    /**
     * Checks the table protection information for a certificate data row
     *
     * @param admin    Administrator performing the operation
     * @param issuerDN the DN of the issuer.
     * @param serno    the serialnumber of the certificate that will be checked
     * @ejb.interface-method
     */
    public void verifyProtection(Admin admin, String issuerDN, BigInteger serno) {
        CertificateDataUtil.verifyProtection(admin, issuerDN, serno, certHome, protecthome, adapter);
    }

    /**
     * Method used to add a CertReqHistory to database
     * 
     * @param admin calling the methods
     * @param cert the certificate to store (Only X509Certificate used for now)
     * @param useradmindata the user information used when issuing the certificate.
     * @ejb.transaction type="Required"
     * @ejb.interface-method     
     */
    public void addCertReqHistoryData(Admin admin, Certificate cert, UserDataVO useradmindata){
    	if (log.isTraceEnabled()) {
        	log.trace(">addCertReqHistoryData(" + CertTools.getSerialNumberAsString(cert) + ", " + CertTools.getIssuerDN(cert) + ", " + useradmindata.getUsername() + ")");
    	}
        try {
            CertReqHistoryDataPK pk = new CertReqHistoryDataPK();
            pk.fingerprint = CertTools.getFingerprintAsString(cert);
            certReqHistoryHome.create(cert,useradmindata);
        	String msg = intres.getLocalizedMessage("store.storehistory", useradmindata.getUsername());            	
            getLogSession().log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), useradmindata.getUsername(), cert, LogConstants.EVENT_INFO_STORECERTIFICATE, msg);            
        } catch (Exception e) {
        	String msg = intres.getLocalizedMessage("store.errorstorehistory", useradmindata.getUsername());            	
            getLogSession().log(admin, cert, LogConstants.MODULE_CA, new java.util.Date(), useradmindata.getUsername(), cert, LogConstants.EVENT_ERROR_STORECERTIFICATE, msg);
            throw new EJBException(e);
        }
    	if (log.isTraceEnabled()) {
    		log.trace("<addCertReqHistoryData()");
        }
    }
    
    /**
     * Method to remove CertReqHistory data.
     * @param admin
     * @param certFingerprint the primary key.
     * @ejb.transaction type="Required"    
     * @ejb.interface-method  
     */
    public void removeCertReqHistoryData(Admin admin, String certFingerprint){
    	if (log.isTraceEnabled()) {
        	log.trace(">removeCertReqHistData(" + certFingerprint + ")");
    	}
        try {          
            CertReqHistoryDataPK pk = new CertReqHistoryDataPK();
            pk.fingerprint = certFingerprint;
        	String msg = intres.getLocalizedMessage("store.removehistory", certFingerprint);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_STORECERTIFICATE, msg);
            this.certReqHistoryHome.remove(pk);
        } catch (Exception e) {
        	String msg = intres.getLocalizedMessage("store.errorremovehistory", certFingerprint);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_STORECERTIFICATE, msg);
            throw new EJBException(e);
        }
        trace("<removeCertReqHistData()");       	
    }
    
    /**
     * Retrieves the certificate request data belonging to given certificate serialnumber and issuerdn
     * 
	 * NOTE: This method will try to repair broken XML and will in that case
	 * update the database. This means that this method must always run in a
	 * transaction! 
	 * 
     * @param admin
     * @param certificateSN serial number of the certificate
     * @param issuerDN
     * @return the CertReqHistory or null if no data is stored with the certificate.
     * @ejb.interface-method
     */
    public CertReqHistory getCertReqHistory(Admin admin, BigInteger certificateSN, String issuerDN){
    	CertReqHistory retval = null;
    	
    	try{
    	  Collection result = certReqHistoryHome.findByIssuerDNSerialNumber(issuerDN, certificateSN.toString());
    	  if(result.iterator().hasNext()) {
    	    retval = ((CertReqHistoryDataLocal) result.iterator().next()).getCertReqHistory();
    	  }
    	}catch(FinderException fe){
    		// Do nothing but return null
    	}
    	
    	return retval;
    }
    
    
    /**
     * Retrieves all cert request datas belonging to a user.
     * 
	 * NOTE: This method will try to repair broken XML and will in that case
	 * update the database. This means that this method must always run in a
	 * transaction! 
	 * 
     * @param admin
     * @param username
     * @return a collection of CertReqHistory
     * @ejb.interface-method
     */
    public List getCertReqHistory(Admin admin, String username){
    	ArrayList retval = new ArrayList();
    	
    	try{
    	  Collection result = certReqHistoryHome.findByUsername(username);
    	  Iterator iter = result.iterator();
    	  while(iter.hasNext()) {
    	    retval.add(((CertReqHistoryDataLocal) iter.next()).getCertReqHistory());
    	  }
    	}catch(FinderException fe){
    		// Do nothing but return null
    	}
    	
    	return retval;
    }
    
    /**
     * A method designed to be called at startuptime to (possibly) upgrade certificate profiles.
     * This method will read all Certificate Profiles and as a side-effect upgrade them if the version if changed for upgrade.
     * Can have a side-effect of upgrading a profile, therefore the Required transaction setting.
     * 
     * @param admin administrator calling the method
     * 
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void initializeAndUpgradeProfiles(Admin admin) {
    	try {
    		Collection result = certprofilehome.findAll();
    		Iterator iter = result.iterator();
    		while(iter.hasNext()) {
    			CertificateProfileDataLocal pdata = (CertificateProfileDataLocal)iter.next();
    			String name = pdata.getCertificateProfileName();
    			pdata.upgradeProfile();
    			float version = pdata.getCertificateProfile().getVersion();
    			log.debug("Loaded certificate profile: "+name+" with version "+version);
    		}
    	} catch (FinderException e) {
    		log.error("FinderException trying to load profiles: ", e);
    	}
    	flushProfileCache();
    }

    
    /**
     * Adds a certificate profile to the database.
     *
     * @param admin                  administrator performing the task
     * @param certificateprofilename readable name of new certificate profile
     * @param certificateprofile     the profile to be added
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void addCertificateProfile(Admin admin, String certificateprofilename,
                                      CertificateProfile certificateprofile) throws CertificateProfileExistsException {
        addCertificateProfile(admin, findFreeCertificateProfileId(), certificateprofilename, certificateprofile);
    } // addCertificateProfile

    /**
     * Adds a certificate profile to the database.
     *
     * @param admin                  administrator performing the task
     * @param certificateprofileid   internal ID of new certificate profile, use only if you know it's right.
     * @param certificateprofilename readable name of new certificate profile
     * @param certificateprofile     the profile to be added
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void addCertificateProfile(Admin admin, int certificateprofileid, String certificateprofilename,
                                      CertificateProfile certificateprofile) throws CertificateProfileExistsException {
        if (isCertificateProfileNameFixed(certificateprofilename)) {
        	String msg = intres.getLocalizedMessage("store.errorcertprofilefixed", certificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            throw new CertificateProfileExistsException(msg);
        }

        if (isFreeCertificateProfileId(certificateprofileid)) {
            try {
                certprofilehome.findByCertificateProfileName(certificateprofilename);
            	String msg = intres.getLocalizedMessage("store.errorcertprofileexists", certificateprofilename);            	
                throw new CertificateProfileExistsException(msg);
            } catch (FinderException e) {
                try {
                    certprofilehome.create(new Integer(certificateprofileid), certificateprofilename, certificateprofile);
                    flushProfileCache();
                	String msg = intres.getLocalizedMessage("store.addedcertprofile", certificateprofilename);            	
                    getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_CERTPROFILE, msg);
                } catch (Exception f) {
                	String msg = intres.getLocalizedMessage("store.errorcreatecertprofile", certificateprofilename);            	
                    getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
                }
            }
        }
    } // addCertificateProfile
    
    /**
     * Adds a certificateprofile  with the same content as the original certificateprofile,
     *
     * @param admin                          Administrator performing the operation
     * @param originalcertificateprofilename readable name of old certificate profile
     * @param newcertificateprofilename      readable name of new certificate profile
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void cloneCertificateProfile(Admin admin, String originalcertificateprofilename, String newcertificateprofilename, Collection authorizedCaIds) throws CertificateProfileExistsException {
        CertificateProfile certificateprofile = null;

        if (isCertificateProfileNameFixed(newcertificateprofilename)) {
        	String msg = intres.getLocalizedMessage("store.errorcertprofilefixed", newcertificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            throw new CertificateProfileExistsException(msg);
        }

        try {
            certificateprofile = (CertificateProfile) getCertificateProfile(admin, originalcertificateprofilename).clone();

            boolean issuperadministrator = false;
            try {
                issuperadministrator = getAuthorizationSession().isAuthorizedNoLog(admin, "/super_administrator");
            } catch (AuthorizationDeniedException ade) {
            }

            if (!issuperadministrator && certificateprofile.isApplicableToAnyCA()) {
                // Not superadministrator, do not use ANYCA;
                certificateprofile.setAvailableCAs(authorizedCaIds);
            }

            try {
                certprofilehome.findByCertificateProfileName(newcertificateprofilename);
            	String msg = intres.getLocalizedMessage("store.erroraddprofilewithtempl", newcertificateprofilename, originalcertificateprofilename);            	
                getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
                throw new CertificateProfileExistsException();
            } catch (FinderException e) {
                try {
                    certprofilehome.create(new Integer(findFreeCertificateProfileId()), newcertificateprofilename, certificateprofile);
                    flushProfileCache();
                	String msg = intres.getLocalizedMessage("store.addedprofilewithtempl", newcertificateprofilename, originalcertificateprofilename);            	
                    getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_CERTPROFILE, msg);
                } catch (CreateException f) {
                }
            }
        } catch (CloneNotSupportedException f) {
        }

    } // cloneCertificateProfile

    /**
     * Removes a certificateprofile from the database, does not throw any errors if the profile does not exist, but it does log a message.
     *
     * @param admin Administrator performing the operation
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void removeCertificateProfile(Admin admin, String certificateprofilename) {
        try {
            CertificateProfileDataLocal pdl = certprofilehome.findByCertificateProfileName(certificateprofilename);
            pdl.remove();
            flushProfileCache();
        	String msg = intres.getLocalizedMessage("store.removedprofile", certificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_CERTPROFILE, msg);
        } catch (Exception e) {
        	String msg = intres.getLocalizedMessage("store.errorremoveprofile", certificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
        }
    } // removeCertificateProfile

    /**
     * Renames a certificateprofile
     *
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void renameCertificateProfile(Admin admin, String oldcertificateprofilename, String newcertificateprofilename) throws CertificateProfileExistsException {
        if (isCertificateProfileNameFixed(newcertificateprofilename)) {
        	String msg = intres.getLocalizedMessage("store.errorcertprofilefixed", newcertificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            throw new CertificateProfileExistsException(msg);
        }
        if (isCertificateProfileNameFixed(oldcertificateprofilename)) {
        	String msg = intres.getLocalizedMessage("store.errorcertprofilefixed", oldcertificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            throw new CertificateProfileExistsException(msg);
        }

        try {
            certprofilehome.findByCertificateProfileName(newcertificateprofilename);
        	String msg = intres.getLocalizedMessage("store.errorcertprofileexists", newcertificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            throw new CertificateProfileExistsException();
        } catch (FinderException e) {
            try {
                CertificateProfileDataLocal pdl = certprofilehome.findByCertificateProfileName(oldcertificateprofilename);
                pdl.setCertificateProfileName(newcertificateprofilename);
                flushProfileCache();
            	String msg = intres.getLocalizedMessage("store.renamedprofile", oldcertificateprofilename, newcertificateprofilename);            	
                getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_CERTPROFILE, msg);
            } catch (FinderException f) {
            	String msg = intres.getLocalizedMessage("store.errorrenameprofile", oldcertificateprofilename, newcertificateprofilename);            	
                getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
            }
        }
    } // renameCertificateProfile

    /**
     * Updates certificateprofile data
     *
     * @param admin Administrator performing the operation
     * @ejb.transaction type="Required"
     * @ejb.interface-method
     */
    public void changeCertificateProfile(Admin admin, String certificateprofilename, CertificateProfile certificateprofile) {
    	internalChangeCertificateProfileNoFlushCache(admin, certificateprofilename, certificateprofile);
        flushProfileCache();
    }// changeCertificateProfile
    
    /** Do not use, use changeCertificateProfile instead.
     * Used internally for testing only. Updates a profile without flushing caches.
     * @ejb.interface-method
     */
    public void internalChangeCertificateProfileNoFlushCache(Admin admin, String certificateprofilename, CertificateProfile certificateprofile) {
        try {
            CertificateProfileDataLocal pdl = certprofilehome.findByCertificateProfileName(certificateprofilename);
            pdl.setCertificateProfile(certificateprofile);
        	String msg = intres.getLocalizedMessage("store.editedprofile", certificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_CERTPROFILE, msg);
        } catch (FinderException e) {
        	String msg = intres.getLocalizedMessage("store.erroreditprofile", certificateprofilename);            	
            getLogSession().log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_CERTPROFILE, msg);
        }
    }// internalChangeCertificateProfileNoFlushCache

    /**
     * Retrives a Collection of id:s (Integer) to authorized profiles.
     *
     * @param certprofiletype should be either CertificateDataBean.CERTTYPE_ENDENTITY, CertificateDataBean.CERTTYPE_SUBCA, CertificateDataBean.CERTTYPE_ROOTCA,
     *                        CertificateDataBean.CERTTYPE_HARDTOKEN (i.e EndEntity certificates and Hardtoken fixed profiles) or 0 for all.
     *                        Retrives certificate profile names sorted.
     * @param authorizedCaIds Collection<Integer> of authorized CA Ids for the specified Admin
     * @return Collection of id:s (Integer)
     * @ejb.interface-method
     */
    public Collection getAuthorizedCertificateProfileIds(Admin admin, int certprofiletype, Collection authorizedCaIds) {
        ArrayList returnval = new ArrayList();
        Collection result = null;

        HashSet authorizedcaids = new HashSet(authorizedCaIds);

        // Add fixed certificate profiles.
        if (certprofiletype == 0 || certprofiletype == SecConst.CERTTYPE_ENDENTITY || certprofiletype == SecConst.CERTTYPE_HARDTOKEN){
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_ENDUSER));
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_OCSPSIGNER));
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_SERVER));
        }
        if (certprofiletype == 0 || certprofiletype == SecConst.CERTTYPE_SUBCA) {
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_SUBCA));
        }
        if (certprofiletype == 0 || certprofiletype == SecConst.CERTTYPE_ROOTCA) {
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_ROOTCA));
        }
        if (certprofiletype == 0 || certprofiletype == SecConst.CERTTYPE_HARDTOKEN) {
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTH));
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTHENC));
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENENC));
            returnval.add(new Integer(SecConst.CERTPROFILE_FIXED_HARDTOKENSIGN));
        }

        try {
            result = certprofilehome.findAll();
            Iterator i = result.iterator();
            while (i.hasNext()) {
                CertificateProfileDataLocal next = (CertificateProfileDataLocal) i.next();
                CertificateProfile profile = next.getCertificateProfile();
                // Check if all profiles available CAs exists in authorizedcaids.
                if (certprofiletype == 0 || certprofiletype == profile.getType()
                        || (profile.getType() == SecConst.CERTTYPE_ENDENTITY &&
                        certprofiletype == SecConst.CERTTYPE_HARDTOKEN)) {
                    Iterator availablecas = profile.getAvailableCAs().iterator();
                    boolean allexists = true;
                    while (availablecas.hasNext()) {
                        Integer nextcaid = (Integer) availablecas.next();
                        if (nextcaid.intValue() == CertificateProfile.ANYCA) {
                            allexists = true;
                            break;
                        }

                        if (!authorizedcaids.contains(nextcaid)) {
                            allexists = false;
                            break;
                        }
                    }

                    if (allexists) {
                        returnval.add(next.getId());
                    }
                }
            }
        } catch (FinderException e) {
        }
        return returnval;
    } // getAuthorizedCertificateProfileNames


    /**
     * Method creating a hashmap mapping profile id (Integer) to profile name (String).
     *
     * @param admin Administrator performing the operation
     * @ejb.interface-method
     */
    public HashMap getCertificateProfileIdToNameMap(Admin admin) {
    	if (log.isTraceEnabled()) {
    		log.trace("><getCertificateProfileIdToNameMap");
    	}
    	return getCertificateProfileIdNameMapInternal();    	
    } // getCertificateProfileIdToNameMap

    /**
     * Clear and reload certificate profile caches.
     * @ejb.transaction type="Supports"
     * @ejb.interface-method
     */
    public void flushProfileCache() {
    	if (log.isTraceEnabled()) {
    		log.trace(">flushProfileCache");
    	}
        HashMap idNameCache = new HashMap();
        HashMap nameIdCache = new HashMap();
        HashMap profCache = new HashMap();
        
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_ENDUSER), EndUserCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_SUBCA), CACertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_ROOTCA), RootCACertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_OCSPSIGNER), OCSPSignerCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_SERVER), ServerCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTH), HardTokenAuthCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTHENC), HardTokenAuthEncCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENENC), HardTokenEncCertificateProfile.CERTIFICATEPROFILENAME);
        idNameCache.put(Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENSIGN), HardTokenSignCertificateProfile.CERTIFICATEPROFILENAME);

        nameIdCache.put(EndUserCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_ENDUSER));
        nameIdCache.put(CACertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_SUBCA));
        nameIdCache.put(RootCACertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_ROOTCA));
        nameIdCache.put(OCSPSignerCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_OCSPSIGNER));
        nameIdCache.put(ServerCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_SERVER));
        nameIdCache.put(HardTokenAuthCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTH));
        nameIdCache.put(HardTokenAuthEncCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENAUTHENC));
        nameIdCache.put(HardTokenEncCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENENC));
        nameIdCache.put(HardTokenSignCertificateProfile.CERTIFICATEPROFILENAME, Integer.valueOf(SecConst.CERTPROFILE_FIXED_HARDTOKENSIGN));

        try{
        	Collection result = certprofilehome.findAll();
        	if (log.isDebugEnabled()) {
                debug("Found "+result.size()+" certificate profiles.");        		
        	}
            Iterator i = result.iterator();
            while(i.hasNext()){
                CertificateProfileDataLocal next = (CertificateProfileDataLocal) i.next();
                idNameCache.put(next.getId(),next.getCertificateProfileName());
                nameIdCache.put(next.getCertificateProfileName(), next.getId());
                profCache.put(next.getId(), next.getCertificateProfile());
            }
        }catch(FinderException e) {
            log.error("Error reading certificate profiles: ", e);
        }
        profileIdNameMapCache = idNameCache;
        profileNameIdMapCache = nameIdCache;
        profileCache = profCache;
        lastProfileCacheUpdateTime = System.currentTimeMillis();
    	if (log.isDebugEnabled()) {
    		log.debug("Flushed profile cache.");
    	}
    	if (log.isTraceEnabled()) {
    		log.trace("<flushProfileCache");
    	}
    } // flushProfileCache

    private HashMap getCertificateProfileIdNameMapInternal() {
    	if ((profileIdNameMapCache == null) || (lastProfileCacheUpdateTime+EjbcaConfiguration.getCacheCertificateProfileTime() < System.currentTimeMillis())) {
    		flushProfileCache();
    	}
    	return profileIdNameMapCache;
      } // getEndEntityProfileIdNameMapInternal

    private Map getCertificateProfileNameIdMapInternal(){
    	if ((profileNameIdMapCache == null) || (lastProfileCacheUpdateTime+EjbcaConfiguration.getCacheCertificateProfileTime() < System.currentTimeMillis())) {
    		flushProfileCache();
    	}
    	return profileNameIdMapCache;
      } // getEndEntityProfileIdNameMapInternal

    private Map getProfileCacheInternal() {
    	if ((profileCache == null) || (lastProfileCacheUpdateTime+EjbcaConfiguration.getCacheCertificateProfileTime() < System.currentTimeMillis())) {
    		flushProfileCache();
    	}
    	return profileCache;
    }

    /**
     * Retrives a named certificate profile or null if none was found.
     *
     * @ejb.interface-method
     */
    public CertificateProfile getCertificateProfile(Admin admin, String certificateprofilename) {
		Integer id = (Integer)getCertificateProfileNameIdMapInternal().get(certificateprofilename);
		if (id != null) {
			return getCertificateProfile(admin, id);			
		} else {
			return null;
		}
    } //  getCertificateProfile

    /**
     * Finds a certificate profile by id.
     *
     * @param admin Administrator performing the operation
     * @return CertificateProfile (cloned) or null if it can not be found.
     * @ejb.interface-method
     */
    public CertificateProfile getCertificateProfile(Admin admin, int id) {
    	if (log.isTraceEnabled()) {
            log.trace(">getCertificateProfile("+id+")");    		
    	}
        CertificateProfile returnval = null;
        if (id < SecConst.FIXED_CERTIFICATEPROFILE_BOUNDRY) {
            switch (id) {
                case SecConst.CERTPROFILE_FIXED_ENDUSER:
                    returnval = new EndUserCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_SUBCA:
                    returnval = new CACertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_ROOTCA:
                    returnval = new RootCACertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_OCSPSIGNER:
                    returnval = new OCSPSignerCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_SERVER:
                    returnval = new ServerCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_HARDTOKENAUTH:
                    returnval = new HardTokenAuthCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_HARDTOKENAUTHENC:
                    returnval = new HardTokenAuthEncCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_HARDTOKENENC:
                    returnval = new HardTokenEncCertificateProfile();
                    break;
                case SecConst.CERTPROFILE_FIXED_HARDTOKENSIGN:
                    returnval = new HardTokenSignCertificateProfile();
                    break;
                default:
                    returnval = new EndUserCertificateProfile();
            }
        } else {
    		// We need to clone the profile, otherwise the cache contents will be modifyable from the outside
        	CertificateProfile cprofile = (CertificateProfile)getProfileCacheInternal().get(Integer.valueOf(id));
    		try {
    			if (cprofile != null) {
        			returnval = (CertificateProfile)cprofile.clone();    				
    			}
    		} catch (CloneNotSupportedException e) {
    			log.error("Should never happen: ", e);
    			throw new RuntimeException(e);
    		}
        }
    	if (log.isTraceEnabled()) {
            log.trace("<getCertificateProfile("+id+"): "+(returnval == null ? "null":"not null"));     
    	}
        return returnval;
    } // getCertificateProfile


    /**
     * Returns a certificate profile id, given it's certificate profile name
     *
     * @param admin Administrator performing the operation
     * @return the id or 0 if certificateprofile cannot be found.
     * @ejb.interface-method
     */
    public int getCertificateProfileId(Admin admin, String certificateprofilename) {
    	if (log.isTraceEnabled()) {
        	log.trace(">getCertificateProfileId: "+certificateprofilename);
    	}
        int returnval = 0;
    	Integer id = (Integer)getCertificateProfileNameIdMapInternal().get(certificateprofilename);
    	if (id != null) {
    		returnval = id.intValue();
    	}
    	if (log.isTraceEnabled()) {
        	log.trace("<getCertificateProfileId: "+certificateprofilename+"): "+returnval);
    	}
        return returnval;
    } // getCertificateProfileId

    /**
     * Returns a certificateprofiles name given it's id.
     *
     * @param admin Administrator performing the operation
     * @return certificateprofilename or null if certificateprofile id doesn't exists.
     * @ejb.interface-method
     */
    public String getCertificateProfileName(Admin admin, int id) {
    	if (log.isTraceEnabled()) {
        	log.trace(">getCertificateProfileName: "+id);
    	}
        String returnval = null;
    	returnval = (String)getCertificateProfileIdNameMapInternal().get(Integer.valueOf(id));
    	if (log.isTraceEnabled()) {
        	log.trace("<getCertificateProfileName: "+id+"): "+returnval);
    	}
        return returnval;
    } // getCertificateProfileName

    /**
     * Method to check if a CA exists in any of the certificate profiles. Used to avoid desyncronization of CA data.
     *
     * @param admin Administrator performing the operation
     * @param caid  the caid to search for.
     * @return true if ca exists in any of the certificate profiles.
     * @ejb.interface-method
     */
    public boolean existsCAInCertificateProfiles(Admin admin, int caid) {
        Iterator availablecas = null;
        boolean exists = false;
        try {
            Collection result = certprofilehome.findAll();
            Iterator i = result.iterator();
            while (i.hasNext() && !exists) {
            	CertificateProfileDataLocal cd = (CertificateProfileDataLocal) i.next();
            	CertificateProfile certProfile = cd.getCertificateProfile(); 
            	if(certProfile.getType() == CertificateProfile.TYPE_ENDENTITY){
            		availablecas = certProfile.getAvailableCAs().iterator();
            		while (availablecas.hasNext()) {
            			if (((Integer) availablecas.next()).intValue() == caid ) {
            				exists = true;
            				debug("CA exists in certificate profile "+cd.getCertificateProfileName());
            				break;
            			}
            		}
            	}
            }
        } catch (FinderException e) {
        }

        return exists;
    } // existsCAInCertificateProfiles

    /**
     * Method to check if a Publisher exists in any of the certificate profiles. Used to avoid desyncronization of publisher data.
     *
     * @param publisherid the publisherid to search for.
     * @return true if publisher exists in any of the certificate profiles.
     * @ejb.interface-method
     */
    public boolean existsPublisherInCertificateProfiles(Admin admin, int publisherid) {
        Iterator availablepublishers = null;
        boolean exists = false;
        try {
            Collection result = certprofilehome.findAll();
            Iterator i = result.iterator();
            while (i.hasNext() && !exists) {
                availablepublishers = ((CertificateProfileDataLocal) i.next()).getCertificateProfile().getPublisherList().iterator();
                while (availablepublishers.hasNext()) {
                    if (((Integer) availablepublishers.next()).intValue() == publisherid) {
                        exists = true;
                        break;
                    }
                }
            }
        } catch (FinderException e) {
        }

        return exists;
    } // existsPublisherInCertificateProfiles

    /**
     * @ejb.interface-method
     */
    public int findFreeCertificateProfileId() {
        Random random = new Random((new Date()).getTime());
        int id = random.nextInt();
        boolean foundfree = false;

        while (!foundfree) {
            try {
                if (id > SecConst.FIXED_CERTIFICATEPROFILE_BOUNDRY) {
                    certprofilehome.findByPrimaryKey(new Integer(id));
                } else {
                    id = random.nextInt();
                }
            } catch (FinderException e) {
                foundfree = true;
            }
        }
        return id;
    } // findFreeCertificateProfileId


    // Private methods

    private boolean isCertificateProfileNameFixed(String certificateprofilename) {
        boolean returnval = false;

        if (certificateprofilename.equals(EndUserCertificateProfile.CERTIFICATEPROFILENAME)) {
            return true;
        }
        if (certificateprofilename.equals(CACertificateProfile.CERTIFICATEPROFILENAME)) {
            return true;
        }
        if (certificateprofilename.equals(RootCACertificateProfile.CERTIFICATEPROFILENAME)) {
            return true;
        }
        if (certificateprofilename.equals(OCSPSignerCertificateProfile.CERTIFICATEPROFILENAME)) {
            return true;
        }
        if (certificateprofilename.equals(ServerCertificateProfile.CERTIFICATEPROFILENAME)) {
            return true;
        }
        return returnval;
    }

    private boolean isFreeCertificateProfileId(int id) {
        boolean foundfree = false;
        try {
            if (id > SecConst.FIXED_CERTIFICATEPROFILE_BOUNDRY) {
                certprofilehome.findByPrimaryKey(new Integer(id));
            }
        } catch (FinderException e) {
            foundfree = true;
        }
        return foundfree;
    } // isFreeCertificateProfileId

    private class MyAdapter implements CertificateDataUtil.Adapter {
        /* (non-Javadoc)
         * @see org.ejbca.core.ejb.ca.store.CertificateDataUtil.Adapter#getLogger()
         */
        public Logger getLogger() {
            return log;
        }
        /* (non-Javadoc)
         * @see org.ejbca.core.ejb.ca.store.CertificateDataUtil.Adapter#log(org.ejbca.core.model.log.Admin, int, int, java.util.Date, java.lang.String, java.security.cert.X509Certificate, int, java.lang.String)
         */
        public void log(Admin admin, int caid, int module, Date time, String username,
                        X509Certificate certificate, int event, String comment) {
            getLogSession().log(admin, caid, module, new java.util.Date(),
                                username, certificate, event, comment);
        }
        /* (non-Javadoc)
         * @see org.ejbca.core.ejb.ca.store.CertificateDataUtil.Adapter#debug(java.lang.String)
         */
        public void debug(String s) {
            LocalCertificateStoreSessionBean.this.debug(s);
        }
        /* (non-Javadoc)
         * @see org.ejbca.core.ejb.ca.store.CertificateDataUtil.Adapter#error(java.lang.String)
         */
        public void error(String s) {
            LocalCertificateStoreSessionBean.this.error(s);        	
        }
        /* (non-Javadoc)
         * @see org.ejbca.core.ejb.ca.store.CertificateDataUtil.Adapter#error(java.lang.String)
         */
        public void error(String s, Exception e) {
            LocalCertificateStoreSessionBean.this.error(s, e);        	
        }
    }
} // CertificateStoreSessionBean
