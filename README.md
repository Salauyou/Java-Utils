Java utils
==========

Some useful utilities I use in Java projects.

**LockKeeper** - hash-based segment locking implementation. Allows to acquire one composite lock for multiple objects atomically. It is ***resistant to order*** (order of objects passed can be any), ***fair*** (threads that wait most are first candidates to acquire locks) and implements ***all-or-nothing*** strategy (which allows other waiter threads continue as soon as all needed locks become available).

<pre>
Lock lock = lockKeeper.lockAndGet(Arrays.asList(o1, o2, o3));
try {
    // do some work on objects o1, o2 and o3
} finally {
    lock.release();
}</pre>

**ExpirableMap** - Thread-safe `Map<K, V>` decorator, where every entry has specified expiration time 
which is measured from the moment the entry was put to the map. Clean-up of expired elements is performed automatically 
on every write/read invocation, so it is guaranteed that no expired entries will be returned by `get(key)` method.
Expiration management is implemented based on internal `LinkedList` and don't utilize any additional threads.

**KCombinationIterator** - `Iterator` that sequentially returns all <a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> from given `Collection<T>`

**Point**, **GeoPoint** immutable classes present points in 2d cartesian and geo coordinates, 
**GeoProjections** class purpose is to convert such points one to another using some projection model. 

**GeoCalculations** - class to perform trilateration in geo coordinates.
