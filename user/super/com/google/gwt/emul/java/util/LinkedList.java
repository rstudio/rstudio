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

import java.io.Serializable;

/**
 * Linked list implementation. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedList.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class LinkedList<E> extends AbstractSequentialList<E> implements
    List<E>, Queue<E>, Serializable {
  /*
   * This implementation uses a doubly-linked circular list with a header node.
   * 
   * TODO(jat): add more efficient subList implementation.
   */

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

    public void add(E o) {
      addBefore(o, currentNode);
      ++currentIndex;
      lastNode = null;
    }

    public boolean hasNext() {
      return currentNode != header;
    }

    public boolean hasPrevious() {
      return currentNode.prev != header;
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      lastNode = currentNode;
      currentNode = currentNode.next;
      ++currentIndex;
      return lastNode.value;
    }

    public int nextIndex() {
      return currentIndex;
    }

    public E previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      lastNode = currentNode = currentNode.prev;
      --currentIndex;
      return lastNode.value;
    }

    public int previousIndex() {
      return currentIndex - 1;
    }

    public void remove() {
      verifyCurrentElement();
      if (currentNode == lastNode) {
        // We just did a previous().
        currentNode = lastNode.next;
      } else {
        // We just did a next().
        --currentIndex;
      }
      lastNode.remove();
      lastNode = null;
      --size;
    }

    public void set(E o) {
      verifyCurrentElement();
      lastNode.value = o;
    }

    protected void verifyCurrentElement() {
      if (lastNode == null) {
        throw new IllegalStateException();
      }
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

    public Node() {
      next = prev = this;
    }

    public Node(E value) {
      this.value = value;
    }

    /**
     * Construct a node containing a value and add it before the specified node.
     * 
     * @param value
     * @param nextNode
     */
    public Node(E value, Node<E> nextNode) {
      this(value);
      this.next = nextNode;
      this.prev = nextNode.prev;
      nextNode.prev.next = this;
      nextNode.prev = this;
    }

    /**
     * Remove this node from any list it is in, leaving it with circular
     * references to itself.
     */
    public void remove() {
      this.next.prev = this.prev;
      this.prev.next = this.next;
      this.next = this.prev = this;
    }
  }

  /**
   * Ensures that RPC will consider type parameter E to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private E exposeElement;

  /**
   * Header node - header.next is the first element of the list, and header.prev
   * is the last element of the list. If the list is empty, the header node will
   * point to itself.
   */
  private Node<E> header;

  /**
   * Number of nodes currently present in the list.
   */
  private int size;

  public LinkedList() {
    clear();
  }

  public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
  }

  @Override
  public boolean add(E o) {
    addLast(o);
    return true;
  }

  public void addFirst(E o) {
    new Node<E>(o, header.next);
    ++size;
  }

  public void addLast(E o) {
    new Node<E>(o, header);
    ++size;
  }

  @Override
  public void clear() {
    header = new Node<E>();
    size = 0;
  }

  public E element() {
    return getFirst();
  }

  public E getFirst() {
    throwEmptyException();
    return header.next.value;
  }

  public E getLast() {
    throwEmptyException();
    return header.prev.value;
  }

  @Override
  public ListIterator<E> listIterator(final int index) {
    if (index < 0 || index > size) {
      indexOutOfBounds(index, size);
    }

    Node<E> node;
    // start from the nearest end of the list
    if (index >= size >> 1) {
      node = header;
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

  public boolean offer(E o) {
    return add(o);
  }

  public E peek() {
    if (size == 0) {
      return null;
    } else {
      return getFirst();
    }
  }

  public E poll() {
    if (size == 0) {
      return null;
    } else {
      return removeFirst();
    }
  }

  public E remove() {
    return removeFirst();
  }

  public E removeFirst() {
    throwEmptyException();
    --size;
    Node<E> node = header.next;
    node.remove();
    return node.value;
  }

  public E removeLast() {
    throwEmptyException();
    --size;
    Node<E> node = header.prev;
    node.remove();
    return node.value;
  }

  @Override
  public int size() {
    return size;
  }

  private void addBefore(E o, Node<E> target) {
    new Node<E>(o, target);
    ++size;
  }

  /**
   * Throw an exception if the list is empty.
   */
  private void throwEmptyException() {
    if (size == 0) {
      throw new NoSuchElementException();
    }
  }
}
