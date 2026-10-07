package com.smarthostel.config;

/**
 * Names of the Redis caches. Each one has its own time-to-live (TTL) in RedisCacheConfig.
 * Only data that changes rarely is cached. Allocations, occupancy, complaint / leave /
 * visitor status and fees are NOT cached - they are always read from Oracle.
 */
public final class CacheNames {

    private CacheNames() {
    }

    public static final String HOSTELS = "hostels";                       // TTL 60 min
    public static final String ANNOUNCEMENTS = "announcements";           // TTL 10 min
    public static final String MESS_MENU = "messMenu";                    // TTL 6 hours
    public static final String EMERGENCY_CONTACTS = "emergencyContacts";  // TTL 60 min
    public static final String DASHBOARD_STATS = "dashboardStats";        // TTL 60 seconds
}
