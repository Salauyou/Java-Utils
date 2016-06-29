package ru.salauyou.util.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class TestMinMaxHeapDeque {

  @Test
  public void testOffer() {
    Deque<Integer> q = ofItems(3, 1, 0, 4, 5, 2);
    assertEquals(6, q.size());
  }
  
  
  @Test(expected = NoSuchElementException.class)
  public void testEmpty() {
    Deque<Integer> q = ofItems();
    assertEquals(0, q.size());
    Iterator<Integer> it = q.iterator();
    assertFalse(it.hasNext());
    it.next();
  }
  
  
  @Test
  public void testPeek() {
    Deque<Integer> q = ofItems(6, 0, 1, 3, 4, 5, 2, 7, 8, 9);
    assertEquals((Integer) 0, q.peek());
    assertEquals((Integer) 9, q.peekLast());
    
    q = new MinMaxHeapDeque<>();
    int size = 100_000;
    List<Integer> items = IntStream.range(0, size).boxed().collect(Collectors.toList());
    Collections.shuffle(items);
    q.addAll(items);
    assertEquals((Integer) 0, q.peek());
    assertEquals((Integer) (size - 1), q.peekLast());
  }
  
  
  @Test
  public void testPoll() {
    Deque<Integer> q = ofItems(9, 0, 1, 3, 4, 5, 2, 7, 8, 6);
    List<Integer> head = new ArrayList<>();
    List<Integer> tail = new ArrayList<>();
    Integer h, t;
    while ((h = q.poll()) != null && (t = q.pollLast()) != null) {
      head.add(h);
      tail.add(t);
    }
    assertEquals(0, q.size());
    assertTrue(q.isEmpty());
    assertEquals(Arrays.asList(0, 1, 2, 3, 4), head);
    assertEquals(Arrays.asList(9, 8, 7, 6, 5), tail);
  }
  
  
  @Test
  public void testPollLarge() {
    int size = 50_000;
    List<Integer> head = IntStream.range(0, size).boxed().collect(Collectors.toList());
    List<Integer> tail = IntStream.range(size, size * 2).boxed().collect(Collectors.toList());
    Collections.reverse(tail);
    List<Integer> all = new ArrayList<>(head);
    all.addAll(tail);
    Collections.shuffle(all);
    
    Deque<Integer> q = new MinMaxHeapDeque<>();
    q.addAll(all);
    
    Integer h, t;
    Iterator<Integer> hi = head.iterator();
    Iterator<Integer> ti = tail.iterator();
    while ((h = q.poll()) != null && (t = q.pollLast()) != null) {
      assertEquals(hi.next(), h);
      assertEquals(ti.next(), t);
    }
    assertTrue(q.isEmpty());
  }
  
  
  @Test
  public void testIterator() {
    Deque<Integer> q = ofItems(9, 0, 1, 3, 4, 5, 2, 7, 8, 6);
    List<Integer> toRemove = Arrays.asList(2, 3, 4, 9, 0);
    Iterator<Integer> it = q.iterator();
    while (it.hasNext()) {
      if (toRemove.contains(it.next())) {
        it.remove();
      }
    }
    assertTrue(q.containsAll(Arrays.asList(1, 5, 6, 7, 8)));
  }
 
  
  @Test
  public void testConsistentRemove() {
    Deque<Integer> q = ofItems(6, 0, 1, 3, 4, 5, 2, 7, 8, 9);
    q.remove(9);
    q.removeLastOccurrence(3);
    q.remove(0);
    q.removeLastOccurrence(4);
    q.remove(2);
    List<Integer> head = new ArrayList<>();
    Integer e;
    while ((e = q.poll()) != null) {
      head.add(e);
    }
    assertEquals(Arrays.asList(1, 5, 6, 7, 8), head);
  }
  
  
  @Test
  public void testGenerateSequence() {
    Integer[][] sequences = new Integer[][] {
      { 0 },
      { 0, 1 },
      { 0, 2, 1 },
      { 0, 3, 2, 1 },
      { 0, 3, 4, 2, 1 },
      { 0, 3, 4, 5, 2, 1 },
      { 0, 3, 4, 5, 6, 2, 1 },
      { 0, 3, 4, 5, 6, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 12, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 13, 12, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 14, 13, 12, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 15, 14, 13, 12, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 15, 16, 14, 13, 12, 11, 10, 9, 8, 7, 2, 1 },
      { 0, 3, 4, 5, 6, 15, 16, 17, 14, 13, 12, 11, 10, 9, 8, 7, 2, 1 },
    };
    for (int i = 0; i < sequences.length; i++) {
      int size = sequences[i].length;
      int j = 0;
      int p = 0;
      while (j >= 0) {
        assertEquals(sequences[i][p], (Integer) j);
        j = MinMaxHeapDeque.nextIndex(j, size);
        p++;
      }
    }
  }
  
  
  @Test
  public void testSortedSetConstructor() {
    SortedSet<Integer> ss = new TreeSet<>(Collections.reverseOrder());
    ss.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
    Deque<Integer> q = new MinMaxHeapDeque<>(ss);
    assertEquals((Integer) 9, q.peek());
    assertEquals((Integer) 1, q.peekLast());
    q.offer(10);
    q.offer(0);
    assertEquals((Integer) 10, q.poll());
    assertEquals((Integer) 9, q.poll());
    assertEquals((Integer) 0, q.pollLast());
    assertEquals((Integer) 1, q.pollLast());
  }
  
  
  @Test
  public void testCollectionConstructor() {
    Deque<Integer> q = new MinMaxHeapDeque<>(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0));
    assertEquals(10, q.size());
    assertEquals((Integer) 0, q.peek());
    assertEquals((Integer) 9, q.peekLast());
    
    int size = 100;
    List<Integer> ordered = IntStream.range(0, size).boxed().collect(Collectors.toList());
    Collections.reverse(ordered);
    List<Integer> shuffled = new ArrayList<>(ordered);
    Collections.shuffle(shuffled);
    q = new MinMaxHeapDeque<>(shuffled, Collections.reverseOrder());
    List<Integer> res = new ArrayList<>();
    Integer e;
    while ((e = q.poll()) != null) {
      res.add(e);
    }
    assertTrue(q.isEmpty());
    assertEquals(ordered, res);
  }
  
  
  @SafeVarargs
  static <T extends Comparable<? super T>> MinMaxHeapDeque<T> ofItems(T... items) {
    final MinMaxHeapDeque<T> q = new MinMaxHeapDeque<>();
    for (T e : items) {
      q.offer(e);
    }
    return q;
  }
  
}
