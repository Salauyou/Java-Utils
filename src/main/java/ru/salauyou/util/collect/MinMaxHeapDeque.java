package ru.salauyou.util.collect;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;

/**
 * Implementation of a priority deque based on min-max heap:
 * {@link http://cglab.ca/~morin/teaching/5408/refs/minmax.pdf}
 * <p>
 * As in most {@code Deque} implementations, null elements aren't 
 * allowed
 * 
 * @author Aliaksandr Salauyou
 *
 * @param <E>
 */
public class MinMaxHeapDeque<E> extends AbstractDeque<E> {

  // TODO: iteratorless implementation of `remove()`
  
  
  /** Zero-based heap array */
  final List<E> heap;
  
  final Comparator<? super E> cmp;
  
  
  // --------- constructors --------- //
 
  
  /**
   * Creates an empty natural-ordered {@code MinMaxHeapDeque}
   */
  public MinMaxHeapDeque() {
    this((Comparator<? super E>) null);
  }
  
  
  /**
   * Creates an empty {@code MinMaxHeapDeque} which will use
   * provided comparator for ordering
   */
  public MinMaxHeapDeque(Comparator<? super E> cmp) {
    this.heap = new ArrayList<>();
    this.cmp = cmp;
  }
  
  
  /**
   * Creates a natural-ordered {@code MinMaxHeapDeque} initialized 
   * by provided elements
   */
  public MinMaxHeapDeque(Collection<? extends E> c) {
    this(c, null);
  }
  
  
  /**
   * Creates a {@code MinMaxHeapDeque} initialized 
   * by provided elements and comparator
   */
  public MinMaxHeapDeque(Collection<? extends E> c, Comparator<? super E> cmp) {
    if (c instanceof MinMaxHeapDeque) {
      @SuppressWarnings("unchecked")
      Comparator<? super E> comp = ((MinMaxHeapDeque<E>) c).cmp;
      if (comp == cmp) {
        this.heap = new ArrayList<>(((MinMaxHeapDeque<? extends E>) c).heap);
        this.cmp = comp;
        return;
      }
    } 
    this.heap = new ArrayList<>(c);
    this.cmp = cmp;
    for (int i = heap.size() - 1; i >= 0; i--) {
      moveDown(i, isMinLevel(i));
    }
  }
  
  
  /**
   * Creates a {@code MinMaxHeapDeque} initialized by elements
   * and comparator taken from provided {@code SortedSet}
   */
  public MinMaxHeapDeque(SortedSet<? extends E> c) {
    int size = c.size();
    final List<E> es = new ArrayBackedList<>(size);
    int i = 0;
    for (E e : c) {
      if (i < 0) {
        throw new AssertionError();
      }
      es.set(i, e);
      i = nextIndex(i, size);
    }
    this.heap = new ArrayList<E>(es);  // set `ArrayList` elementData directly
    @SuppressWarnings("unchecked")
    Comparator<? super E> cmp = (Comparator<? super E>) c.comparator();
    this.cmp = cmp;
  }
  
  
  static class ArrayBackedList<T> extends AbstractList<T> {
    final T[] arr;    
    @SuppressWarnings("unchecked")
    ArrayBackedList(int size) { this.arr = (T[]) new Object[size]; }
    @Override public T set(int index, T e) { arr[index] = e; return null; }
    @Override public T get(int index) { return arr[index]; }
    @Override public int size() { return arr.length; }
    @Override public Object[] toArray() { return arr; }
  }
  
  
  // ----------- `Deque` implementation ---------- //
  
  @Override
  public int size() {
    return heap.size();
  }


  @Override
  public boolean offer(E e) {
    Objects.requireNonNull(e);
    heap.add(e);
    int size = heap.size();
    if (size > 1) {
      moveUp(size - 1);
    }
    return true;
  }

  
  @Override
  public boolean offerFirst(E e) {
    return offer(e);
  }
  
  
  @SuppressWarnings("unchecked")
  int compare(E e1, E e2) {
    return cmp == null 
        ? ((Comparable<E>) e1).compareTo(e2) 
        : cmp.compare(e1, e2);
  }
  
  void moveUp(int i) {
    int p = parent(i);
    E pe = heap.get(p);
    boolean min = isMinLevel(i);
    int cmp = compare(heap.get(i), pe);
    if (min ? cmp > 0 : cmp < 0) {
      swap(i, p);
      moveUp(p, !min);
    } else {
      moveUp(i, min);
    }
  }
  
  
  void moveUp(int i, boolean min) {
    int g;
    E e = heap.get(i);
    while ((g = grandParent(i)) >= 0) {
      int cmp = compare(e, heap.get(g));
      if (min ? cmp > 0 : cmp < 0) {
        break;
      }
      swap(g, i);
      i = g;
    }
  }

  
  @Override
  public E peek() {
    return heap.size() == 0 ? null : heap.get(0);
  }

  
  @Override
  public E peekLast() {
    int i = maxItem();
    return i < 0 ? null : heap.get(i);
  }

  
  @Override
  public E poll() {
    int size = heap.size();
    if (size == 0) {
      return null;
    } else if (size == 1) {
      return heap.remove(0);
    }
    E res = heap.get(0);
    heap.set(0, heap.remove(size - 1));
    moveDown(0, true);
    return res;
  }

  
  @Override
  public E pollLast() {
    int size = heap.size();
    if (size == 0) {
      return null;
    } else if (size <= 2) {
      return heap.remove(size - 1);
    } 
    int p = maxItem();
    E res = heap.get(p);
    if (size == 3 && p == 2) {
      return heap.remove(2);
    }
    heap.set(p, heap.remove(size - 1));
    moveDown(p, false);
    return res;
  }
  
  
  void moveDown(int i, boolean min) {
    int size = heap.size();
    while (i < size) {
      int p = highDescendant(i, min);
      if (p < 0) {
        return;
      }
      int cmp = compare(heap.get(p), heap.get(i));
      if (i == grandParent(p)) {
        if (min ? cmp < 0 : cmp > 0) {
          swap(i, p);
          int pr = parent(p);
          int c = compare(heap.get(p), heap.get(pr));
          if (min ? c > 0 : c < 0) {
            swap(p, pr);
          }
        }
      } else {
        if (min ? cmp < 0 : cmp > 0) {
          swap(i, p);
          return;
        }
      }
      i = p;
    }
  }
  
  
  /**
   * Returns min (max) node among children and 
   * grandchildren of the given node
   */
  int highDescendant(int i, boolean min) {
    int size = heap.size();
    
    // look among children
    int j = firstChild(i);
    if (j >= size) {
      return -1;
    }
    E high = heap.get(j);
    int p = j;
    E e;
    j++;
    if (j < size) {
      int cmp = compare(e = heap.get(j), high);
      if (min ? cmp < 0 : cmp > 0) {
        high = e;
        p = j;
      }
      
      // look among grandchildren
      int g = firstGrandChild(i);
      for (j = g; j < size && j < g + 4; j++) {
        cmp = compare(e = heap.get(j), high);
        if (min ? cmp < 0 : cmp > 0) {
          high = e;
          p = j;
        }
      }
    }
    return p;
  }
  
  
  void swap(int a, int b) {
    E e = heap.get(a);
    heap.set(a, heap.get(b));
    heap.set(b, e);
  }
   
  
  /**
   * Index of maximum item in the heap
   */
  int maxItem() {
    int size = heap.size();
    if (size <= 0) {
      return -1;
    } else if (size == 1) {
      return 0;
    } else {
      return (size > 2 && compare(heap.get(2), heap.get(1)) > 0) 
           ? 2 : 1;
    }
  }
  
  
  static int firstChild(int index) {
    return ((index + 1) << 1) - 1;
  }

  static int firstGrandChild(int index) {
    return ((index + 1) << 2) - 1;
  }
  
  /**
   * Index of parent node, or -1 if parent doesn't exist
   */
  static int parent(int index) {
    return index < 1 ? -1 : ((index + 1) >>> 1) - 1;
  }
  
  /**
   * Index of grandparent node, or -1 if it doesn't exist
   */
  static int grandParent(int index) {
    return index < 3 ? -1 : ((index + 1) >>> 2) - 1;
  }
  
  static boolean isMinLevel(int index) {
    return (Integer.numberOfLeadingZeros(index + 1) & 1) == 1;
  }

  
  @Override
  public Iterator<E> iterator() {
    return new HeapItr();
  }

  
  @Override
  public Iterator<E> descendingIterator() {
    return new HeapItr();
  }
  
  
  /**
   * Returns 0-based index in a heap array, which follows 
   * given index, or -1 if heap traversal is finished. Heap levels
   * are traversed starting from the top min level, then
   * upside down to the bottom min level, then from the bottom
   * max level downside up to the top max level
   */
  static int nextIndex(int index, int size) {
    if (size <= 1) {
      return -1;
    }
    // TODO: refactor, simplify
    int i = index + 1;            // switch to 1-based
    if (isMinLevel(index)) {      // we are at min level
      if ((i & (i + 1)) == 0) {   // complete min level
        if (i == size) {
          // go to bottom max level
          int bits = 31 - Integer.numberOfLeadingZeros(i);
          return (1 << bits) - 2;
        }
        i = (i + 1) << 1;         // start next min level
        if (i >= size) {
          return size - 1;
        }
      } else {
        i++;
        if (i > size) {
          // go to bottom max level
          int bits = 31 - Integer.numberOfLeadingZeros(i);
          return (1 << bits) - 2;
        }
      }
    } else {                      // we are at max level
      i--;
      // decreasing index resulted complete min level
      if ((i & (i + 1)) == 0) {
        i >>>= 1;
      }
      if (i < 1) {
        return -1;
      }
    }
    return i - 1;
  }
  
  
  // ---------- iterator ------------ //
  
  class HeapItr implements Iterator<E> {
    
    int i = 0;
    int size = MinMaxHeapDeque.this.size();
    int next = size == 0 ? -1 : 0;
    boolean nextCalled = false;
    
    @Override
    public boolean hasNext() {
      if (next >= 0) {
        return true;
      }
      return (next = nextIndex(i, size)) >= 0;
    }
    
    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      i = next;
      next = -1;
      nextCalled = true;
      return MinMaxHeapDeque.this.heap.get(i);
    }

    @Override
    public void remove() {
      if (!nextCalled) {
        throw new IllegalStateException();
      }
      MinMaxHeapDeque<E> q = MinMaxHeapDeque.this;
      E res = q.heap.remove(--size);
      if (i < size) {
        q.heap.set(i, res);
        q.moveDown(i, isMinLevel(i));
      }
      next = -1;
      nextCalled = false;
    }
  }
  

}
