Java-Utils
==========

Some useful utilities I use in Java projects.

**ExpirableHashMap** class - `Map<K, V>` implementation, based on `HashMap`, where every entry has an expiration time measured from the moment the entry was added. Each time you read an entry, if the map returns it, it is guaranteed that such entry is not expired yet.
Expiration management is implemented with internal `LinkedList` and is performed automatically on write/read operations, without need of additional watcher threads.

**Point**, **GeoPoint** immutable classes present a point in 2d cartesian and geo coordinates, **GeoProjections** class purpose is to convert geo to cartesian and vice versa. 

**GeoCalculations** - class to perform some calculations in geo coordinates.
