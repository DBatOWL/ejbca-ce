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
package org.ejbca.core.protocol.acme;

import java.util.List;
import java.util.Map;

import org.ejbca.core.protocol.acme.AcmeIdentifier.AcmeIdentifierTypes;

/**
 * Interface for read operations related to {@link AcmeAuthorizationData}.
 */
public interface AcmeAuthorizationDataSession {
    static final String ACME_MODULE = "acme";


    /**
     *
     * @param authorizationId the ID of the authorization
     * @return the sought authorization, or null if none exists
     */
    AcmeAuthorization getAcmeAuthorization(final String authorizationId);

    /**
     *
     * @param orderId the ID of the order
     * @return list of sought authorizations, or null if none exists
     */
    List<AcmeAuthorization> getAcmeAuthorizationsByOrderId(final String orderId);

    /**
     *
     * @param accountId the ID of the account
     * @return list of sought authorizations, or null if none exists
     */
    List<AcmeAuthorization> getAcmeAuthorizationsByAccountId(final String accountId);
    
    /**
    * // ECA-10060 Consider identical identifiers of different types for future items.
    * 
    * Returns the list of ACME authorizations with the given criteria.
    *  
    * @param accountId the ID of the account.
    * @param identifiers the map of identifier and identifier type (see {@link AcmeIdentifierTypes}) pairs.
    * @param status the List of status (see {@link AcmeAuthorizationStatus}).
    * 
    * @return the list of authorization or an empty list.
    */
   List<AcmeAuthorization> getAcmeAuthorizations(String accountId, Map<String,String> identifiers, List<String> status);

}
