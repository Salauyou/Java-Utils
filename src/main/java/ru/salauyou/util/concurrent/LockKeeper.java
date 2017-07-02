package ru.salauyou.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Hashcode-based segment locking by one or multiple objects
 * 
 * <p>Main purpose is to lock by different copies of object (<tt>o1.equals(o2)</tt>) like they are
 * the same object (<tt>o1 == o2</tt>), which cannot be achieved by <tt>synchronized(o)</tt>.
 * Another purpose is to atomically get a single lock for collection of objects. When locking
 * by multiple objects, all-or-nothing approach is implemented, so locks are obtained as soon
 * as they become available for a given thread (other waiting threads don't hold any locks).
 * 
 * <p><tt>lockAndGet()</tt> method is blocking, it waits until all needed locks can be acquired,
 * and returns <tt>Lock</tt> object which supports only <tt>unlock()</tt> method
 * to be called after operations on objects are finished.
 * 
 * @author Salauyou
 */


public final class LockKeeper {
    
    public static enum LockType { READ, WRITE };
        
    private final int mask;
    private final Map<Class<?>, ReentrantReadWriteLock[]> lockStorage = new HashMap<>();
    private final ReentrantReadWriteLock[] objectLockStorage;
    private boolean classed;
    
    final Lock queueLock = new ReentrantLock();
    final Deque<Waiter> waiters = new LinkedList<>();

    
    /**
     * Creates new <tt>LockKeeper</tt> for <tt>Object</tt> type with 1024 segments
     */
    public LockKeeper() {
        this(10);
        classed = false;
    }
    
    

    /**
     * Creates new <tt>LockKeeper</tt> for given types and number of segments
     * 
    
     * @param segmentPower  power of 2 base of desired segment number 
     *                      (i. e. <tt>segmentPower</tt> = 10 
     *                      means that 1024 segments will be created)
     * @param classes       classes that are supposed to be used (LockKeeper 
     *                      will create separate lock set for every class from 
     *                      the list, and one common set for rest)
     */
    public LockKeeper(int segmentPower, Class<?>... classes) {
        mask = (1 << segmentPower) - 1;
        for (Class<?> clazz : classes) {
            if (clazz != null) {
                ReentrantReadWriteLock[] ls = new ReentrantReadWriteLock[1 << segmentPower];
                for (int i = 0; i < ls.length; i++) {
                    ls[i] = new ReentrantReadWriteLock();
                }
                lockStorage.put(clazz, ls);
            }
        }
        objectLockStorage = new ReentrantReadWriteLock[1 << segmentPower];
        for (int i = 0; i < objectLockStorage.length; i++) {
            objectLockStorage[i] = new ReentrantReadWriteLock();
        }
        classed = true;
    }

    
    
    /**
     * Returns exclusive (write) in a locked state for given object
     */
    public Lock lockAndGet(Object o) throws InterruptedException {
        return lockAndGet(LockType.WRITE, o);
    }
    
    
    
    /**
     * Returns a lock in locked state for given object
     */
    public Lock lockAndGet(LockType lockType, Object o) throws InterruptedException {
        if (o != null) {
            ReadWriteLock lock = forObject(o);
            Lock lo = lockType == LockType.READ ? lock.readLock() : lock.writeLock();
            lo.lock();
            return new CompositeLock(Arrays.asList(lock), lockType, this);
        }
        return new CompositeLock(Collections.emptyList(), lockType, this);
    }
    
    
    
    /**
     * Returns a composite exclusive (write) lock in which locks 
     * for all given objects are acquired
     */
    public Lock lockAndGet(Object... objects) throws InterruptedException {
        return lockAndGet(LockType.WRITE, objects);
    }
    
    
    
    /**
     * Returns a composite lock in which locks for all given objects are acquired
     */
    public Lock lockAndGet(LockType lockType, Collection<? extends Object> objects)
                                                        throws InterruptedException {
        return lockAndGet(lockType, objects.toArray());
    }
    
    
    
    /**
     * Returns a composite lock in which locks for all given objects are acquired
     */
    public Lock lockAndGet(LockType lockType, Object... objects) 
                                                        throws InterruptedException {
        if (objects.length == 0)
            return new CompositeLock(Collections.emptyList(), lockType, this);
        
        List<ReentrantReadWriteLock> locks = new ArrayList<>(objects.length);
        
        for (Object o : objects) {
            if (o != null) {
                locks.add(forObject(o));
            }
        }
        Waiter w = null;
        queueLock.lock();
        try {
            int r = tryAllLocks(locks, lockType);
            if (r > 0) 
                return new CompositeLock(locks, lockType, this);
            else {
                w = new Waiter();
                waiters.add(w);
            }
        } finally {
            queueLock.unlock();
        }
        for (;;) {
            w.semIn.acquire();
            int r = tryAllLocks(locks, lockType);
            if (r > 0) {
                w.acquired = true;
                w.semOut.release();
                return new CompositeLock(locks, lockType, this);
            } else 
                w.semOut.release();
        }
    }
    
    
    
    // private stuff //
    
    private ReentrantReadWriteLock forObject(Object o) {
        ReentrantReadWriteLock[] ls = null;
        if (classed) {
            ls = lockStorage.get(o.getClass());
        }
        if (ls == null) {
            ls = objectLockStorage;
        }
        return ls[o.hashCode() & mask];
    }
    
    
    /**
     * @return  1 - all locks acquired
     *          0 - all locks were free at the beginning,
     *              but failed to acquire
     *       (-x) - number of locks unable to acquire
     */
    private int tryAllLocks(List<ReentrantReadWriteLock> locks, LockType type) {
        int x = 0;
        for (ReentrantReadWriteLock lock : locks) {
            if ((type == LockType.READ && lock.isWriteLocked()) 
                    || (type == LockType.WRITE 
                          && (lock.isWriteLocked() || lock.getReadLockCount() > 0))) {
                x--;
            }
        }
        if (x < 0) 
            return x;

        List<Lock> result = new ArrayList<>();
        for (ReentrantReadWriteLock lock : locks) {
            Lock lo = type == LockType.READ ? lock.readLock() : lock.writeLock();
            if (lo.tryLock()) {
                result.add(lo);
            } else {
                for (Lock lc : result) {
                    lc.unlock();
                }
                return 0;
            }
        }
        return 1;
    }
    

    
    private List<Waiter> newWaiters = new ArrayList<>();
    
    private void makeRound() {
        queueLock.lock();
        Waiter w = null;
        while ((w = waiters.poll()) != null) {
            if (!w.acquired) {
                w.semIn.release();
                try {
                    w.semOut.acquire();
                } catch (InterruptedException e) {
                    w.acquired = true;
                }
                if (!w.acquired) {
                    newWaiters.add(w);
                }
            }
        }
        waiters.addAll(newWaiters);
        newWaiters.clear();
        queueLock.unlock();
    }
    
    
    
    /** ==================================================== **/
    
    
    static class Waiter {

        final Semaphore semIn = new Semaphore(0);
        final Semaphore semOut = new Semaphore(0);
        volatile boolean acquired = false;
        
        Waiter() { }
    }
    
    
    
    /** ==================================================== **/
    
    
    /**
     * Composite lock class. Can be obtained by <tt>LockKeeper#lockAndGet</tt>
     */
    public static class CompositeLock implements Lock {

        private static final String UNSUPPORTED_EXCEPTION_MSG 
                = "This lock is in locked state when obtained by LockKeeper#lockAndGet method";
        
        private final List<Lock> locks = new ArrayList<>();
        private final LockKeeper keeper;
        
        CompositeLock(List<? extends ReadWriteLock> locks, LockType type, LockKeeper lockKeeper) {
            for (ReadWriteLock lock : locks) {
                this.locks.add(type == LockType.READ ? lock.readLock() : lock.writeLock());
            }
            this.keeper = lockKeeper;
        }
        

        @Override
        public void lock() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_MSG);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException, UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_MSG);
        }

        @Override
        public boolean tryLock() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_MSG);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_MSG);
        }

        @Override
        public void unlock() {
            for (Lock lock : locks) {
                lock.unlock();
            }
            keeper.makeRound();
        }

        @Override
        public Condition newCondition() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
