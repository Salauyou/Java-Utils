package ru.salauyou.util.collect;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;



public class TestIndexedSkipList {

  public List<Integer> source() {
    List<Integer> values = new ArrayList<>();
    if (values.isEmpty()) {
      for (int v = 0; v < 50; v++) {
        values.add(v);
        values.add(v);
      }
      Collections.shuffle(values, new Random(1));
    }
    return values;
  }


  @Test
  public void testNew() {
    List<Integer> list = new IndexedSkipList<>();
    List<Integer> listC = new IndexedSkipList<>((i1, i2) -> i2.compareTo(i1));
    assertNotNull(list);
    assertNotNull(listC);
  }


  @Test
  public void testAddRemove() {
    List<Integer> list = new IndexedSkipList<>();
    Comparator<Integer> cmp = Comparator.naturalOrder();
    for (int v : source()) {
      list.add(v);
      assertSorted(list, cmp);
    }
    assertEquals(100, list.size());
    
    assertEquals(-1, list.indexOf(-1));
    assertEquals(-1, list.lastIndexOf(-1));
    assertEquals(-1, list.indexOf(-10));
    assertEquals(-1, list.lastIndexOf(-10));
    assertEquals(0, list.indexOf(0));
    assertEquals(1, list.lastIndexOf(0));
    assertEquals(20, list.indexOf(10));
    assertEquals(21, list.lastIndexOf(10));
    assertEquals(-101, list.indexOf(100));
    assertEquals(-101, list.lastIndexOf(100));
    assertEquals(-101, list.indexOf(1010));
    assertEquals(-101, list.lastIndexOf(1010));
    
    assertTrue(list.remove((Integer) 0));
    assertTrue(list.remove((Integer) 0));
    assertEquals(98, list.size());
    assertEquals(-1, list.indexOf(0));
    assertEquals(-1, list.lastIndexOf(0));
    assertEquals(0, list.indexOf(1));
    assertEquals(1, list.lastIndexOf(1));
    
    assertFalse(list.remove((Integer) 99));
    
    List<Integer> listToRemove = new ArrayList<>();
    for (int i = 1; i <= 95; i++) {
      listToRemove.add(i);
    }
    Collections.shuffle(listToRemove);
    for (Integer i : listToRemove) {
      list.remove(i);
      assertSorted(list, cmp);
    }
    assertEquals(49, list.size());
  }

  
  static <T> void assertSorted(List<T> list, Comparator<? super T> cmp) {
    T pred = null;
    for (T t : list) {
      if (pred != null) {
        assertTrue(cmp.compare(pred, t) <= 0);
      }
      pred = t;
    }
  }
  
}
