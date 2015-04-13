package ru.salauyou.locks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Class that performes hashcode-based segment locking
 * 
 * <p>Main purpose is to lock by different copies of object (<tt>o1.equals(o2)</tt>) the same way
 * as by the same object (<tt>o1 == o2</tt>), which cannot be achieved by <tt>synchronized(o)</tt>.
 * Another purpose is to atomically get a single lock for list of objects.
 * 
 * <p>Each <tt>lockAndGet()</tt> method is blocking, waiting until all needed locks can be acquired,
 * and returns <tt>Lock</tt> object supporting only <tt>unlock()</tt> method, which
 * should be called after operations on objects are finished.
 * 
 * @author Salauyou
 */


public final class LockKeeper {
	
	public static enum LockType {READ, WRITE};
		
	private final int mask;
	private final Map<Class<?>, ReadWriteLock[]> lockStorage = new HashMap<>();
	private final ReadWriteLock[] objectLockStorage;
	private AtomicInteger waiters = new AtomicInteger(0);
	private AtomicInteger cycle = new AtomicInteger(0);
	private Semaphore waitingSemaphore;
	private boolean classed;
	

	
	/**
	 * Creates new <tt>LockKeeper</tt> for <tt>Object</tt> type with 1024 segments
	 */
	public LockKeeper() {
		this(Collections.emptyList(), 10);
		classed = false;
	}
	
	

	/**
	 * Creates new <tt>LockKeeper</tt> for given types and number of segments
	 * 
	 * @param classes		classes that are supposed to be used (LockKeeper 
	 * 						will create separate lock sets for given classes, 
	 * 						and one common set for rest classes)
	 * @param segmentPower	power of 2 base of desired segment number 
	 * 						(i. e. <tt>segmentPower</tt> = 10 
	 * 						means that 1024 segments will be created)
	 */
	public LockKeeper(Collection<Class<?>> classes, int segmentPower) {
		mask = (1 << segmentPower) - 1;
		for (Class<?> clazz : classes) {
			if (clazz != null) {
				ReadWriteLock[] ls = new ReadWriteLock[1 << segmentPower];
				for (int i = 0; i < ls.length; i++) {
					ls[i] = new ReentrantReadWriteLock();
				}
				lockStorage.put(clazz, ls);
			}
		}
		objectLockStorage = new ReadWriteLock[1 << segmentPower];
		for (int i = 0; i < objectLockStorage.length; i++) {
			objectLockStorage[i] = new ReentrantReadWriteLock();
		}
		classed = true;
		waitingSemaphore = new Semaphore(0, true);
	}

	
	
	/**
	 * Returns a lock in locked state for given object
	 */
	public Lock lockAndGet(Object o, LockType lockType) {
		if (o != null) {
			Lock lock = forObject(o, lockType);
			lock.lock();
			return new CompositeLock(this, Arrays.asList(lock));
		}
		return new CompositeLock(this, Collections.emptyList());
	}
	
	
	
	/**
	 * Returns a composite lock in which locks for all given objects are acquired
	 * @throws InterruptedException 
	 */
	public Lock lockAndGet(Collection<? extends Object> objects, LockType lockType) throws InterruptedException {
		if (objects.isEmpty())
			return new CompositeLock(this, Collections.emptyList());
		
		List<Lock> locks = new ArrayList<>(objects.size());
		List<Lock> result = new ArrayList<>(objects.size());
		
		for (Object o : objects) {
			if (o != null) {
				locks.add(forObject(o, lockType));
			}
		}
		
		boolean available;
		boolean notify = false;
		waiters.incrementAndGet();
		do {
			available = true;
			for (Lock lock : locks) {
				if (lock.tryLock()) {
					result.add(lock);
				} else {
					for (Lock lo : result) {
						lo.unlock();
					}
					if (notify) {
						notifyWaiters(false);
					}
					available = false;
					result.clear();
					break;
				}
			}
			if (!available) {
				notify = waitingSemaphore.tryAcquire(1, TimeUnit.MILLISECONDS);
			}
		} while (!available);
		waiters.decrementAndGet();
		return new CompositeLock(this, result);
	}
	
	
	
	private Lock forObject(Object o, LockType lockType) {
		ReadWriteLock[] ls = null;
		if (classed) {
			ls = lockStorage.get(o.getClass());
		}
		if (ls == null) {
			ls = objectLockStorage;
		}
		ReadWriteLock lo = ls[o.hashCode() & mask];
		return (lockType == LockType.READ ? lo.readLock() : lo.writeLock());
	}
	
	

	private synchronized void notifyWaiters(boolean allWaiters) {
		if (allWaiters) {
			cycle.set(waiters.get());
		} 
		if (cycle.decrementAndGet() >= 0) {
			waitingSemaphore.release();
		}
	}
	
	
	
	
	/** ==================================================== **/
	
	
	/**
	 * Composite lock class. Can be obtained by <tt>LockKeeper#lockAndGet</tt>
	 */
	public static class CompositeLock implements Lock {

		private static final String UNSUPPORTED_EXCEPTION_MSG 
				= "This lock is in locked state when obtained by LockKeeper#lockAndGet method";
		
		private final List<Lock> locks;
		private final LockKeeper keeper;
		
		private CompositeLock(LockKeeper lockKeeperUnsafe, List<Lock> locks) {
			this.locks = locks;
			this.keeper = lockKeeperUnsafe;
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
			keeper.notifyWaiters(true);
		}

		@Override
		public Condition newCondition() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
	}
}
