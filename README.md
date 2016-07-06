Java utils
==========

Some useful utilities I use in Java projects.

[`LockKeeper`](src/main/java/ru/salauyou/util/concurrent/LockKeeper.java) — 
hash-based stripe locker, which allows to acquire one composite lock 
for multiple objects by single operation. It is ***insensitive to order*** 
(objects can be passed in any order), ***fair***, implements 
***all-or-none*** strategy (sub-locks aren't acquired one by one, instead,
a thread waits until all needed locks are available. Such approach reduces
contention and allows to avoid deadlocking in multiple lock acquisition). 
Are supported ***exclusive*** (write) as well as ***shared*** (read) locks.

<pre>
Lock lock = lockKeeper.lockAndGet(o1, o2, o3); // blocks until locks for all 
                                               // o1, o2 and o3 become available
try {
    // do some work on objects o1, o2 and o3
} finally {
    lock.unlock();                             // releases locks for all o1, o2 and o3
}</pre>

[`LockKeeperV2`](src/main/java/ru/salauyou/util/concurrent/LockKeeperV2.java) — 
faster implementation, but doesn't yet support reentrancy and read locks

--
[`BeanHelper`](src/main/java/ru/salauyou/util/misc/BeanHelper.java) — 
utility to work with beans. Now, it has only one method `BeanHelper.cloneOf()` 
which deeply clones a bean with nested hirerarchy without using serialization.
Nested maps and collections are deeply cloned, proxied properties are resolved 
to original types (now are handled JDK, Javassist, Spring and CGLIB proxies), 
which allows to easily and safely clone beans that are proxied or have proxied 
properties/collections, such as entity beans produced by Hibernate.

--
[`EntityMapper`](src/main/java/ru/salauyou/util/mapper/EntityMapper.java) — 
utility to simplify converting from one object (document, CSV line, java object etc) 
to some java bean which may have deep nested hierarchy. Mappings are defined
as elementary "mappers" (lambdas which extract scalar properties from the source
object) mapped to bean-style properties of destination class 
(they are accessed by correspondent setters behind the scenes).
Simple example of usage: [`TestMapper.java`](src/test/java/tests/mapper/TestMapper.java).

--
[`MinMaxHeapDeque`](src/main/java/ru/salauyou/util/collect/MinMaxHeapDeque.java) — 
double-ended priority queue based on [min-max heap](https://en.wikipedia.org/wiki/Min-max_heap). 
Unlike Guava's [`MinMaxPriorityQueue`]
(https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/MinMaxPriorityQueue.html),
it fully implements `Deque` interface.

[`ExpirableMap`](src/main/java/ru/salauyou/util/collect/ExpirableMap.java) — 
synchronized `Map<K, V>` decorator, where every entry has specified 
expiration time which is measured from the moment the entry was put 
to the map. Clean-up of expired elements is performed automatically 
on every write/read invocation, so it is guaranteed that no expired 
entries will be returned by retrieval operations. Expiration management 
is implemented based on internal `LinkedList` and don't utilize 
any additional threads.


[`KCombinationIterator`](src/main/java/ru/salauyou/util/misc/KCombinationIterator.java) — 
iterator that sequentially returns all k-subsets, or
<a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> 
of a given `Collection<T>`

`Point`, `GeoPoint` — immutable classes present points in 2d cartesian and 
geo coordinates. `GeoProjections` class purpose is to convert such points 
one to another using some projection model. `GeoCalculations` — class 
to perform trilateration in geo coordinates.
