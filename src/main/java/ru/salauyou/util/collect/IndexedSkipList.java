package ru.salauyou.util.collect;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;


public class IndexedSkipList<E> extends AbstractList<E> 
        implements RandomAccess {

  
  // --------------- constructors ---------------- //
  
  public IndexedSkipList(Comparator<? super E> comparator) {
    cmp = comparator;
    clear();
  }


  public IndexedSkipList() {
    this((o1, o2) -> {
      @SuppressWarnings("unchecked")
      Comparable<? super E> e1 = (Comparable<? super E>) o1;
      return e1.compareTo(o2);
    });
  }


  public IndexedSkipList(Comparator<? super E> comparator,
          Collection<? extends E> source) {
    this(comparator);
    addAll(source);
  }


  public IndexedSkipList(Collection<? extends E> source) {
    this();
    addAll(source);
  }

  
  // ------------- inner properties -------------- //

  final Comparator<? super E> cmp;
  final Random rnd = new Random(1);

  final Node<E> head = new Node<>(null);
  int size;
  Node<E> tail;
  int headLevel;


  // --------------- main methods ----------------- //

  @Override
  public void clear() {
    size = 0;
    tail = null;
    head.next = null;
    head.index = new Index<>(head, null, -1);
    headLevel = 1;
  }


  @Override
  public int size() {
    return size;
  }


  @Override
  public boolean isEmpty() {
    return size == 0;
  }


  @Override
  public boolean add(E e) {

    Objects.requireNonNull(e);

    @SuppressWarnings("unchecked")
    ANode<E>[] nodes = new ANode[headLevel + 1];

    int[] distances = new int[headLevel + 1];
    int index = findElement(e, null, nodes, distances, false);
    Node<E> n = (Node<E>) nodes[0];
    Node<E> newNode = new Node<>(e);

    newNode.next = n.next;
    newNode.pred = n;
    if (n.next != null)
      n.next.pred = newNode;
    n.next = newNode;

    int level = generateIndexLevel();
    Index<E> higher = null; // index node one level higher
    Index<E> higherh = null; // head index node one level higher
    Index<E> newHeadIndex = null;

    @SuppressWarnings("unchecked")
    Index<E>[] newIndex = new Index[level + 1];

    int i = Math.max(level, headLevel);

    // build upper part of head index if level > headLevel
    for (; i > headLevel; i--) {
      Index<E> in = new Index<>(newNode, null, -1); // node for index
      Index<E> inh = new Index<>(head, in, index >= 0 ? index + 2 : (-index));
      newIndex[i] = in;
      if (newNode.index == null)
        newNode.index = in;
      if (newHeadIndex == null)
        newHeadIndex = inh;
      if (higher != null) {
        higher.lower = in;
        higherh.lower = inh;
      }
      higher = in;
      higherh = inh;
    }
    if (higherh != null) {
      headLevel = level;
      higherh.lower = head.index;
      head.index = newHeadIndex;
    }

    // increment distances in index nodes that are higher than level
    for (; i > level; i--) {
      Index<E> pred = (Index<E>) nodes[i];
      if (pred != null && pred.next != null) {
        pred.distance++;
      }
    }

    // build new index
    for (; i > 0; i--) {
      Index<E> in = new Index<>(newNode, null, -1);
      Index<E> pred = (Index<E>) nodes[i];
      newIndex[i] = in;
      if (newNode.index == null)
        newNode.index = in;
      if (higher != null)
        higher.lower = in;
      if (pred != null) {
        in.next = pred.next;
        pred.next = in;
      }
      higher = in;
    }

    // assign distances
    int d = distances[0] + 1;
    for (i = 1; i <= level; i++) {
      if (newIndex[i].next != null) {
        newIndex[i].distance = ((Index<E>) nodes[i]).distance - d + 1;
      }
      if (i < nodes.length) {
        Index<E> in = (Index<E>) nodes[i];
        in.distance = d;
        d += distances[i];
      }
    }
    size++;
    return true;
  }


  @Override
  public boolean remove(Object o) {
    if (o == null) {
      return false;
    }
    @SuppressWarnings("unchecked")
    ANode<E>[] nodes = new ANode[headLevel + 1];
    @SuppressWarnings("unchecked")
    ANode<E>[] nodesPred = new ANode[headLevel + 1];
    @SuppressWarnings("unchecked")
    E e = (E) o;
    int i = findElement(e, nodes, nodesPred, null, true);
    if (i < 0) {
      return false;
    }
    Node<E> n = (Node<E>) nodes[0];
    for (int level = headLevel; level >= 1; level--) {
      Index<E> in = (Index<E>) nodes[level];
      Index<E> inp = (Index<E>) nodesPred[level];
      if (in != null) {
        if (in.next != null) {
          inp.distance = in.distance + inp.distance - 1;
          inp.next = in.next;
        } else {
          inp.distance = -1;
          inp.next = null;
        }
      } else {
        inp.distance--;
      }
    }
    n.pred.next = n.next;
    if (n.next != null) {
      n.next.pred = n.pred;
    }
    n.index = null;   // help GC
    size--;
    return true;
  }

  
  @Override
  public boolean removeAll(Collection<?> c) {
    Objects.requireNonNull(c);
    boolean changed = false;
    for (Object o : c) {
      changed |= remove(o);
    }
    return changed;
  }
  

  @Override
  public int indexOf(Object o) {
    @SuppressWarnings("unchecked")
    E e = (E) o;
    return findElement(e, null, null, null, true);
  }


  @Override
  public int lastIndexOf(Object o) {
    @SuppressWarnings("unchecked")
    E e = (E) o;
    return findElement(e, null, null, null, false);
  }


  @Override
  public boolean contains(Object o) {
    @SuppressWarnings("unchecked")
    E e = (E) o;
    return findElement(e, null, null, null, false) >= 0;
  }

  
  @Override
  public E get(int index) {
    throw new UnsupportedOperationException("to be implemented");
    // TODO: implement
  }

  
  @Override
  public E remove(int index) {
    throw new UnsupportedOperationException("to be implemented");
  }
  

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {

      Node<E> n = head.next;

      @Override
      public boolean hasNext() {
        return n != null;
      }

      @Override
      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        E value = n.value;
        n = n.next;
        return value;
      }
      
      @Override
      public void remove() {
        throw new UnsupportedOperationException("to be implemented");
        // TODO: implement
      }
    };
  }

  
  /**
   * Setting by index not supported
   */
  @Override
  public E set(int index, E element) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  
  /**
   * Setting by index not supported
   */
  @Override
  public void add(int index, E element) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  
  /**
   * Setting by index not supported
   */
  @Override
  public boolean addAll(int index, Collection<? extends E> c)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  
  String verboseToString() {
    if (size == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("INDEX (head level=");
    sb.append(headLevel).append(")\n");
    Index<E> h = head.index;
    while (h != null) {
      sb.append(h);
      Index<E> in = h.next;
      while (in != null) {
        sb.append(" -> ").append(in);
        in = in.next;
      }
      sb.append('\n');
      h = h.lower;
    }

    sb.append('[');
    Node<E> n = head.next;
    while (n != null) {
      sb.append(n.value);
      if (n.index != null) {
        sb.append('*');
      }
      sb.append(',').append(' ');
      n = n.next;
    }
    return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
  }



  // --------------- helper methods -------------------- //

  /**
   * Returns index of first or last occurence of element in the list or
   * (-insertionPoint - 1) if such element doesn't exist.
   * 
   * `nodes` and/or `nodesPred` arrays which size must be (headLevel + 1) become
   * filled by nodes: 
   * 0-th element - node with given value is such found, or node after 
   * which the new node with given value should be inserted; 
   * 1...n-th elements - index nodes of node that was found (`nodes`) 
   * and preceeding index nodes (`nodesPred`)
   */

  int findElement(E e, ANode<E>[] nodes, ANode<E>[] nodesPred,
      int[] distances, boolean first) {
    int i = 0;
    int d = 0;
    Index<E> in = head.index;

    // walk over index
    for (int level = headLevel; level >= 1; level--) {
      d = 0;
      while (indexPreceeds(in.next, e, first)) {
        d += in.distance;
        in = in.next;
      }
      if (nodesPred != null) {
        nodesPred[level] = in;
      }
      if (distances != null) {
        distances[level] = d;
      }
      if (level > 1) {
        in = in.lower;
      }
      i += d;
    }

    // walk over data nodes
    d = 0;
    Node<E> n = in.root;
    while (nodePreceeds(n.next, e, first)) {
      d++;
      n = n.next;
    }
    boolean equal = nodeEquals(n, e);
    if (first && !equal && nodeEquals(n.next, e)) {
      equal = true;
      d++;
      n = n.next;
    }
    if (nodes != null) {
      nodes[0] = n;
    }
    if (nodesPred != null) {
      nodesPred[0] = n;
    }
    if (distances != null) {
      distances[0] = d;
    }
    i += d;
    
    // fill up `nodes` array
    if (first && nodes != null && nodesPred != null) {
      in = null;
      for (int level = headLevel; level >= 1; level--) {
        Index<E> nn = ((Index<E>) nodesPred[level]).next;
        if (in == null && n.index == nn) {
          in = nn;
        }
        nodes[level] = in;
        if (in != null && level > 1)
          in = in.lower;
      }
    }
    return (equal ? i : -i) - 1;
  }


  int generateIndexLevel() {
    int r = rnd.nextInt(size + 1);
    int v = 0;
    while (r > 0 && (r & 1) == 0) {
      v++;
      r = r >>> 1;
    }
    return v;
  }


  // ----------------- compare methods -------------------- //

  boolean indexPreceeds(Index<E> n, E e, boolean strict) {
    if (n == null) {
      return false;
    } else if (n.root == head) {
      return true;
    } else {
      int c = cmp.compare(n.root.value, e);
      return strict ? (c < 0) : (c <= 0);
    }
  }


  boolean nodePreceeds(Node<E> n, E e, boolean strict) {
    if (n == null) {
      return false;
    } else if (n == head) {
      return false;
    } else {
      int c = cmp.compare(n.value, e);
      return strict ? (c < 0) : (c <= 0);
    }
  }


  boolean nodeEquals(Node<E> n, E e) {
    return n != null && n != head
        && cmp.compare(n.value, e) == 0;
  }


  // ---------- nested Node and Index classes ------------ //

  class ANode<T> {} // for extension


  class Node<T> extends ANode<T> {
    Node<T> next, pred;
    Index<T> index;
    T value;

    Node(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value == null ? "HEAD" : value.toString();
    }
  }


  class Index<T> extends ANode<T> {
    Node<T> root;
    Index<T> lower, next;
    int distance;

    Index(Node<T> root, Index<T> next, int distance) {
      this.root = root;
      this.distance = distance;
      this.next = next;
    }

    @Override
    public String toString() {
      return (root.value == null ? "H" : root.value) + "("
          + (distance > 0 ? distance : "∞") + ")";
    }
  }

}
