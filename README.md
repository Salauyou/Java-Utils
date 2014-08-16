Java utils
==========

Some useful utilities I use in Java projects.

**ExpirableMap** - Thread-safe `Map<K, V>` decorator, where every entry has specified expiration time 
which is measured from the moment the entry was added. Clean-up of expired elements is performed automatically 
on every write/read invocation, so it is guaranteed that no expired entries will be returned by `get(key)` method.
Expiration management is implemented based on internal `LinkedList` and don't utilize any additional threads.

**Point**, **GeoPoint** immutable classes present points in 2d cartesian and geo coordinates, 
**GeoProjections** class purpose is to convert such points one to another using some projection model. 

**GeoCalculations** - class to perform trilateration in geo coordinates.
