package ru.salauyou.utils.locks;

import static java.lang.Thread.currentThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import ru.salauyou.utils.locks.LockKeeper.LockType;

/**
 * Version 2 of {@link LockKeeper}, adopted to highly concurrent 
 * environment.
 * <p>
 * <b>Not implemented yet:</b>
 * <ul>
 * <li>read locks
 * <li>reentrancy
 * </ul>
 * <p>
 * After implementation of these features, will replace current
 * version of {@link LockKeeper}.
 * 
 * @author Salauyou
 */
public class LockKeeperV2 {

    /*
     * This implementation aims to:
     * 1) get rid of global lock for waiter queue
     * 2) allow concurrent acquision for non-overlapping stripes
     * 3) switch from Lock[] to int[]
     * 
     * TODO: make locks reentrant
     * TODO: implement readlock
     */
    
    final int mask;
    final AtomicIntegerArray stripes;
    final Queue<Waiter> waiters = new ConcurrentLinkedQueue<>();
    final Map<Class<?>, Integer> shiftsForClasses;   
    
    
    /**
     * Creates a new LockKeeper with specified number of stripes
     * @param segmentSizeLog log₂ of stripe number for each class. 
     *        E. g. "10" means "1024 stripes"
     * @param classes classes for which separate segments (stripe sets)
     *        should be created. Total number of stripes will be 
     *        (|classes| + 1) * 2<sup>segmentSizeLog</sup>
     */
    public LockKeeperV2(int segmentSizeLog, Class<?>... classes) {
        mask = (1 << segmentSizeLog) - 1;
        int size = mask + 1;
        if (classes.length > 0) {
            shiftsForClasses = new HashMap<>();
            for (Class<?> cl : classes)
                shiftsForClasses.put(cl, (mask + 1) * (shiftsForClasses.size() + 1));
            size *= shiftsForClasses.size() + 1;
        } else 
            shiftsForClasses = null;        
        stripes = new AtomicIntegerArray(size);
    }
    
    
    
    /**
     * Creates a new LockKeeper with one segment of 1024 
     * stripes for all classes
     */
    public LockKeeperV2() {
        this(10);
    }
      
    
    public Lock lockAndGet(Object o) throws InterruptedException {
        return lockAndGet(LockType.WRITE, new Object[]{o});
    }
    
    
    public Lock lockAndGet(LockType lockType, Object o) 
                                              throws InterruptedException {
        return lockAndGet(lockType, new Object[]{o});
    }
    
    
    public Lock lockAndGet(Object... objects) throws InterruptedException {
        return lockAndGet(LockType.WRITE, objects);
    }
    
    
    
    public Lock lockAndGet(LockType lockType, Object... objects) 
                                              throws InterruptedException {
        int[] locks = collectLocks(objects);
        Thread t = tryGetLocks(locks, false);
        if (t == null)
            return new CompositeLock(locks, LockType.WRITE);
        else {
            Waiter w = new Waiter(t, locks);
            waiters.add(w);
            LockSupport.park();
            while (!w.allAcquired) {
                LockSupport.park();
                if (currentThread().isInterrupted())
                    // TODO: release locks if any are held
                    throw new InterruptedException();
            }
            return new CompositeLock(locks, LockType.WRITE);
        }   
    }
    
    
    
    // ------------------ private stuff ---------------------- //
    
    
    
    int[] collectLocks(Object... os) {
        Set<Integer> pos = new HashSet<>();
        for (Object o : os) 
            pos.add(stripeForObject(o));
        int[] ps = new int[pos.size()];
        int i = 0;
        for (Integer p : pos)
            ps[i++] = p;
        Arrays.sort(ps);    // avoid deadlocking in `tryAllLocks`
        return ps;
    }
    
    
    
    int stripeForObject(Object o) {
        int shift = shiftsForClasses == null 
              ? 0 : shiftsForClasses.getOrDefault(o.getClass(), 0);
        return shift + (o.hashCode() & mask);
    } 
    
    
    
    Thread tryGetLocks(final int[] locks, boolean precheck) {       
        // acquision of locks is performed in two stages:
        // 1) reservation stage, where each lock is "reserved",
        //    then tested if it can be acquired. This is a place 
        //    where lock-freeness is violated: before reservation, 
        //    busy waiting is peformed if the lock is already reserved 
        //    by another thread, so current thead can be "blocked" for
        //    unknown amount of time (fortunately, this is a rare case). 
        //    If it is found that some particular lock is unable 
        //    to be acquired, all reserved locks gets unreserved 
        //    and a current thread is returned;
        // 2) acquision stage, where locks are marked acquired
        //    and reservation marks are cleared. 
        
        // pre-check
        if (precheck) {
            for (int lo : locks) {
                if (isWriteLocked(stripes.get(lo)))
                    return currentThread();
            }
        }
        
        // reserved locks
        final List<Integer> res = new ArrayList<>(locks.length);
        for (int lo : locks) {         
            for (;;) {
                int v;
                while ((isReserved(v = stripes.get(lo))));  
                if (isWriteLocked(v)) {                     
                    for (int lk : res)                      
                        stripes.set(lk, setUnreserved(stripes.get(lk)));
                    return currentThread();
                }
                if (stripes.compareAndSet(lo, v, setReserved(v))) { 
                    res.add(lo);
                    break;
                }
            }
        }
        for (int lo : locks) 
            stripes.set(lo, setWriteLocked(stripes.get(lo)));
        return null;
    }
    
   
    
    final AtomicBoolean queueLock = new AtomicBoolean();  // synchronizer for `tryUnlockWaiters` 
    final AtomicLong opCounter = new AtomicLong();        // operation counter
    final List<Waiter> newWaiters = new ArrayList<>();
    
    
    void tryUnlockWaiters() {
        opCounter.incrementAndGet();
        for (;;) {
            if (!queueLock.compareAndSet(false, true))
                return;
            long c = opCounter.get();
            Waiter w;
            while ((w = waiters.poll()) != null) {
                if (tryGetLocks(w.locks, true) == null) {
                    w.allAcquired = true;
                    LockSupport.unpark(w.th);
                } else
                    newWaiters.add(w);
            }
            waiters.addAll(newWaiters);
            newWaiters.clear();            
            queueLock.set(false);
            
            // return only if no unlocks occurred
            // during queue traversal
            if (opCounter.get() == c) 
                return;
        }
    }
       
    

    static final int RESERVED_BIT     = 1 << 30;
    static final int WRITE_LOCKED_BIT = 1 << 29;
    
    
    static boolean isReserved(int lock) {
        return (RESERVED_BIT & lock) > 0;
    }
    
    static int setReserved(int lock) {
        return RESERVED_BIT | lock;
    }
    
    static int setUnreserved(int lock) {
        return ~RESERVED_BIT & lock;
    }
    
    static boolean isWriteLocked(int lock) {
        return (WRITE_LOCKED_BIT & lock) > 0;
    }
    
    static int setWriteLocked(int lock) {
        return WRITE_LOCKED_BIT | setUnreserved(lock);
    }
    
    static int setWriteUnlocked(int lock) {
        return ~WRITE_LOCKED_BIT & setUnreserved(lock);
    }
    
    
    
    public static class Waiter {
        
        volatile boolean allAcquired = false;
        final Thread th;
        final int[] locks;
        
        Waiter(final Thread th, final int[] locks) {
            this.th = th;
            this.locks = locks;
        }
    }
    
    
    
    
    /**
     * Composite lock class. Can be obtained by <tt>LockKeeper#lockAndGet</tt>
     */
    public class CompositeLock implements Lock {

        private static final String UNSUPPORTED_MSG 
                = "This lock is in locked state when obtained by LockKeeper#lockAndGet method";
        
        final int[] keptLocks;
        
        CompositeLock(int[] locks, LockType type) {
            keptLocks = locks;
        }
        
        @Override
        public void unlock() {
            for (int lo : keptLocks)
                stripes.set(lo, setWriteUnlocked(stripes.get(lo)));
            LockKeeperV2.this.tryUnlockWaiters();
        }

        @Override
        public void lock() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_MSG);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException, 
                                               UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_MSG);
        }

        @Override
        public boolean tryLock() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_MSG);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) 
                                         throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_MSG);
        }
        
        @Override
        public Condition newCondition() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
    
}

