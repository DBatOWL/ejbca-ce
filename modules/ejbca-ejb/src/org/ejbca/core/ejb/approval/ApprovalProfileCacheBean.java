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
package org.ejbca.core.ejb.approval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.log4j.Logger;
import org.ejbca.config.EjbcaConfiguration;
import org.ejbca.core.ejb.profiles.ProfileData;
import org.ejbca.core.model.approval.ApprovalProfile;

/**
 * Class Holding cache variable.
 * 
 * This cache is designed so only one thread at the time will update the cache if it is too old. Other
 * threads will happily return a bit too old object. If a cache update is forced, for example when
 * a profile is edited, it will always update the cache even if the commit of the transaction fails.
 * 
 * Another known issue during forced updates is the race condition exists, so an update in progress
 * might overwrite the result from forced update's database query.
 * 
 * The intention of this design is better throughput than fully ordered sequential updates.
 * 
 * @version $Id$
 */
@Singleton
@Startup
@DependsOn("StartupSingletonBean")
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class ApprovalProfileCacheBean {

    private final Logger LOG = Logger.getLogger(ApprovalProfileCacheBean.class);

    @EJB
    private ApprovalProfileSessionLocal approvalProfileSession;
    
    /*
     * Cache of profiles, with Id as keys. This cache may be
     * unsynchronized between multiple instances of EJBCA, but is common to all
     * threads in the same VM. Set volatile to make it thread friendly.
     */

    /** Cache of mappings between profileId and profileName */
    private volatile Map<Integer, String> idNameMapCache = null;
    /** Cache of mappings between profileName and profileId */
    private volatile Map<String, Integer> nameIdMapCache = null;
    /** Cache of approval profiles, with Id as keys */
    private volatile Map<Integer, ApprovalProfile> profileCache = null;

    private volatile long lastUpdate = 0;

    /* Create template maps with all static constants */
    private HashMap<Integer, String> idNameMapCacheTemplate;
    private HashMap<String, Integer> nameIdMapCacheTemplate;

    private ReentrantLock lock;

    
    @PostConstruct
    public void initialize() {
        lock = new ReentrantLock(false);
        idNameMapCacheTemplate = new HashMap<Integer, String>();
        nameIdMapCacheTemplate = new HashMap<String, Integer>();
    }
    
    public ApprovalProfileCacheBean() {
        
    }
    
    /**
     * Fetch all profiles from the database, unless cache is enabled, valid and we do not force an update.
     * 
     * @param entityManager is required for reading the profiles from the database if we need to update the cache
     * @param force if true, this will force an update even if the cache is not yet invalid
     */
    public void updateProfileCache(final boolean force) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(">updateProfileCache");
        }
        
        final long cacheApprovalProfileTime = EjbcaConfiguration.getCacheApprovalProfileTime();
        final long now = System.currentTimeMillis();
        // Check before acquiring lock
        if (!force && cacheApprovalProfileTime != 0 && lastUpdate + cacheApprovalProfileTime > now) {
            return; // We don't need to update cache
        }
        try {
            lock.lock();
            if (!force && cacheApprovalProfileTime != 0 && lastUpdate + cacheApprovalProfileTime > now) {
                return; // We don't need to update cache
            }
            lastUpdate = now; // make sure next thread does not also pass the update test
        } finally {
            lock.unlock();
        }
        final Map<Integer, String> idNameCache = new HashMap<Integer, String>(idNameMapCacheTemplate);
        final Map<String, Integer> nameIdCache = new HashMap<String, Integer>(nameIdMapCacheTemplate);
        final Map<Integer, ApprovalProfile> profCache = new HashMap<Integer, ApprovalProfile>();
        try {
            final List<ProfileData> result = approvalProfileSession.findAllApprovalProfiles();
            for (final ProfileData current : result) {
                final Integer id = current.getId();
                final String approvalProfileName = current.getProfileName();
                idNameCache.put(id, approvalProfileName);
                nameIdCache.put(approvalProfileName, id);
                profCache.put(id, current.getProfile());
            }
        } catch (Exception e) {
            LOG.error("Error reading certificate profiles: ", e);
        }
        idNameMapCache = idNameCache;
        nameIdMapCache = nameIdCache;
        profileCache = profCache;
        if (LOG.isTraceEnabled()) {
            LOG.trace("<updateProfileCache");
        }
    }

    /** @return the latest object from the cache or a current database representation if no caching is used. */
    public Map<Integer, ApprovalProfile> getProfileCache() {
        updateProfileCache(false);
        return profileCache;
    }

    /** @return the latest object from the cache or a current database representation if no caching is used. */
    public Map<Integer, String> getIdNameMapCache() {
        updateProfileCache(false);
        return idNameMapCache;
    }

    /** @return the latest object from the cache or a current database representation if no caching is used. */
    public Map<String, Integer> getNameIdMapCache() {
        updateProfileCache(false);
        return nameIdMapCache;
    }
}
