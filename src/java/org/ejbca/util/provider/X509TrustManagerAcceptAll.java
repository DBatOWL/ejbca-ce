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
package org.ejbca.util.provider;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * This trust manager may be used used by a client that does not bother to verify the TLS certificate chain of the server.
 * Could be us used when you are fetching things from the server that are signed by the server (like certificates).
 * The class must not be used on the server side.
 *
 * @author Lars Silven PrimeKey
 * @version  $Id$
 *
 */
public class X509TrustManagerAcceptAll implements X509TrustManager {

	/**
	 */
	public X509TrustManagerAcceptAll() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// do nothing
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// do nothing
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// only called from server side
		return null;
	}

}
