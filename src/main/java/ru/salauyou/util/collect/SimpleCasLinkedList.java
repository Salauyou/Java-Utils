package ru.salauyou.util.collect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
 * A simple implementation of thread-safe lock-free linked list.
 * Remove operations are not supported. Iterator returns elements
 * in reversed order (LIFO).
 * 
 * @author Salauyou
 *
 * @param <E>
 */
public class SimpleCasLinkedList<E> implements Collection<E> {
    
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<SimpleCasLinkedList, Node> tailUpdater 
        = AtomicReferenceFieldUpdater.newUpdater(SimpleCasLinkedList.class, Node.class, "tail");
    
    volatile Node<E> tail = null;
    
    
    @Override
    public int size() {
        // To reduce memory consumption we don't hold 
        // internal property for size, so `size()` 
        // becomes O(n)
        int size = 0;
        for (Node<E> n = tail; n != null; n = n.prev)
            size++;
        return size;
    }

    
    @Override
    public boolean isEmpty() {
        return tail == null;
    }

    
    @Override
    public boolean contains(Object o) {
        Node<E> n = tail;
        if (n == null)
            return false;
        for (; n != null; n = n.prev) {
            if (Objects.equals(o, n.value))
                return true;
        }
        return false;
    }

    
    @Override
    public Iterator<E> iterator() {
        
        final Node<E> t = tail;
        if (t == null)
            return Collections.emptyIterator();

        return new Iterator<E>() {
            Node<E> n = t;
            
            @Override
            public boolean hasNext() {
                return n != null;
            }

            @Override
            public E next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                E res = n.value;
                n = n.prev;
                return res;
            }
        };
    }

    
    @Override
    public Object[] toArray() {
        return toList().toArray();
    }

    
    @Override
    public <T> T[] toArray(T[] a) {
        return toList().toArray(a);
    }

    
    List<E> toList() {
        Node<E> n = tail;
        if (n == null)
            return Collections.emptyList();
        final List<E> res = new ArrayList<>();
        for (; n != null; n = n.prev)
            res.add(n.value);
        return res;
    }
    
    
    @Override
    public boolean add(E e) {
        for(;;) {
            final Node<E> t = tail;
            final Node<E> n = new Node<>(e, t);
            if (tailUpdater.compareAndSet(this, t, n))
                return true;
        }
    }

    
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o))
                return false;
        }
        return true;
    }

    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c.isEmpty())
            return false;
        for (E e : c)
            add(e);
        return true;
    }
    
    
    
    // ----- remove operations aren't supported ----- //
    
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
    
    
    
    // ------------- Node<E> --------------- //
    
    final static class Node<E> {
        
        final E value;
        final Node<E> prev;
        
        Node(E value, Node<E> prev) {
            this.value = value;
            this.prev = prev;
        }    
    }
    
}
