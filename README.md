Java utils
==========

Some useful utilities I use in Java projects.

[`LockKeeper`](src/main/java/ru/salauyou/util/concurrent/LockKeeper.java) — 
hash-based stripe locker allowing to acquire one composite lock 
for multiple objects by single operation. It is ***insensitive to order*** 
(objects can be passed in any order), ***fair***, implements 
***all-or-nothing*** strategy. Are supported ***exclusive*** (write) as well 
as ***shared*** (read) locks.

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

[`BeanHelper`](src/main/java/ru/salauyou/util/misc/BeanHelper.java):

`BeanHelper.cloneOf()` — utility to deeply clone a bean with nested hirerarchy without 
using serialization, handling nested maps and collections and proxied properties 
(JDK, Javassist, Spring and CGLIB proxies);

`BeanHelper.resolveTypeArguments()` — utility to resolve actual type arguments
that given type defines for given generic supertype directly or indirectly

[`EntityMapper`](src/main/java/ru/salauyou/util/mapper/EntityMapper.java) — 
utility to simplify converting from one object (document, CSV line, java object etc) 
to some java bean which may have deep nested hierarchy. Mappings are defined
as elementary "mappers" (lambdas which extract scalar properties from the source
object) mapped to bean-style properties of destination class 
(they are accessed by correspondent setters behind the scenes).
Simple example of usage: [`TestMapper.java`](src/test/java/tests/mapper/TestMapper.java).

[`MinMaxHeapDeque`](src/main/java/ru/salauyou/util/collect/MinMaxHeapDeque.java) — 
double-ended priority queue based on [min-max heap](https://en.wikipedia.org/wiki/Min-max_heap). 
Unlike Guava's [`MinMaxPriorityQueue`]
(https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/MinMaxPriorityQueue.html),
it fully implements `Deque` interface.

[`KCombinationIterator`](src/main/java/ru/salauyou/util/misc/KCombinationIterator.java) — 
iterator over <a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> 
of a given `Collection<T>`
