package ru.salauyou.util.collect;

import java.util.AbstractQueue;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Skeleton implementation of `Deque`.
 * Like in `AbstractQueue`, null value returned by peek/poll
 * indicates that the deque is empty.
 * 
 * @author Aliaksandr Salauyou
 *
 * @param <E>
 */
public abstract class AbstractDeque<E> extends AbstractQueue<E> implements Deque<E> {

  protected AbstractDeque() {};
  
  
  @Override
  public void addFirst(E e) {
    if (!offerFirst(e)) {
      throw new IllegalStateException("Queue full");
    }
  }

  
  @Override
  public void push(E e) {
    addFirst(e);
  }
  
  
  @Override
  public void addLast(E e) {
    add(e);
  }

 
  @Override
  public boolean offerLast(E e) {
    return offer(e);
  }

  
  @Override
  public E removeFirst() {
    return returnOrThrowNse(poll());
  }

  
  @Override
  public E pop() {
    return returnOrThrowNse(poll());
  }
  
  
  @Override
  public E remove() {
    return returnOrThrowNse(poll());
  }
  
  
  @Override
  public E getFirst() {
    return returnOrThrowNse(peek());
  }
  
  
  @Override
  public E element() {
    return returnOrThrowNse(peek());
  }
  
  
  @Override
  public E removeLast() {
    return returnOrThrowNse(pollLast());
  }


  @Override
  public E getLast() {
    return returnOrThrowNse(peekLast());
  }
  
  
  @Override
  public E pollFirst() {
    return poll();
  }

  
  @Override
  public E peekFirst() {
    return peek();
  }
  
  
  static <T> T returnOrThrowNse(T x) {
    if (x != null)
      return x;
    else
      throw new NoSuchElementException();
  }
  
  
  @Override
  public boolean remove(Object o) {
    return removeUsingIterator(iterator(), o);
  }
  
  
  @Override
  public boolean removeFirstOccurrence(Object o) {
    return removeUsingIterator(iterator(), o);
  }

  
  @Override
  public boolean removeLastOccurrence(Object o) {
    return removeUsingIterator(descendingIterator(), o);
  }

  
  boolean removeUsingIterator(Iterator<E> it, Object o) {
    while (it.hasNext()) {
      if (Objects.equals(it.next(), o)) {
        it.remove();
        return true;
      }
    }
    return false;
  }
  
  
  @Override public abstract int size();
  
  @Override public abstract boolean offer(E e);
  
  @Override public abstract boolean offerFirst(E e);
  
  @Override public abstract E peek();
  
  @Override public abstract E peekLast();
  
  @Override public abstract E poll();

  @Override public abstract E pollLast();
  
  @Override public abstract Iterator<E> iterator();

  @Override public abstract Iterator<E> descendingIterator();

}
