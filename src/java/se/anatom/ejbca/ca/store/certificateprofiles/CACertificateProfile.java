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
 
/*
 * CACertificateProfile.java
 *
 * Created on den 29 juli 2002, 22:08
 */
package se.anatom.ejbca.ca.store.certificateprofiles;

import java.util.ArrayList;


/**
 * CACertificateProfile is a class defining the fixed characteristics of a CA certificate profile.
 *
 * @author  TomSelleck
 */
public class CACertificateProfile extends CertificateProfile{

    // Public Constants
    public final static  String CERTIFICATEPROFILENAME =  "SUBCA";

    // Public Methods

    /**
     * Creates a certificate with the characteristics of an end user.
     */
    public CACertificateProfile() {

      setCertificateVersion(VERSION_X509V3);
      setValidity(730);

      setUseBasicConstraints(true);
      setBasicConstraintsCritical(true);

      setUseSubjectKeyIdentifier(true);
      setSubjectKeyIdentifierCritical(false);

      setUseAuthorityKeyIdentifier(true);
      setAuthorityKeyIdentifierCritical(false);

      setUseSubjectAlternativeName(true);
      setSubjectAlternativeNameCritical(false);

      setUseCRLDistributionPoint(false);
      setCRLDistributionPointCritical(false);
      setCRLDistributionPointURI("");

      setUseCertificatePolicies(false);
      setCertificatePoliciesCritical(false);
      setCertificatePolicyId("2.5.29.32.0");
      setType(TYPE_SUBCA);


      int[] bitlengths = {512,1024,2048,4096};
      setAvailableBitLengths(bitlengths);

      setUseKeyUsage(true);
      setKeyUsage(new boolean[9]);
      setKeyUsage(DIGITALSIGNATURE,true);
      setKeyUsage(KEYCERTSIGN,true);
      setKeyUsage(CRLSIGN,true);
      setKeyUsageCritical(true);

      setUseExtendedKeyUsage(false);
      setExtendedKeyUsage(new ArrayList());
      setExtendedKeyUsageCritical(false);
      
      ArrayList availablecas = new ArrayList();
      availablecas.add(new Integer(ANYCA));
      setAvailableCAs(availablecas);      
      setPublisherList(new ArrayList());
    }

    // Public Methods.
    public void upgrade(){
      super.upgrade(); 	
      if(LATEST_VERSION != getVersion()){
        // New version of the class, upgrade
		        
      }      
    }

    // Private fields.
}
