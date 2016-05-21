package ru.salauyou.util.concurrent;

import static java.lang.Thread.currentThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;


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
     *        (|classes| + 1) * 2<sup>segmentSizeLog</sup>. Only classes
     *        are accepted, attempt to pass an iterface will cause
     *        <tt>IllegalArgumentException</tt>
     */
    public LockKeeperV2(int segmentSizeLog, Class<?>... classes) {
        mask = (1 << segmentSizeLog) - 1;
        int size = mask + 1;
        if (classes.length > 0) {
            Map<Class<?>, Integer> shifts = new HashMap<>();
            for (Class<?> cl : classes) {
                if (cl.isInterface())
                    throw new IllegalArgumentException("Class " + cl.getName() + " is an interface, not a class");
                shifts.put(cl, (mask + 1) * (shifts.size() + 1));
            }
            size *= shifts.size() + 1;
            shiftsForClasses = Collections.unmodifiableMap(shifts);
        } else {
            shiftsForClasses = null;     
        }
        stripes = new AtomicIntegerArray(size);
    }
    
    
    
    /**
     * Creates a new LockKeeper with one segment of 1024 
     * stripes for all classes
     */
    public LockKeeperV2() {
        this(10);
    }
      
    
    /**
     * Returns exclusive (write) lock in a locked state for a given object
     */
    public Lock lockAndGet(Object o) throws InterruptedException {
        return lockAndGet(LockType.WRITE, new Object[]{o});
    }
    
    
    public Lock lockAndGet(LockType lockType, Object o) 
                                              throws InterruptedException {
        return lockAndGet(lockType, new Object[]{o});
    }
    
    
    /**
     * Returns exclusive (write) lock in a locked state for all given objects
     */
    public Lock lockAndGet(Object... objects) throws InterruptedException {
        return lockAndGet(LockType.WRITE, objects);
    }    
    
   
    public Lock lockAndGet(LockType lockType, Object... objects) 
                                              throws InterruptedException {
        int[] locks = collectLocks(objects);
        Thread t = tryGetLocks(locks, false);
        if (t != null) {
            Waiter w = new Waiter(t, locks);
            waiters.add(w);
            tryUnlockWaiters();         // if locks are acquired here, 
            LockSupport.park();         // this won't block 
            while (!w.allAcquired) {
                LockSupport.park();
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        }
        return new CompositeLock(locks, LockType.WRITE);
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
        if (o == null)
            return 0;
        Class<?> cl = o.getClass();
        Integer shift = null;
        while (cl != Object.class && (shift = shiftsForClasses.get(cl)) == null)
            cl = cl.getSuperclass();
        if (shift == null)
            shift = 0;
        return shift + (o.hashCode() & mask);
    } 
    

    
    Thread tryGetLocks(final int[] locks, boolean precheck) {       
        // acquisition of locks is performed in two stages:
        
        // 1) reservation stage, where each lock is "reserved",
        //    then tested if it can be acquired. This is a place 
        //    where lock-freeness is violated: before reservation, 
        //    busy waiting is peformed if the lock is already reserved 
        //    by another thread, so current thead can be "blocked" for
        //    unknown amount of time (fortunately, this is a rare case). 
        //    If it is found that some particular lock is unable 
        //    to be acquired, all reserved locks gets unreserved 
        //    and a current thread is returned;
        // 2) acquisition stage, where locks are marked acquired
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
    
 
   
    final AtomicLong opCounter = new AtomicLong();  // operation counter
    
    
    void tryUnlockWaiters() {
        opCounter.incrementAndGet();
        for (;;) {
            final List<Waiter> ws = new ArrayList<>();
            final long c = opCounter.get();
            Waiter w;
            while ((w = waiters.poll()) != null) {
                if (tryGetLocks(w.locks, true) == null) {
                    w.allAcquired = true;
                    LockSupport.unpark(w.th);
                } else
                    ws.add(w);
            }
            waiters.addAll(ws);
            
            // return only if no unlocks occurred
            // during queue traversal
            if (opCounter.get() == c) 
                return;
        }
    }
       
    
    public static enum LockType { READ, WRITE }
    

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
                = "This lock is in locked state when obtained by lockAndGet()";
        
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
            throwUnsupported(UNSUPPORTED_MSG);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException, 
                                               UnsupportedOperationException {
            throwUnsupported(UNSUPPORTED_MSG);
        }

        @Override
        public boolean tryLock() throws UnsupportedOperationException {
            return throwUnsupported(UNSUPPORTED_MSG);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) 
                                         throws UnsupportedOperationException {
            return throwUnsupported(UNSUPPORTED_MSG);
        }
        
        @Override
        public Condition newCondition() throws UnsupportedOperationException {
            return throwUnsupported("");
        }
    }
    
    
    static <T> T throwUnsupported(String msg) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(msg);
    }
    
}

