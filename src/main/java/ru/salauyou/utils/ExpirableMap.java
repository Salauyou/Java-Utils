package ru.salauyou.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Thread-safe {@code Map<K, V>} decorator, where every entry has specified expiration time. 
 * Clean-up of expired elements is performed automatically on every write/read invocation, 
 * so it is guaranteed that no expired entries will be returned by {@code get(key)} method.
 * 
 * Complexities of operations are similar to such in used base map, except {@code put(key, value, lifetime)} 
 * and {@code remove(key)}, that are at least O(n).
 * 
 * @param <K>    key type
 * @param <V>    value type
 */

public class ExpirableMap<K, V> implements Map<K, V> {

    class ExpirationEntry {
        final K key;
        final long expirationTime;
        
        public ExpirationEntry(K key, long expirationTime){
            this.key = key;
            this.expirationTime = expirationTime;
        }
    }
    
    final private Map<K, V> data;
    final private long defaultLifetime;
    
    // list of keys sorted by expiration time (closer to head -> sooner expiration)
    private List<ExpirationEntry> expirationTimes; 
    
    //==============================================================
    
    private void cleanUp(long time){
        Iterator<ExpirationEntry> i = expirationTimes.iterator();
        ExpirationEntry e;
        
        // iterate expiration list from head to tail and remove expired items
        while (i.hasNext() && (e = i.next()).expirationTime <= time){
            data.remove(e.key);
            i.remove();
        }
    }
    
    private void cleanUp(){
        cleanUp(System.currentTimeMillis());
    }

    //==============================================================
    

    /**
     * Creates ExpirableMap and setups default lifetime for entries that will be added further
     * 
     * @param baseMap    Map on which ExpirableMap is based on. All entries that it contains at the moment
     * of ExpirableMap creation are treated as having no lifetime (i. e. existing forever until being 
     * explicitly removed). After ExpirableMap creation, it is not recommended to access baseMap directly
     * @param defaultLifetime    default lifetime in ms
     */
    public ExpirableMap(Map<K, V> baseMap, long defaultLifetime){
        data = baseMap;
        expirationTimes = new LinkedList<ExpirationEntry>();
        this.defaultLifetime = defaultLifetime;
    }
    
    
    @Override
    public int size() {
        synchronized (this){
            cleanUp();
            return data.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this){
            cleanUp();
            return data.isEmpty();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        synchronized (this){
            cleanUp();
            return data.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (this){
            cleanUp();
            return data.containsValue(value);
        }
    }

    @Override
    public V get(Object key) {
        synchronized (this){
            cleanUp();
            return data.get(key);
        }
    }

    /**
     * Puts (K, V) entry with default lifetime
     */
    @Override
    public V put(K key, V value) {
        return put(key, value, defaultLifetime);
    }

    /**
     * Puts (K, V) entry with specified lifetime
     * 
     * @param key
     * @param value
     * @param lifetime    lifetime in ms
     * @return
     */
    public V put(K key, V value, long lifetime) {
        synchronized (this){
            long now = System.currentTimeMillis();
            long expTime = now + lifetime;
            cleanUp(now);
            int index = expirationTimes.size();
            
            // iterate list of expiration entries from tail to head, 
            // looking where the new expiration entry should be inserted
            ListIterator<ExpirationEntry> i = expirationTimes.listIterator(expirationTimes.size());
            while (i.hasPrevious() && i.previous().expirationTime > expTime)
                index--;
            if (index == expirationTimes.size())
                expirationTimes.add(new ExpirationEntry(key, expTime));
            else
                expirationTimes.add(index, new ExpirationEntry(key, expTime));
        
            return data.put(key, value);
        }
    }
    
    /**
     * Puts (K, V) which will never expire
     * 
     * @param key
     * @param value
     * @return
     */
    public V putForever(K key, V value){
        synchronized (this){
            cleanUp();
            return data.put(key, value);
        }
    }
    
    /**
     * Removes (K, V) entry from the Map as specified in Map interface. Since internal 
     * expiration entry should be removed as well, it requires at least O(n) complexity.
     */
    @Override
    public V remove(Object key) {
        synchronized (this){
            cleanUp();
            // searching corresponding expiration entry to remove it together with data entry
            Iterator<ExpirationEntry> i = expirationTimes.iterator();
            ExpirationEntry e;
            while (i.hasNext()){
                e = i.next();
                if (Objects.equals(key, e.key)) {
                    i.remove();
                    return data.remove(key);
                }
            }
            return data.remove(key);
        }
    }

    @Override
    public void clear() {
        data.clear();
        expirationTimes.clear();
    }
    

    
    
    
    // methods to be implemented more carefully
    
    /**
     * Not yet implemented
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not yet synchronized by expiration time! 
     * When elements are accessed by key set, they aren't guaranteed to be unexpired.
     * Expiration check is performed only when this method is called.
     */
    @Override
    public Set<K> keySet() {
        synchronized (this){
            cleanUp();
            return data.keySet();
        }
    }

    /**
     * Not yet synchronized by expiration time! 
     * When elements are accessed by value collection, they aren't guaranteed to be unexpired.
     * Expiration check is performed only when this method is called.
     */
    @Override
    public Collection<V> values() {
        synchronized (this){
            cleanUp();
            return data.values();
        }
    }

    /**
     * Not yet synchronized by expiration time! 
     * When elements are accessed by entry set, they aren't guaranteed to be unexpired.
     * Expiration check is performed only when this method is called.
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        synchronized (this){
            cleanUp();
            return data.entrySet();
        }
    }

}
