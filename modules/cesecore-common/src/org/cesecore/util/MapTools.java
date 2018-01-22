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
package org.cesecore.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility functions to work with maps.
 * 
 * @version $Id$
 */
public final class MapTools {
    
    /** Utility class, cannot be instantiated */
    private MapTools() { }
    
    /**
     * Adds an item to a map, and returns it. Useful for initialization:
     * <code>
     * doWorkWithMapExample(addToMap(new HashMap<Integer,String>(), 0, "none"));
     * </code>
     * 
     * <p>Unlike the <code>new HashMap<>() {{ put(x,y); }}</code> pattern, this does not create a new class (which can create problems with de-serialization)
     * 
     * @param map Map to use.
     * @param key1 First key.
     * @param value1 Value of first key.
     * @return Returns the map.
     * @see #addToMap(Map, Object, Object, Object, Object)
     * @see #addToMap(Map, Object, Object, Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> addToMap(final Map<K,V> map, final K key1, final V value1) {
        map.put(key1, value1);
        return map;
    }
    
    /**
     * Adds two items to a map, and returns it. Useful for initialization. See {@link #addToMap(Map, Object, Object)}.
     * @see #addToMap(Map, Object, Object)
     * @see #addToMap(Map, Object, Object, Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> addToMap(final Map<K,V> map, final K key1, final V value1, final K key2, final V value2) {
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }
    
    /**
     * Adds three items to a map, and returns it. Useful for initialization. See {@link #addToMap(Map, Object, Object)}.
     * @see #addToMap(Map, Object, Object)
     * @see #addToMap(Map, Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> addToMap(final Map<K,V> map, final K key1, final V value1, final K key2, final V value2, final K key3, final V value3) {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }
    
    /**
     * Creates an unmodifiable map with two items. The returned map is backed by a LinkedHashMap, so it preserves order. Useful for initialization:
     * <code>
     * doWorkWithMapExample(unmodifiableMap(0, "none"));
     * </code>
     * 
     * <p>Unlike the <code>new HashMap<>() {{ put(x,y); }}</code> pattern, this does not create a new class (which can create problems with de-serialization)
     * 
     * @param key1 First key.
     * @param value1 Value of first key.
     * @return Returns the map.
     * @see #unmodifiableMap(Object, Object, Object, Object)
     * @see #unmodifiableMap(Object, Object, Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> unmodifiableMap(final K key1, final V value1) {
        return Collections.unmodifiableMap(addToMap(new LinkedHashMap<K,V>(), key1, value1));
    }
    
    
    /**
     * Creates an unmodifiable map with two items. The returned map is backed by a LinkedHashMap, so it preserves order.
     * Useful for initialization. See {@link #unmodifiableMap(Object, Object)}.
     * @see #unmodifiableMap(Object, Object)
     * @see #unmodifiableMap(Object, Object, Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> unmodifiableMap(final K key1, final V value1, final K key2, final V value2) {
        return Collections.unmodifiableMap(addToMap(new LinkedHashMap<K,V>(), key1, value1, key2, value2));
    }
    
    /**
     * Creates an unmodifiable map with two items. The returned map is backed by a LinkedHashMap, so it preserves order.
     * Useful for initialization. See {@link #unmodifiableMap(Object, Object)}.
     * @see #unmodifiableMap(Object, Object)
     * @see #unmodifiableMap(Object, Object, Object, Object)
     */
    public static <K,V> Map<K,V> unmodifiableMap(final K key1, final V value1, final K key2, final V value2, final K key3, final V value3) {
        return Collections.unmodifiableMap(addToMap(new LinkedHashMap<K,V>(), key1, value1, key2, value2, key3, value3));
    }
    
}
