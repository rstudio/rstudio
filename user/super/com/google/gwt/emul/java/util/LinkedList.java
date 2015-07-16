/*
 * Copyright 2008 Google Inc.
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

import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkPositionIndex;
import static javaemul.internal.InternalPreconditions.checkState;

import java.io.Serializable;

/**
 * Linked list implementation. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedList.html">[Sun
 * docs]</a>
 *
 * @param <E> element type.
 */
public class LinkedList<E> extends AbstractSequentialList<E> implements
    Cloneable, List<E>, Deque<E>, Serializable {
  /*
   * This implementation uses a doubly-linked list with a header/tail node.
   *
   * TODO(jat): add more efficient subList implementation.
   */

  private final class DescendingIteratorImpl implements Iterator<E> {
    private final ListIterator<E> itr = new ListIteratorImpl(size, tail);

    @Override
    public boolean hasNext() {
      return itr.hasPrevious();
    }

    @Override
    public E next() {
      return itr.previous();
    }

    @Override
    public void remove() {
      itr.remove();
    }
  }

  /**
   * Implementation of ListIterator for linked lists.
   */
  private final class ListIteratorImpl implements ListIterator<E> {

    /**
     * The index to the current position.
     */
    protected int currentIndex;

    /**
     * Current node, to be returned from next.
     */
    protected Node<E> currentNode;

    /**
     * The last node returned from next/previous, or null if deleted or never
     * called.
     */
    protected Node<E> lastNode = null;

    /**
     * @param index from the beginning of the list (0 = first node)
     * @param startNode the initial current node
     */
    public ListIteratorImpl(int index, Node<E> startNode) {
      currentNode = startNode;
      currentIndex = index;
    }

    @Override
    public void add(E o) {
      addNode(o, currentNode.prev, currentNode);
      ++currentIndex;
      lastNode = null;
    }

    @Override
    public boolean hasNext() {
      return currentNode != tail;
    }

    @Override
    public boolean hasPrevious() {
      return currentNode.prev != header;
    }

    @Override
    public E next() {
      checkElement(hasNext());

      lastNode = currentNode;
      currentNode = currentNode.next;
      ++currentIndex;
      return lastNode.value;
    }

    @Override
    public int nextIndex() {
      return currentIndex;
    }

    @Override
    public E previous() {
      checkElement(hasPrevious());

      lastNode = currentNode = currentNode.prev;
      --currentIndex;
      return lastNode.value;
    }

    @Override
    public int previousIndex() {
      return currentIndex - 1;
    }

    @Override
    public void remove() {
      checkState(lastNode != null);

      Node<E> nextNode = lastNode.next;
      removeNode(lastNode);
      if (currentNode == lastNode) {
        // We just did a previous().
        currentNode = nextNode;
      } else {
        // We just did a next().
        --currentIndex;
      }
      lastNode = null;
    }

    @Override
    public void set(E o) {
      checkState(lastNode != null);

      lastNode.value = o;
    }
  }

  /**
   * Internal class representing a doubly-linked list node.
   *
   * @param <E> element type
   */
  private static class Node<E> {
    public Node<E> next;
    public Node<E> prev;
    public E value;
  }

  /**
   * Ensures that RPC will consider type parameter E to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private E exposeElement;

  /**
   * Header node - header.next is the first element of the list.
   */
  private final Node<E> header = new Node<E>();

  /**
   * Tail node - tail.prev is the last element of the list.
   */
  private final Node<E> tail = new Node<E>();

  /**
   * Number of nodes currently present in the list.
   */
  private int size;

  public LinkedList() {
    reset();
  }

  public LinkedList(Collection<? extends E> c) {
    reset();
    addAll(c);
  }

  @Override
  public boolean add(E o) {
    addLast(o);
    return true;
  }

  @Override
  public void addFirst(E o) {
    addNode(o, header, header.next);
  }

  @Override
  public void addLast(E o) {
    addNode(o, tail.prev, tail);
  }

  @Override
  public void clear() {
    reset();
  }

  private void reset() {
    header.next = tail;
    tail.prev = header;
    header.prev = tail.next = null;
    size = 0;
  }

  public Object clone() {
    return new LinkedList<E>(this);
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
    checkElement(size != 0);

    return header.next.value;
  }

  @Override
  public E getLast() {
    checkElement(size != 0);

    return tail.prev.value;
  }

  @Override
  public ListIterator<E> listIterator(final int index) {
    checkPositionIndex(index, size);

    Node<E> node;
    // start from the nearest end of the list
    if (index >= size >> 1) {
      node = tail;
      for (int i = size; i > index; --i) {
        node = node.prev;
      }
    } else {
      node = header.next;
      for (int i = 0; i < index; ++i) {
        node = node.next;
      }
    }

    return new ListIteratorImpl(index, node);
  }

  @Override
  public boolean offer(E o) {
    return offerLast(o);
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
    return size == 0 ? null : getFirst();
  }

  @Override
  public E peekLast() {
    return size == 0 ? null : getLast();
  }

  @Override
  public E poll() {
    return pollFirst();
  }

  @Override
  public E pollFirst() {
    return size == 0 ? null : removeFirst();
  }

  @Override
  public E pollLast() {
    return size == 0 ? null : removeLast();
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
  public E removeFirst() {
    checkElement(size != 0);

    return removeNode(header.next);
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    return remove(o);
  }

  @Override
  public E removeLast() {
    checkElement(size != 0);

    return removeNode(tail.prev);
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    for (Node<E> e = tail.prev; e != header; e = e.prev) {
      if (Objects.equals(e.value, o)) {
        removeNode(e);
        return true;
      }
    }
    return false;
  }

  @Override
  public int size() {
    return size;
  }

  private void addNode(E o, Node<E> prev, Node<E> next) {
    Node<E> node = new Node<E>();
    node.value = o;
    node.prev = prev;
    node.next = next;
    next.prev = prev.next = node;
    ++size;
  }

  private E removeNode(Node<E> node) {
    E oldValue = node.value;
    node.next.prev = node.prev;
    node.prev.next = node.next;
    node.next = node.prev = null;
    node.value = null;
    --size;
    return oldValue;
  }
}
