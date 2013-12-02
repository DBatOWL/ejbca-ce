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
package org.cesecore.certificates.ca;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x509.Extensions;
import org.cesecore.certificates.certificate.request.RequestMessage;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.keys.token.CryptoToken;
import org.cesecore.keys.token.CryptoTokenOfflineException;


/**
 * CVCCAImpl is an interface for CVCCAs. There can be different types of CVCCAs.  
 *
 * @version $Id$
 */
public interface CVCCAImpl {

    /**
     * Sets the CA that this implementation class will use.
     */
    public void setCA(CA ca);
    
    /** 
     * @see org.cesecore.certificates.ca.CA#createRequest(CryptoToken, Collection, String, Certificate, int) 
     */
	public byte[] createRequest(CryptoToken cryptoToken, Collection<ASN1Encodable> attributes, String signAlg, Certificate cacert, int signatureKeyPurpose) throws CryptoTokenOfflineException;

    /** 
     * @see org.cesecore.certificates.ca.CA#createAuthCertSignRequest(CryptoToken, byte[]) 
     */
	public byte[] createAuthCertSignRequest(CryptoToken cryptoToken, byte[] request) throws CryptoTokenOfflineException;
	
    /** 
     * @see org.cesecore.certificates.ca.CA#createOrRemoveLinkCertificate(CryptoToken, boolean, CertificateProfile) 
     */
	public byte[] createOrRemoveLinkCertificate(final CryptoToken cryptoToken, final boolean createLinkCertificate, final CertificateProfile certProfile) throws CryptoTokenOfflineException;	

    /** 
     * @see org.cesecore.certificates.ca.CA#generateCertificate(CryptoToken, EndEntityInformation, RequestMessage, PublicKey, int, Date, Date, CertificateProfile, Extensions, String) 
     */
	public Certificate generateCertificate(CryptoToken cryptoToken, EndEntityInformation subject, 
    		RequestMessage request,
            PublicKey publicKey, 
			int keyusage, 
			Date notBefore,
			Date notAfter,
			CertificateProfile certProfile,
			Extensions extensions,
			String sequence) throws Exception;
}
