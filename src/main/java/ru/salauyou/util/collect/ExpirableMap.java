package ru.salauyou.util.collect;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Synchronized {@link Map} decorator, where each entry has
 * specified expiration time. Clean-up of expired elements 
 * is performed automatically on every write/read operation, 
 * so it is guaranteed that no expired entries will be 
 * returned by {@link ExpirableMap#get(Object)}.
 * <p>
 * Complexity of operations are similar to such in used 
 * backed map, except {@link ExpirableMap#put(Object, Object, long)}
 * and {@link ExpirableMap#remove(Object)}, that are O(n).
 * <p>
 * <i>Iterators are not synchronized by expiration time (i. e.
 * can return expired elements). This wrong behavior is a subject 
 * for correction in further implementation</i>
 * 
 * @author Salauyou
 */
public class ExpirableMap<K, V> extends AbstractMap<K, V> {

    class ExpirationEntry {
        final K key;
        final long deadline;
        
        public ExpirationEntry(K key, long deadline) {
            this.key = key;
            this.deadline = deadline;
        }
    }
    
    final private Map<K, V> data;
    final private long defaultLifetime;
    
    
    // list of keys sorted by expiration time 
    // (closer to head -> sooner expiration)
    private final List<ExpirationEntry> deadlines = new LinkedList<>(); 
    
    
    //==============================================================
    
    private void cleanUp(long time) {
        Iterator<ExpirationEntry> i = deadlines.iterator();
        ExpirationEntry e;
        
        // remove expired entries
        while (i.hasNext() && (e = i.next()).deadline <= time){
            data.remove(e.key);
            i.remove();
        }
    }
    
    
    private void cleanUp() {
        cleanUp(System.currentTimeMillis());
    }

    
    
    //==============================================================
    

    /**
     * Creates ExpirableMap and setups default lifetime 
     * for entries that will be added further.
     * 
     * @param baseMap backed map. All entries that it contains so far
     *        become treated as stored with no deadline, i. e. "forever"
     * @param defaultLifetime default lifetime in ms
     * @throws IllegalArgumentException if defaultLifetime < 0
     */
    public ExpirableMap(Map<K, V> baseMap, long defaultLifetime) throws IllegalArgumentException {
        if (defaultLifetime < 0)
            throw new IllegalArgumentException("defaultLifetime must be >= 0");
        data = baseMap;
        this.defaultLifetime = defaultLifetime;
    }
    
    
    @Override
    public synchronized int size() {
        cleanUp();
        return data.size();
    }

    
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    
    @Override
    public synchronized boolean containsKey(Object key) {
        cleanUp();
        return data.containsKey(key);
    }

    
    @Override
    public synchronized boolean containsValue(Object value) {
        cleanUp();
        return data.containsValue(value);
    }

    
    @Override
    public synchronized V get(Object key) {        
        cleanUp();
        return data.get(key);
    }

    
    /**
     * Puts an entry with default lifetime
     */
    @Override
    public synchronized V put(K key, V value) {
        return put(key, value, defaultLifetime);
    }

    
    /**    
     * Puts an entry with specified lifetime
     * 
     * @param lifetime    lifetime in ms
     * @throws IllegalArgumentException if lifetime < 0
     */
    public synchronized V put(K key, V value, long lifetime) throws IllegalArgumentException {
        if (lifetime < 0)
            throw new IllegalArgumentException("lifetime must be >= 0");
        long now = System.currentTimeMillis();
        long expTime = now + lifetime;
        if (expTime < 0) // overflow!
            expTime = Long.MAX_VALUE;
        cleanUp(now);
        int index = deadlines.size();

        // iterate over entries looking where the new 
        // expiration entry should be inserted
        ListIterator<ExpirationEntry> i = deadlines.listIterator(deadlines.size());
        while (i.hasPrevious() && i.previous().deadline > expTime)
            index--;
        if (index == deadlines.size())
            deadlines.add(new ExpirationEntry(key, expTime));
        else
            deadlines.add(index, new ExpirationEntry(key, expTime));
        return data.put(key, value);
    }
    
    
    /**
     * Puts (K, V) which will never expire
     */
    public synchronized V putForever(K key, V value) {   
        cleanUp();
        return data.put(key, value);
    }
    
    
    @Override
    public synchronized V remove(Object key) {
        cleanUp();
        // search for corresponding expiration entry 
        // to remove it together with data entry
        Iterator<ExpirationEntry> i = deadlines.iterator();
        ExpirationEntry e;
        while (i.hasNext()) {
            e = i.next();
            if (Objects.equals(key, e.key)) {
                i.remove();
                return data.remove(key);
            }
        }
        return data.remove(key);
    }

    
    @Override
    public synchronized void clear() {
        data.clear();
        deadlines.clear();
    }
    

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        cleanUp();
        return data.entrySet();
    }

}
