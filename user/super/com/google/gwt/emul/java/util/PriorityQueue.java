/*
 * Copyright 2007 Google Inc.
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

/**
 * An unbounded priority queue based on a priority heap. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/PriorityQueue.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class PriorityQueue<E> extends AbstractQueue<E> {

  /**
   * A heap held in an array. heap[0] is the root of the heap (the smallest
   * element), the subtrees of node i are 2*i+1 (left) and 2*i+2 (right). Node i
   * is a leaf node if 2*i>=n. Node i's parent, if i>0, is floor((i-1)/2).
   */
  private ArrayList<E> heap;
  private Comparator<? super E> cmp;

  public PriorityQueue() {
    this(11);
  }

  public PriorityQueue(Collection<? extends E> c) {
    this(c.size());
    addAll(c);
  }

  public PriorityQueue(int initialCapacity) {
    this(initialCapacity, Comparators.natural());
  }

  public PriorityQueue(int initialCapacity, Comparator<? super E> cmp) {
    heap = new ArrayList<E>(initialCapacity);
    this.cmp = cmp;
  }

  public PriorityQueue(PriorityQueue<? extends E> c) {
    // TODO(jat): better solution
    this(c.size());
    addAll(c);
  }

  public PriorityQueue(SortedSet<? extends E> c) {
    // TODO(jat): better solution
    this(c.size());
    addAll(c);
  }

  @Override
  public Iterator<E> iterator() {
    // TODO(jat): perhaps a better way to do this
    return Collections.unmodifiableList(heap).iterator();
  }

  @Override
  public boolean offer(E e) {
    int node = heap.size();
    heap.add(e);
    while (node > 0) {
      int childNode = node;
      node = (node - 1) >> 1; // get parent of current node
      if (cmp.compare(heap.get(node), heap.get(childNode)) <= 0) {
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
    if (heap.size() == 0) {
      return null;
    }
    return heap.get(0);
  }

  @Override
  public E poll() {
    if (heap.size() == 0) {
      return null;
    }
    E value = heap.get(0);
    heap.set(0, heap.remove(heap.size() - 1)); // move last element to root
    makeHeap(0); // make it back into a heap
    return value;
  }

  @Override
  public int size() {
    return heap.size();
  }

  /**
   * Make the subtree rooted at <code>node</code> a valid heap. O(n) time
   * 
   * @param node
   */
  protected void makeHeap(int node) {
    int n = heap.size();
    if (2 * node >= n) {
      // leaf node, no work to do
      return;
    }
    makeHeap(2 * node + 1); // make left subtree a heap
    makeHeap(2 * node + 2); // make right subtree a heap
    mergeHeaps(node);
  }

  /**
   * Merge two subheaps into a single heap. O(log n) time
   * 
   * PRECONDITION: both children of <code>node</code> are heaps
   * 
   * @param node the parent of the two subtrees to merge
   */
  protected void mergeHeaps(int node) {
    int n = heap.size();
    E value = heap.get(node);
    while (node * 2 < n) {
      int childNode = 2 * node + 1; // start with left child
      if ((childNode + 1 < n)
          && (cmp.compare(heap.get(childNode + 1), heap.get(childNode)) < 0)) {
        childNode++; // right child is smaller, go down that path
      }
      heap.set(node, heap.get(childNode));
      node = childNode;
    }
    heap.set(node, value);
  }

}
