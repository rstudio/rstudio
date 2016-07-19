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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

/**
 * An unbounded priority queue based on a priority heap.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/PriorityQueue.html">
 * the official Java API doc</a> for details.
 * A priority queue does not permit {@code null} elements.
 *
 * @param <E> element type.
 */
public class PriorityQueue<E> extends AbstractQueue<E> {

  private static final int DEFAULT_INITIAL_CAPACITY = 11;

  private static int getLeftChild(int node) {
    return 2 * node + 1;
  }

  private static int getParent(int node) {
    return (node - 1) / 2;
  }

  private static int getRightChild(int node) {
    return 2 * node + 2;
  }

  private static boolean isLeaf(int node, int size) {
    return node * 2 + 1 >= size;
  }

  private Comparator<? super E> cmp;

  /**
   * A heap held in an array. heap[0] is the root of the heap (the smallest
   * element), the subtrees of node i are 2*i+1 (left) and 2*i+2 (right). Node i
   * is a leaf node if 2*i>=n. Node i's parent, if i>0, is floor((i-1)/2).
   */
  private ArrayList<E> heap;

  public PriorityQueue() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  public PriorityQueue(Collection<? extends E> c) {
    this(c.size());
    addAll(c);
  }

  public PriorityQueue(int initialCapacity) {
    this(initialCapacity, null);
  }

  public PriorityQueue(int initialCapacity, Comparator<? super E> cmp) {
    heap = new ArrayList<>(initialCapacity);
    this.cmp = Comparators.nullToNaturalOrder(cmp);
  }

  public PriorityQueue(Comparator<? super E> comparator) {
    this(DEFAULT_INITIAL_CAPACITY, comparator);
  }

  @SuppressWarnings("unchecked")
  public PriorityQueue(PriorityQueue<? extends E> c) {
    this(c.size(), (Comparator<? super E>) c.comparator());
    addAll(c);
  }

  @SuppressWarnings("unchecked")
  public PriorityQueue(SortedSet<? extends E> c) {
    this(c.size(), (Comparator<? super E>) c.comparator());
    addAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkNotNull(c);
    checkArgument(c != this);

    int oldSize = heap.size();
    for (E e : c) {
      heap.add(checkCriticalNotNull(e));
    }
    if (oldSize != heap.size()) {
      makeHeap(0);
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    heap.clear();
  }

  public Comparator<? super E> comparator() {
    return Comparators.naturalOrderToNull(cmp);
  }

  @Override
  public boolean contains(Object o) {
    return indexOf(o) != -1;
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      int i = 0, last = -1;

      @Override
      public boolean hasNext() {
        return i < heap.size();
      }

      @Override
      public E next() {
        checkElement(hasNext());

        last = i++;
        return heap.get(last);
      }

      @Override
      public void remove() {
        checkState(last != -1);

        removeAtIndex(i = last);
        last = -1;
      }
    };
  }

  @Override
  public boolean offer(E e) {
    checkCriticalNotNull(e);
    int node = heap.size();
    heap.add(e);
    while (node > 0) {
      int childNode = node;
      node = getParent(node);
      if (cmp.compare(heap.get(node), e) <= 0) {
        // parent is smaller, so we have a valid heap
        heap.set(childNode, e);
        return true;
      }
      // exchange with parent and try again
      heap.set(childNode, heap.get(node));
    }
    heap.set(node, e);
    return true;
  }

  @Override
  public E peek() {
    return heap.isEmpty() ? null : heap.get(0);
  }

  @Override
  public E poll() {
    E value = peek();
    if (value != null) {
      removeAtIndex(0);
    }
    return value;
  }

  @Override
  public boolean remove(Object o) {
    int index = indexOf(o);
    if (index < 0) {
      return false;
    }
    removeAtIndex(index);
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (heap.removeAll(c)) {
      makeHeap(0);
      return true;
    }
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    if (heap.retainAll(c)) {
      makeHeap(0);
      return true;
    }
    return false;
  }

  @Override
  public int size() {
    return heap.size();
  }

  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.NONNULL);
  }

  @Override
  public Object[] toArray() {
    return heap.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return heap.toArray(a);
  }

  /**
   * Make the subtree rooted at <code>node</code> a valid heap. O(n) time
   *
   * @param node
   */
  private void makeHeap(int node) {
    if (isLeaf(node)) {
      // leaf node are automatically valid heaps
      return;
    }
    makeHeap(getLeftChild(node)); // make left subtree a heap
    // an interior node might not have a right child
    int rightChild = getRightChild(node);
    if (rightChild < heap.size()) {
      makeHeap(rightChild); // make right subtree a heap
    }
    mergeHeaps(node);
  }

  /**
   * Merge two subheaps into a single heap. O(log n) time
   * 
   * PRECONDITION: both children of <code>node</code> are heaps
   * 
   * @param node the parent of the two subtrees to merge
   */
  private void mergeHeaps(int node) {
    int heapSize = heap.size();
    E value = heap.get(node);
    while (!isLeaf(node, heapSize)) {
      int smallestChild = getSmallestChild(node, heapSize);
      if (cmp.compare(value, heap.get(smallestChild)) < 0) {
        // Current node is smaller than the smallest child, so we are done.
        break;
      }
      // Move the smallest child up and iterate using its old slot.
      heap.set(node, heap.get(smallestChild));
      node = smallestChild;
    }
    heap.set(node, value);
  }

  private int getSmallestChild(int node, int heapSize) {
    int smallestChild;
    int leftChild = getLeftChild(node); // start with left child
    int rightChild = leftChild + 1;
    smallestChild = leftChild;
    if ((rightChild < heapSize)
        && (cmp.compare(heap.get(rightChild), heap.get(leftChild)) < 0)) {
      // right child is smaller, go down that path
      smallestChild = rightChild;
    }
    return smallestChild;
  }

  private int indexOf(Object o) {
    return o == null ? -1 : heap.indexOf(o);
  }

  private boolean isLeaf(int node) {
    return isLeaf(node, heap.size());
  }

  /**
   * This method leaves the elements at up to i-1, inclusive, untouched.
   * This information is used by PriorityQueue iterator implementation.
   */
  private void removeAtIndex(int index) {
    // Remove the last element; put it in place of the really removed element.
    E lastValue = heap.remove(heap.size() - 1);
    // Unless the last element was actually the one we wanted.
    if (index < heap.size()) {
      // Move last element to the now-empty slot and reheap.
      heap.set(index, lastValue);
      mergeHeaps(index);
    }
  }
}
