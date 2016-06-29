package ru.salauyou.util.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.salauyou.util.collect.SimpleCasLinkedList;


public class TestSimpleCasLinkedList {

    
    final Collection<String> c = new SimpleCasLinkedList<>();
    
    @Before
    public void addItems() {
        c.add("A");
        c.add("B");
        c.addAll(Arrays.asList("C", "D", "E"));
    }
    
    
    @Test
    public void testSizeContains() {
        assertFalse(c.isEmpty());
        assertEquals(5, c.size());
        assertTrue(c.contains("A"));
        assertTrue(c.contains("C"));
        assertTrue(c.contains("E"));
        assertTrue(c.containsAll(Arrays.asList("B", "D", "A")));
        
        assertFalse(c.contains("AA"));
        assertFalse(c.contains(null));
        assertFalse(c.contains("F"));
    }
    
    
    @Test
    public void testIterator() {
        Set<String> copy = new HashSet<>();
        for (String s : c)
            copy.add(s);
        assertTrue(copy.containsAll(Arrays.asList("A", "B", "C", "D", "E")));
        assertEquals(5, copy.size());
    }
    
    
    @Test
    public void testToArray() {
        assertEquals(5, c.toArray().length);
    }
    
}
