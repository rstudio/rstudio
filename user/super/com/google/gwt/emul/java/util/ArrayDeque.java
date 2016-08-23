/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.util;

import static javaemul.internal.InternalPreconditions.checkCriticalElement;
import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkState;

import javaemul.internal.ArrayHelper;

/**
 * A {@link Deque} based on circular buffer that is implemented with an array and head/tail
 * pointers. Array deques have no capacity restrictions; they grow as necessary to support usage.
 * Null elements are prohibited. This class is likely to be faster than {@link Stack}
 * when used as a stack, and faster than {@link LinkedList} when used as a queue.
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ArrayDeque.html">ArrayDeque</a>
 *
 * @param <E> the element type.
 */
public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable {

  private final class IteratorImpl implements Iterator<E> {
    /**
     * Index of element to be returned by subsequent call to next.
     */
    private int currentIndex = head;

    /**
     * Tail recorded at construction (also in remove), to stop
     * iterator and also to check for comodification.
     */
    private int fence = tail;

    /**
     * Index of element returned by most recent call to next.
     * Reset to -1 if element is deleted by a call to remove.
     */
    private int lastIndex = -1;

    @Override
    public boolean hasNext() {
      return currentIndex != fence;
    }

    @Override
    public E next() {
      checkCriticalElement(hasNext());

      E e = array[currentIndex];
      // OpenJDK ArrayDeque doesn't catch all possible comodifications,
      // but does catch the ones that corrupt traversal
      checkConcurrentModification(fence == tail && e != null);
      lastIndex = currentIndex;
      currentIndex = (currentIndex + 1) & (array.length - 1);
      return e;
    }

    @Override
    public void remove() {
      checkState(lastIndex >= 0);

      if (removeAtIndex(lastIndex) < 0) {
        // if left-shifted, undo increment in next()
        currentIndex = (currentIndex - 1) & (array.length - 1);
        fence = tail;
      }
      lastIndex = -1;
    }
  }

  private final class DescendingIteratorImpl implements Iterator<E> {
    private int currentIndex = tail;
    private int fence = head;
    private int lastIndex = -1;

    @Override
    public boolean hasNext() {
      return currentIndex != fence;
    }

    @Override
    public E next() {
      checkCriticalElement(hasNext());

      currentIndex = (currentIndex - 1) & (array.length - 1);
      E e = array[currentIndex];
      checkConcurrentModification(fence == head && e != null);
      lastIndex = currentIndex;
      return e;
    }

    @Override
    public void remove() {
      checkState(lastIndex >= 0);

      if (removeAtIndex(lastIndex) > 0) {
        // if right-shifted, undo decrement in next()
        currentIndex = (currentIndex + 1) & (array.length - 1);
        fence = head;
      }
      lastIndex = -1;
    }
  }

  /**
   * The minimum capacity that we'll use for a newly created deque.
   * Must be a power of 2.
   */
  private static final int MIN_INITIAL_CAPACITY = 8;

  private static void checkConcurrentModification(boolean expression) {
    if (!expression) {
      throw new ConcurrentModificationException();
    }
  }

  /**
   * Returns best power-of-two array length to hold the given number of elements.
   *
   * @param numElements the number of elements to hold
   */
  @SuppressWarnings("unchecked")
  private static int nextArrayLength(int numElements) {
    return nextPowerOfTwo(Math.max(MIN_INITIAL_CAPACITY, numElements));
  }

  /**
   * Returns a number that is greater than {@code num} and is a power of two.
   * If passed {@code num} is not positive integer or next power of two overflows then
   * returned value is non-positive.
   * E.g., if num == 32, returns 64. if num == 31, returns 32.
   *
   * @param num positive integer.
   */
  private static int nextPowerOfTwo(int num) {
    return Integer.highestOneBit(num) << 1;
  }

  /**
   * This field holds a JavaScript array.
   */
  @SuppressWarnings("unchecked")
  private E[] array = (E[]) new Object[MIN_INITIAL_CAPACITY];

  /**
   * The index of the element at the head of the deque (which is the
   * element that would be removed by remove() or pop()); or an
   * arbitrary number equal to tail if the deque is empty.
   */
  private int head;

  /**
   * The index at which the next element would be added to the tail
   * of the deque (via addLast(E), add(E), or push(E)).
   */
  private int tail;

  public ArrayDeque() {
  }

  public ArrayDeque(Collection<? extends E> c) {
    this(c.size());
    addAll(c);
  }

  public ArrayDeque(int numElements) {
    ArrayHelper.setLength(array, nextArrayLength(numElements));
  }

  @Override
  public boolean add(E e) {
    addLast(e);
    return true;
  }

  @Override
  public void addFirst(E e) {
    checkCriticalNotNull(e);

    head = (head - 1) & (array.length - 1);
    array[head] = e;
    ensureCapacity();
  }

  @Override
  public void addLast(E e) {
    checkCriticalNotNull(e);

    array[tail] = e;
    tail = (tail + 1) & (array.length - 1);
    ensureCapacity();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    if (head == tail) {
      return;
    }

    array = (E[]) new Object[MIN_INITIAL_CAPACITY];
    head = 0;
    tail = 0;
  }

  public Object clone() {
    return new ArrayDeque<>(this);
  }

  @Override
  public boolean contains(Object o) {
    return contains(iterator(), o);
  }

  @Override
  public Iterator<E> descendingIterator() {
    return new DescendingIteratorImpl();
  }

  @Override
  public E element() {
    return getFirst();
  }

  @Override
  public E getFirst() {
    E e = peekFirstElement();
    checkElement(e != null);
    return e;
  }

  @Override
  public E getLast() {
    E e = peekLastElement();
    checkElement(e != null);
    return e;
  }

  @Override
  public boolean isEmpty() {
    return head == tail;
  }

  @Override
  public Iterator<E> iterator() {
    return new IteratorImpl();
  }

  @Override
  public boolean offer(E e) {
    return offerLast(e);
  }

  @Override
  public boolean offerFirst(E e) {
    addFirst(e);
    return true;
  }

  @Override
  public boolean offerLast(E e) {
    addLast(e);
    return true;
  }

  @Override
  public E peek() {
    return peekFirst();
  }

  @Override
  public E peekFirst() {
    return peekFirstElement();
  }

  @Override
  public E peekLast() {
    return peekLastElement();
  }

  @Override
  public E poll() {
    return pollFirst();
  }

  @Override
  public E pollFirst() {
    E e = peekFirstElement();
    if (e == null) {
      return null;
    }
    array[head] = null;
    head = (head + 1) & (array.length - 1);
    return e;
  }

  @Override
  public E pollLast() {
    E e = peekLastElement();
    if (e == null) {
      return null;
    }
    tail = (tail - 1) & (array.length - 1);
    array[tail] = null;
    return e;
  }

  @Override
  public E pop() {
    return removeFirst();
  }

  @Override
  public void push(E e) {
    addFirst(e);
  }

  @Override
  public E remove() {
    return removeFirst();
  }

  @Override
  public boolean remove(Object o) {
    return removeFirstOccurrence(o);
  }

  @Override
  public E removeFirst() {
    E e = pollFirst();
    checkElement(e != null);
    return e;
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    return remove(iterator(), o);
  }

  @Override
  public E removeLast() {
    E e = pollLast();
    checkElement(e != null);
    return e;
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    return remove(descendingIterator(), o);
  }

  @Override
  public int size() {
    return (tail - head) & (array.length - 1);
  }

  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.NONNULL | Spliterator.ORDERED);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] out) {
    int size = size();
    if (out.length < size) {
      out = ArrayHelper.createFrom(out, size);
    }
    copyElements(out, size);
    if (out.length > size) {
      out[size] = null;
    }
    return out;
  }

  private boolean contains(Iterator<E> it, Object o) {
    if (o == null) {
      return false;
    }

    while (it.hasNext()) {
      if (o.equals(it.next())) {
        return true;
      }
    }
    return false;
  }

  private boolean remove(Iterator<E> it, Object o) {
    if (contains(it, o)) {
      it.remove();
      return true;
    }
    return false;
  }

  private E peekFirstElement() {
    return array[head];
  }

  private E peekLastElement() {
    return array[(tail - 1) & (array.length - 1)];
  }

  /**
   * Copies {@code count} ArrayDeque's elements to {@code dest} array.
   * The method is safe to use when ArrayDeque's array has been rolled over,
   * i.e. {@code head == tail}.
   * It is assumed that {@code count < size()}.
   */
  private void copyElements(Object[] dest, int count) {
    final int mask = array.length - 1;
    for (int i = head, dstIdx = 0; dstIdx < count; i = (i + 1) & mask, ++dstIdx) {
      dest[dstIdx] = array[i];
    }
  }

  /**
   * Increase the capacity of this deque when full, i.e.,
   * when head and tail have wrapped around to become equal.
   */
  private void ensureCapacity() {
    if (head != tail) {
      return;
    }

    int numElements = array.length;
    int newLength = nextArrayLength(numElements);
    if (head != 0) {
      E[] newArray = ArrayHelper.createFrom(array, newLength);
      copyElements(newArray, numElements);
      array = newArray;
      head = 0;
    } else {
      ArrayHelper.setLength(array, newLength);
    }
    tail = numElements;
  }

  /**
   * Removes the element at the specified position in the elements array,
   * adjusting head and tail as necessary. This results in motion of
   * elements backwards or forwards in the array.
   *
   * @return -1 if elements moved backwards (left-shifted); 1 if forwards (right-shifted).
   */
  private int removeAtIndex(int i) {
    final int mask = array.length - 1;
    int headDistance = (i - head) & mask;
    int tailDistance = (tail - i) & mask;
    int size = (tail - head) & mask;

    checkConcurrentModification(headDistance < size);
    if (headDistance >= tailDistance) {
      shiftLeftAtIndex(i);
      return -1;
    } else {
      shiftRightAtIndex(i);
      return 1;
    }
  }

  private void shiftLeftAtIndex(int i) {
    final int mask = array.length - 1;
    tail = (tail - 1) & mask;
    while (i != tail) {
      int nextOffset = (i + 1) & mask;
      array[i] = array[nextOffset];
      i = nextOffset;
    }
    array[tail] = null;
  }

  private void shiftRightAtIndex(int i) {
    final int mask = array.length - 1;
    while (i != head) {
      int prevOffset = (i - 1) & mask;
      array[i] = array[prevOffset];
      i = prevOffset;
    }
    array[head] = null;
    head = (head + 1) & mask;
  }
}
