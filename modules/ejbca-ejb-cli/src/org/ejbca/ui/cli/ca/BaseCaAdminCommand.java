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

package org.ejbca.ui.cli.ca;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.ConvertingWrapDynaBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.WrapDynaBean;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.rules.AccessRuleNotFoundException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.roles.RoleExistsException;
import org.cesecore.roles.RoleNotFoundException;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.ejbca.core.model.SecConst;
import org.ejbca.ui.cli.BaseCommand;

/**
 * Base for CA commands, contains common functions for CA operations
 * 
 * @version $Id$
 */
public abstract class BaseCaAdminCommand extends BaseCommand {

    protected static final String MAINCOMMAND = "ca";

    protected static final String defaultSuperAdminCN = "SuperAdmin";

    /** Private key alias in PKCS12 keystores */
    protected String privKeyAlias = "privateKey";
    protected char[] privateKeyPass = null;

    /**
     * Retrieves the complete certificate chain from the CA
     * 
     * @param human readable name of CA
     * @return array of certificates, from ISignSession.getCertificateChain()
     */
    protected Collection<Certificate> getCertChain(AuthenticationToken authenticationToken, String caname) throws Exception {
        getLogger().trace(">getCertChain()");
        Collection<Certificate> returnval = new ArrayList<Certificate>();
        try {
            CAInfo cainfo = ejb.getCaSession().getCAInfo(authenticationToken, caname);
            if (cainfo != null) {
                returnval = cainfo.getCertificateChain();
            }
        } catch (Exception e) {
            getLogger().error("Error while getting certfificate chain from CA.", e);
        }
        getLogger().trace("<getCertChain()");
        return returnval;
    }

    protected void makeCertRequest(String dn, KeyPair rsaKeys, String reqfile) throws NoSuchAlgorithmException, IOException, NoSuchProviderException,
            InvalidKeyException, SignatureException {
        getLogger().trace(">makeCertRequest: dn='" + dn + "', reqfile='" + reqfile + "'.");

        PKCS10CertificationRequest req = new PKCS10CertificationRequest("SHA1WithRSA", CertTools.stringToBcX509Name(dn), rsaKeys.getPublic(), new DERSet(),
                rsaKeys.getPrivate());

        /*
         * We don't use these unnecessary attributes DERConstructedSequence kName
         * = new DERConstructedSequence(); DERConstructedSet kSeq = new
         * DERConstructedSet();
         * kName.addObject(PKCSObjectIdentifiers.pkcs_9_at_emailAddress);
         * kSeq.addObject(new DERIA5String("foo@bar.se"));
         * kName.addObject(kSeq); req.setAttributes(kName);
         */
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(req);
        dOut.close();

        PKCS10CertificationRequest req2 = new PKCS10CertificationRequest(bOut.toByteArray());
        boolean verify = req2.verify();
        getLogger().info("Verify returned " + verify);

        if (verify == false) {
            getLogger().info("Aborting!");
            return;
        }

        FileOutputStream os1 = new FileOutputStream(reqfile);
        os1.write("-----BEGIN CERTIFICATE REQUEST-----\n".getBytes());
        os1.write(Base64.encode(bOut.toByteArray()));
        os1.write("\n-----END CERTIFICATE REQUEST-----\n".getBytes());
        os1.close();
        getLogger().info("CertificationRequest '" + reqfile + "' generated successfully.");
        getLogger().trace("<makeCertRequest: dn='" + dn + "', reqfile='" + reqfile + "'.");
    }

    protected void createCRL(final String cliUsername, final String cliPassword, final String issuerdn, final boolean deltaCRL) {
        getLogger().trace(">createCRL()");
        try {
            if (issuerdn != null) {
                CAInfo cainfo = ejb.getCaSession().getCAInfo(getAdmin(cliUserName, cliPassword), issuerdn.hashCode());
                if (!deltaCRL) {
                    ejb.getCrlCreateSession().forceCRL(getAdmin(cliUserName, cliPassword), cainfo.getCAId());
                    int number = ejb.getCrlStoreSession().getLastCRLNumber(issuerdn, false);
                    getLogger().info("CRL with number " + number + " generated.");
                } else {
                    ejb.getCrlCreateSession().forceDeltaCRL(getAdmin(cliUserName, cliPassword), cainfo.getCAId());
                    int number = ejb.getCrlStoreSession().getLastCRLNumber(issuerdn, true);
                    getLogger().info("Delta CRL with number " + number + " generated.");
                }
            } else {
                int createdcrls = ejb.getCrlCreateSession().createCRLs(getAdmin(cliUserName, cliPassword));
                getLogger().info("  " + createdcrls + " CRLs have been created.");
                int createddeltacrls = ejb.getCrlCreateSession().createDeltaCRLs(getAdmin(cliUserName, cliPassword));
                getLogger().info("  " + createddeltacrls + " delta CRLs have been created.");
            }
        } catch (Exception e) {
            getLogger().error("Error while getting certficate chain from CA.", e);
        }
        getLogger().trace(">createCRL()");
    }

    protected String getIssuerDN(AuthenticationToken authenticationToken, String caname) throws Exception {
        CAInfo cainfo = ejb.getCaSession().getCAInfo(authenticationToken, caname);
        return cainfo != null ? cainfo.getSubjectDN() : null;
    }

    protected CAInfo getCAInfo(AuthenticationToken authenticationToken, String caname) throws Exception {
        CAInfo result;
        try {
            result = ejb.getCaSession().getCAInfo(authenticationToken, caname);
        } catch (Exception e) {
            getLogger().debug("Error retriving CA " + caname + " info.", e);
            throw new Exception("Error retriving CA " + caname + " info.");
        }
        if (result == null) {
            getLogger().debug("CA " + caname + " not found.");
            throw new Exception("CA " + caname + " not found.");
        }
        return result;
    }

    protected void initAuthorizationModule(AuthenticationToken authenticationToken, int caid, String superAdminCN) throws AccessRuleNotFoundException, RoleExistsException, AuthorizationDeniedException, RoleNotFoundException {
    	if (superAdminCN == null) {
    		getLogger().info("Not initializing authorization module.");
    	} else {
    		getLogger().info("Initalizing authorization module with caid="+caid+" and superadmin CN '"+superAdminCN+"'.");
    	}
        ejb.getComplexAccessControlSession().initializeAuthorizationModule(authenticationToken, caid, superAdminCN);
    } // initAuthorizationModule
    
    protected String getAvailableCasString(String cliUserName, String cliPassword) {
		// List available CAs by name
		final StringBuilder existingCas = new StringBuilder();
		try {
			for (final Integer nextId : ejb.getCaSession().getAvailableCAs(getAdmin(cliUserName, cliPassword))) {
				final String caName = ejb.getCaSession().getCAInfo(getAdmin(cliUserName, cliPassword), nextId.intValue()).getName();
				if (existingCas.length()>0) {
					existingCas.append(", ");
				}
				existingCas.append("\"").append(caName).append("\"");
			}
		} catch (Exception e) {
			existingCas.append("<unable to fetch available CA(s)>");
		}
		return existingCas.toString();
    }

    protected String getAvailableEepsString(String cliUserName, String cliPassword) {
		// List available CAs by name
		final StringBuilder existingCas = new StringBuilder();
		try {
			for (final Integer nextId : ejb.getEndEntityProfileSession().getAuthorizedEndEntityProfileIds(getAdmin(cliUserName, cliPassword))) {
				final String caName = ejb.getEndEntityProfileSession().getEndEntityProfileName(nextId.intValue());
				if (existingCas.length()>0) {
					existingCas.append(", ");
				}
				existingCas.append("\"").append(caName).append("\"");
			}
		} catch (Exception e) {
			existingCas.append("<unable to fetch available End Entity Profile(s)>");
		}
		return existingCas.toString();
    }

    protected String getAvailableEndUserCpsString(String cliUserName, String cliPassword) {
		// List available CAs by name
		final StringBuilder existingCas = new StringBuilder();
		try {
			for (final Integer nextId : ejb.getCertificateProfileSession().getAuthorizedCertificateProfileIds(SecConst.CERTTYPE_ENDENTITY, ejb.getCaSession().getAvailableCAs(getAdmin(cliUserName, cliPassword)))) {
				final String caName = ejb.getCertificateProfileSession().getCertificateProfileName(nextId.intValue());
				if (existingCas.length()>0) {
					existingCas.append(", ");
				}
				existingCas.append("\"").append(caName).append("\"");
			}
		} catch (Exception e) {
			existingCas.append("<unable to fetch available Certificate Profile(s)>");
		}
		return existingCas.toString();
    }
    
    /** Lists methods in a class the has "setXyz", and prints them as "Xyz".
     * Ignores (does not list) type, version, latestVersion, upgrade and class
     * 
     * @param obj the Object where to look for setMethods
     */
    protected void listSetMethods(final Object obj) {
        DynaBean wrapper = new WrapDynaBean(obj);
        DynaProperty[] props = wrapper.getDynaClass().getDynaProperties();
        for (DynaProperty dynaProperty : props) {
            if (!dynaProperty.getName().equals("type") && !dynaProperty.getName().equals("version") 
                    && !dynaProperty.getName().equals("class") && !dynaProperty.getName().equals("latestVersion")
                    && !dynaProperty.getName().equals("upgraded") ) {
                System.out.println(dynaProperty.getName()+", "+dynaProperty.getType());
            }
        }
    }
    
    /** gets a field value from a bean
     * 
     * @param field the field to get
     * @param obj the bran to get the value from
     * @return the value
     */
    protected Object getBeanValue(final String field, final Object obj) {
        final DynaBean moddb = new WrapDynaBean(obj);
        final Object gotValue = moddb.get(field);
        getLogger().info(field+" returned value '"+gotValue+"'.");
        return gotValue;
    }
    
    /** Lists, Gets or sets fields in a Bean.
     * 
     * @param listOnly if true, fields will be listed, and nothing more will happen.
     * @param getOnly if true (and listOnly is false), will get the value of a field and nothing else will happen
     * @param name the name of the Bean to be modified
     * @param field the field name to get or set
     * @param value the value to set, of we should set a new value
     * @param obj the Bean to list, get or set fields
     * @return true if we only listed or got a value, i.e. if nothing was modified, false is we set a value.
     */
    protected boolean listGetOrSet(boolean listOnly, boolean getOnly, final String name, final String field, final String value, final Object obj) {
        if (listOnly) {
            listSetMethods(obj);
        } else if (getOnly) {
            getBeanValue(field, obj);
        } else {
            Object val = value;
            getLogger().info("Modifying '"+name+"'...");
            final ConvertingWrapDynaBean db = new ConvertingWrapDynaBean(obj);
            DynaProperty prop = db.getDynaClass().getDynaProperty(field);
            if (prop.getType().isInterface()) {
                System.out.print("Converting value '"+value+"' to type '"+ArrayList.class+"', ");
                // If the value can be converted into an integer, we will use an ArrayList<Integer>
                // Our problem here is that the type of a collection (<Integer>, <String>) is only compile time, it can not be determined in runtime.
                List<Object> arr = new ArrayList<Object>();
                if (StringUtils.isNumeric(value)) {
                    System.out.println("using Integer value.");
                    arr.add(Integer.valueOf(value));
                } else {
                    // Make it into an array of String
                    System.out.println("using String value.");
                    arr.add(value);
                }
                val = arr;
            }
            final Object gotValue = db.get(field);
            getLogger().info("Current value of "+field+" is '"+gotValue+"'.");
            db.set(field, val);
        }
        // return true of we only listed
        return listOnly || getOnly;
    }

}
