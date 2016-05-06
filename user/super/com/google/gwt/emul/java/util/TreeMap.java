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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.io.Serializable;

/**
 * Implements a TreeMap using a red-black tree. This guarantees O(log n)
 * performance on lookups, inserts, and deletes while maintaining linear
 * in-order traversal time. Null keys and values are fully supported if the
 * comparator supports them (the default comparator does not).
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TreeMap<K, V> extends AbstractNavigableMap<K, V> implements Serializable {
  /*
   * Implementation derived from public domain C implementation as of 5
   * September 2007 at:
   * http://eternallyconfuzzled.com/tuts/datastructures/jsw_tut_rbtree.aspx
   * written by Julienne Walker.
   *
   * This version does not require a parent pointer kept in each node.
   */

  /**
   * Iterator for <code>descendingMap().entrySet()</code>.
   */
  private final class DescendingEntryIterator implements Iterator<Entry<K, V>> {
    private final ListIterator<Entry<K, V>> iter;
    private Entry<K, V> last;

    /**
     * Constructor for <code>DescendingEntryIterator</code>.
     */
    public DescendingEntryIterator() {
      this(SubMapType.All, null, false, null, false);
    }

    /**
     * Create an iterator which may return only a restricted range.
     *
     * @param fromKey the first key to return in the iterator.
     * @param toKey the upper bound of keys to return.
     */
    public DescendingEntryIterator(SubMapType type,
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      List<Entry<K, V>> list = new ArrayList<Entry<K, V>>();
      inOrderAdd(list, type, TreeMap.this.root,
          fromKey, fromInclusive, toKey, toInclusive);
      this.iter = list.listIterator(list.size());
    }

    @Override
    public boolean hasNext() {
      return iter.hasPrevious();
    }

    @Override
    public Entry<K, V> next() {
      return last = iter.previous();
    }

    @Override
    public void remove() {
      iter.remove();
      removeEntry(last);
      last = null;
    }
  }

  /**
   * Iterator for <code>EntrySet</code>.
   */
  private final class EntryIterator implements Iterator<Entry<K, V>> {
    private final ListIterator<Entry<K, V>> iter;
    private Entry<K, V> last;

    /**
     * Constructor for <code>EntrySetIterator</code>.
     */
    public EntryIterator() {
      this(SubMapType.All, null, false, null, false);
    }

    /**
     * Create an iterator which may return only a restricted range.
     *
     * @param fromKey the first key to return in the iterator.
     * @param toKey the upper bound of keys to return.
     */
    public EntryIterator(SubMapType type,
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      List<Entry<K, V>> list = new ArrayList<Entry<K, V>>();
      inOrderAdd(list, type, TreeMap.this.root,
          fromKey, fromInclusive, toKey, toInclusive);
      this.iter = list.listIterator();
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      return last = iter.next();
    }

    @Override
    public void remove() {
      iter.remove();
      removeEntry(last);
      last = null;
    }
  }

  private final class EntrySet extends AbstractNavigableMap.EntrySet {
    @Override
    public void clear() {
      TreeMap.this.clear();
    }
  }

  /**
   * Tree node.
   *
   * @param <K> key type
   * @param <V> value type
   */
  private static class Node<K, V> extends SimpleEntry<K, V> {
    /*
     * The children are kept in an array to minimize the normal duplication of
     * code.
     */
    @SuppressWarnings("unchecked")
    protected final Node<K, V>[] child = new Node[2];
    protected boolean isRed;

    /**
     * Create a red node.
     *
     * @param key
     * @param value
     */
    public Node(K key, V value) {
      this(key, value, true);
    }

    /**
     * Create a node of the specified color.
     *
     * @param key
     * @param value
     * @param isRed true if this should be a red node, false for black
     */
    public Node(K key, V value, boolean isRed) {
      super(key, value);
      this.isRed = isRed;
    }
  }

  /**
   * A state object which is passed down the tree for both insert and remove.
   * All uses make use of the done flag to indicate when no further rebalancing
   * of the tree is required. Remove methods use the found flag to indicate when
   * the desired key has been found. value is used both to return the value of a
   * removed node as well as to pass in a value which must match (used for
   * entrySet().remove(entry)), and the matchValue flag is used to request this
   * behavior.
   *
   * @param <V> value type
   */
  private static class State<V> {
    public boolean done;
    public boolean found;
    public boolean matchValue;
    public V value;

    @Override
    public String toString() {
      return "State: mv=" + matchValue + " value=" + value + " done=" + done + " found=" + found;
    }
  }

  private class SubMap extends AbstractNavigableMap<K, V> {
    private final boolean fromInclusive;

    // valid only if type is Range or Tail
    private final K fromKey;

    private final boolean toInclusive;

    // valid only if type is Range or Head
    private final K toKey;

    private final SubMapType type;

    SubMap(SubMapType type,
        K fromKey, boolean fromInclusive,
        K toKey, boolean toInclusive) {
      switch (type) {
        case Range:
          if (cmp.compare(toKey, fromKey) < 0) {
            throw new IllegalArgumentException("subMap: " + toKey
                + " less than " + fromKey);
          }
          break;
        case Head:
          // check key for compatibility with comparator
          cmp.compare(toKey, toKey);
          break;
        case Tail:
          // check key for compatibility with comparator
          cmp.compare(fromKey, fromKey);
          break;
        case All:
          // no checks are needed
          break;
      }
      this.type = type;
      this.fromKey = fromKey;
      this.fromInclusive = fromInclusive;
      this.toKey = toKey;
      this.toInclusive = toInclusive;
    }

    @Override
    public Comparator<? super K> comparator() {
      return TreeMap.this.comparator();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new SubMap.EntrySet();
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean toInclusive) {
      if (type.toKeyValid() && cmp.compare(toKey, this.toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + toKey +
            " greater than " + this.toKey);
      }
      if (type.fromKeyValid()) {
        return TreeMap.this.subMap(fromKey, fromInclusive, toKey, toInclusive);
      } else {
        return TreeMap.this.headMap(toKey, toInclusive);
      }
    }

    @Override
    public V put(K key, V value) {
      if (!inRange(key)) {
        throw new IllegalArgumentException(key + " outside the range "
            + fromKey + " to " + toKey);
      }
      return TreeMap.this.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object k) {
      K key = (K) k;
      if (!inRange(key)) {
        return null;
      }
      return TreeMap.this.remove(key);
    }

    @Override
    public int size() {
      if (getFirstEntry() == null) {
        return 0;
      }

      // TODO(jat): more efficient way to do this?
      int count = 0;
      for (Iterator<Entry<K, V>> it = entryIterator(); it.hasNext(); it.next()) {
        count++;
      }
      return count;
    }

    @Override
    public NavigableMap<K, V> subMap(K newFromKey, boolean newFromInclusive,
        K newToKey, boolean newToInclusive) {
      if (type.fromKeyValid() && cmp.compare(newFromKey, fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + newFromKey +
            " less than " + fromKey);
      }
      if (type.toKeyValid() && cmp.compare(newToKey, toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + newToKey +
            " greater than " + toKey);
      }
      return TreeMap.this.subMap(newFromKey, newFromInclusive, newToKey, newToInclusive);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean fromInclusive) {
      if (type.fromKeyValid() && cmp.compare(fromKey, this.fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + fromKey +
            " less than " + this.fromKey);
      }
      if (type.toKeyValid()) {
        return TreeMap.this.subMap(fromKey, fromInclusive, toKey, toInclusive);
      } else {
        return TreeMap.this.tailMap(fromKey, fromInclusive);
      }
    }

    @Override
    Iterator<Entry<K, V>> descendingEntryIterator() {
      return new DescendingEntryIterator(type, fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return new EntryIterator(type, fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    Entry<K, V> getEntry(K key) {
      return guardInRange(TreeMap.this.getEntry(key));
    }

    @Override
    Entry<K, V> getFirstEntry() {
      Entry<K, V> entry;
      if (type.fromKeyValid()) {
        if (fromInclusive) {
          entry = TreeMap.this.getCeilingEntry(fromKey);
        } else {
          entry = TreeMap.this.getHigherEntry(fromKey);
        }
      } else {
        entry = TreeMap.this.getFirstEntry();
      }
      // The map is empty if the first key after fromKey is out of range.
      return guardInRange(entry);
    }

    @Override
    Entry<K, V> getLastEntry() {
      Entry<K, V> entry;
      if (type.toKeyValid()) {
        if (toInclusive) {
          entry = TreeMap.this.getFloorEntry(toKey);
        } else {
          entry = TreeMap.this.getLowerEntry(toKey);
        }
      } else {
        entry = TreeMap.this.getLastEntry();
      }
      // The map is empty if the last key before toKey is out of range.
      return guardInRange(entry);
    }

    @Override
    Entry<K, V> getCeilingEntry(K key) {
      return guardInRange(TreeMap.this.getCeilingEntry(key));
    }

    @Override
    Entry<K, V> getFloorEntry(K key) {
      return guardInRange(TreeMap.this.getFloorEntry(key));
    }

    @Override
    Entry<K, V> getHigherEntry(K key) {
      return guardInRange(TreeMap.this.getHigherEntry(key));
    }

    @Override
    Entry<K, V> getLowerEntry(K key) {
      return guardInRange(TreeMap.this.getLowerEntry(key));
    }

    @Override
    boolean removeEntry(Entry<K, V> entry) {
      return inRange(entry.getKey()) && TreeMap.this.removeEntry(entry);
    }

    private Entry<K, V> guardInRange(Entry<K, V> entry) {
      return entry != null && inRange(entry.getKey()) ? entry : null;
    }

    private boolean inRange(K key) {
      return TreeMap.this.inRange(type, key, fromKey, fromInclusive, toKey, toInclusive);
    }
  }

  private enum SubMapType {
    All,

    Head {
      @Override
      public boolean toKeyValid() {
        return true;
      }
    },

    Range {
      @Override
      public boolean fromKeyValid() {
        return true;
      }

      @Override
      public boolean toKeyValid() {
        return true;
      }
    },

    Tail {
      @Override
      public boolean fromKeyValid() {
        return true;
      }
    };

    /**
     * Returns true if this submap type uses a from-key.
     */
    public boolean fromKeyValid() {
      return false;
    }

    /**
     * Returns true if this submap type uses a to-key.
     */
    public boolean toKeyValid() {
      return false;
    }
  }

  private static final int LEFT = 0;
  private static final int RIGHT = 1;

  private static int otherChild(int child) {
    assert (child == 0 || child == 1);
    return 1 - child;
  }

  // The comparator to use.
  private Comparator<? super K> cmp;

  /*
   * These two fields are just hints to STOB so that it generates serializers
   * for K and V
   */
  @SuppressWarnings("unused")
  private K exposeKeyType;

  @SuppressWarnings("unused")
  private V exposeValueType;

  // The root of the tree.
  private transient Node<K, V> root;

  // The number of nodes in the tree.
  private int size = 0;

  public TreeMap() {
    this((Comparator<? super K>) null);
  }

  @SuppressWarnings("unchecked")
  public TreeMap(Comparator<? super K> c) {
    root = null;
    cmp = Comparators.nullToNaturalOrder(c);
  }

  public TreeMap(Map<? extends K, ? extends V> map) {
    this();
    putAll(map);
  }

  @SuppressWarnings("unchecked")
  public TreeMap(SortedMap<K, ? extends V> map) {
    this(checkNotNull(map).comparator());
    putAll(map); // TODO(jat): more efficient init from sorted map
  }

  @Override
  public void clear() {
    root = null;
    size = 0;
  }

  @Override
  public Comparator<? super K> comparator() {
    return Comparators.naturalOrderToNull(cmp);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
    return new SubMap(SubMapType.Head, null, false, toKey, inclusive);
  }

  @Override
  public V put(K key, V value) {
    Node<K, V> node = new Node<K, V>(key, value);
    State<V> state = new State<V>();
    root = insert(root, node, state);
    if (!state.found) {
      ++size;
    }
    root.isRed = false;
    return state.value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object k) {
    K key = (K) k;
    State<V> state = new State<V>();
    removeWithState(key, state);
    return state.value;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
      K toKey, boolean toInclusive) {
    return new SubMap(SubMapType.Range, fromKey, fromInclusive, toKey, toInclusive);
  }

  @Override
  public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
    return new SubMap(SubMapType.Tail, fromKey, inclusive, null, false);
  }

  /**
   * Returns the first node which compares greater than the given key.
   *
   * @param key the key to search for
   * @return the next node, or null if there is none
   */
  private Node<K, V> getNodeAfter(K key, boolean inclusive) {
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (node != null) {
      int c = cmp.compare(key, node.getKey());
      if (inclusive && c == 0) {
        return node;
      }
      if (c >= 0) {
        node = node.child[RIGHT];
      } else {
        foundNode = node;
        node = node.child[LEFT];
      }
    }
    return foundNode;
  }

  /**
   * Returns the last node which is strictly less than the given key.
   *
   * @param key the key to search for
   * @return the previous node, or null if there is none
   */
  private Node<K, V> getNodeBefore(K key, boolean inclusive) {
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (node != null) {
      int c = cmp.compare(key, node.getKey());
      if (inclusive && c == 0) {
        return node;
      }
      if (c <= 0) {
        node = node.child[LEFT];
      } else {
        foundNode = node;
        node = node.child[RIGHT];
      }
    }
    return foundNode;
  }

  /**
   * Used for testing. Validate that the tree meets all red-black correctness
   * requirements. These include:
   *
   * <pre>
   *  - root is black
   *  - no children of a red node may be red
   *  - the black height of every path through the three to a leaf is exactly the same
   * </pre>
   *
   * @throws RuntimeException if any correctness errors are detected.
   */
  void assertCorrectness() {
    assertCorrectness(root, true);
  }

  @Override
  Iterator<Entry<K, V>> descendingEntryIterator() {
    return new DescendingEntryIterator();
  }

  @Override
  Iterator<Entry<K, V>> entryIterator() {
    return new EntryIterator();
  }

  /**
   * Internal helper function for public {@link #assertCorrectness()}.
   *
   * @param tree the subtree to validate.
   * @param isRed true if the parent of this node is red.
   * @return the black height of this subtree.
   * @throws RuntimeException if this RB-tree is not valid.
   */
  private int assertCorrectness(Node<K, V> tree, boolean isRed) {
    if (tree == null) {
      return 0;
    }
    if (isRed && tree.isRed) {
      throw new RuntimeException("Two red nodes adjacent");
    }

    Node<K, V> leftNode = tree.child[LEFT];
    if (leftNode != null
        && cmp.compare(leftNode.getKey(), tree.getKey()) > 0) {
      throw new RuntimeException("Left child " + leftNode
          + " larger than " + tree);
    }

    Node<K, V> rightNode = tree.child[RIGHT];
    if (rightNode != null
        && cmp.compare(rightNode.getKey(), tree.getKey()) < 0) {
      throw new RuntimeException("Right child " + rightNode
          + " smaller than " + tree);
    }

    int leftHeight = assertCorrectness(leftNode, tree.isRed);
    int rightHeight = assertCorrectness(rightNode, tree.isRed);
    if (leftHeight != 0 && rightHeight != 0 && leftHeight != rightHeight) {
      throw new RuntimeException("Black heights don't match");
    }
    return tree.isRed ? leftHeight : leftHeight + 1;
  }

  /**
   * Finds an entry given a key and returns the node.
   *
   * @param key the search key
   * @return the node matching the key or null
   */
  @Override
  Entry<K, V> getEntry(K key) {
    Node<K, V> tree = root;
    while (tree != null) {
      int c = cmp.compare(key, tree.getKey());
      if (c == 0) {
        return tree;
      }
      int childNum = c < 0 ? LEFT : RIGHT;
      tree = tree.child[childNum];
    }
    return null;
  }

  /**
   * Returns the left-most node of the tree, or null if empty.
   */
  @Override
  Entry<K, V> getFirstEntry() {
    if (root == null) {
      return null;
    }
    Node<K, V> node = root;
    Node<K, V> nextNode;
    while ((nextNode = node.child[LEFT]) != null) {
      node = nextNode;
    }
    return node;
  }

  /**
   * Returns the right-most node of the tree, or null if empty.
   */
  @Override
  Entry<K, V> getLastEntry() {
    if (root == null) {
      return null;
    }
    Node<K, V> node = root;
    Node<K, V> nextNode;
    while ((nextNode = node.child[RIGHT]) != null) {
      node = nextNode;
    }
    return node;
  }

  @Override
  Entry<K, V> getCeilingEntry(K key) {
    return getNodeAfter(key, true);
  }

  @Override
  Entry<K, V> getFloorEntry(K key) {
    return getNodeBefore(key, true);
  }

  @Override
  Entry<K, V> getHigherEntry(K key) {
    return getNodeAfter(key, false);
  }

  @Override
  Entry<K, V> getLowerEntry(K key) {
    return getNodeBefore(key, false);
  }

  @Override
  boolean removeEntry(Entry<K, V> entry) {
    State<V> state = new State<V>();
    state.matchValue = true;
    state.value = entry.getValue();
    return removeWithState(entry.getKey(), state);
  }

  private void inOrderAdd(List<Entry<K, V>> list, SubMapType type, Node<K, V> current,
      K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    if (current == null) {
      return;
    }
    // TODO: truncate this recursion if the whole subtree is known to be
    // outside of bounds?
    Node<K, V> leftNode = current.child[LEFT];
    if (leftNode != null) {
      inOrderAdd(list, type, leftNode,
          fromKey, fromInclusive, toKey, toInclusive);
    }
    if (inRange(type, current.getKey(), fromKey, fromInclusive, toKey, toInclusive)) {
      list.add(current);
    }
    Node<K, V> rightNode = current.child[RIGHT];
    if (rightNode != null) {
      inOrderAdd(list, type, rightNode, fromKey, fromInclusive, toKey, toInclusive);
    }
  }

  private boolean inRange(SubMapType type, K key,
      K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    if (type.fromKeyValid() && smaller(key, fromKey, !fromInclusive)) {
      return false;
    }
    if (type.toKeyValid() && larger(key, toKey, !toInclusive)) {
      return false;
    }
    return true;
  }

  /**
   * Insert a node into a subtree, collecting state about the insertion.
   *
   * If the same key already exists, the value of the node is overwritten with
   * the value from the new node instead.
   *
   * @param tree subtree to insert into
   * @param newNode new node to insert
   * @param state result of the insertion: state.found true if the key already
   *          existed in the tree state.value the old value if the key existed
   * @return the new subtree root
   */
  private Node<K, V> insert(Node<K, V> tree, Node<K, V> newNode, State<V> state) {
    if (tree == null) {
      return newNode;
    } else {
      int c = cmp.compare(newNode.getKey(), tree.getKey());
      if (c == 0) {
        state.value = tree.setValue(newNode.getValue());
        state.found = true;
        return tree;
      }
      int childNum = c < 0 ? LEFT : RIGHT;
      tree.child[childNum] = insert(tree.child[childNum], newNode, state);
      if (isRed(tree.child[childNum])) {
        if (isRed(tree.child[otherChild(childNum)])) {
          // both children are red (nulls are black), make both black and me red
          tree.isRed = true;
          tree.child[LEFT].isRed = false;
          tree.child[RIGHT].isRed = false;
        } else {
          //
          if (isRed(tree.child[childNum].child[childNum])) {
            tree = rotateSingle(tree, otherChild(childNum));
          } else if (isRed(tree.child[childNum].child[otherChild(childNum)])) {
            tree = rotateDouble(tree, otherChild(childNum));
          }
        }
      }
    }
    return tree;
  }

  /**
   * Returns true if <code>node</code> is red. Note that null pointers are
   * considered black.
   */
  private boolean isRed(Node<K, V> node) {
    return node != null && node.isRed;
  }

  /**
   * Returns true if <code>a</code> is greater than or equal to <code>b</code>.
   */
  private boolean larger(K a, K b, boolean orEqual) {
    int compare = cmp.compare(a, b);
    return compare > 0 || (orEqual && compare == 0);
  }

  /**
   * Returns true if <code>a</code> is less than or equal to <code>b</code>.
   */
  private boolean smaller(K a, K b, boolean orEqual) {
    int compare = cmp.compare(a, b);
    return compare < 0 || (orEqual && compare == 0);
  }

  /**
   * Remove a key from the tree, returning whether it was found and its value.
   *
   * @param key key to remove
   * @param state return state, not null
   * @return true if the value was found
   */
  private boolean removeWithState(K key, State<V> state) {
    if (root == null) {
      return false;
    }
    Node<K, V> found = null;
    Node<K, V> parent = null;

    // create a fake tree root to minimize special cases for changing the root
    Node<K, V> head = new Node<K, V>(null, null);
    int dir = RIGHT;
    head.child[RIGHT] = root;

    Node<K, V> node = head;
    while (node.child[dir] != null) {
      int last = dir;
      Node<K, V> grandparent = parent;
      parent = node;
      node = node.child[dir];
      int c = cmp.compare(key, node.getKey());
      dir = c < 0 ? LEFT : RIGHT;
      if (c == 0 && (!state.matchValue || Objects.equals(node.getValue(), state.value))) {
        found = node;
      }
      if (!isRed(node) && !isRed(node.child[dir])) {
        if (isRed(node.child[otherChild(dir)])) {
          parent = parent.child[last] = rotateSingle(node, dir);
        } else if (!isRed(node.child[otherChild(dir)])) {
          Node<K, V> sibling = parent.child[otherChild(last)];
          if (sibling != null) {
            if (!isRed(sibling.child[otherChild(last)])
                && !isRed(sibling.child[last])) {
              parent.isRed = false;
              sibling.isRed = true;
              node.isRed = true;
            } else {
              assert grandparent != null;
              int dir2 = grandparent.child[RIGHT] == parent ? RIGHT : LEFT;
              if (isRed(sibling.child[last])) {
                grandparent.child[dir2] = rotateDouble(parent, last);
              } else if (isRed(sibling.child[otherChild(last)])) {
                grandparent.child[dir2] = rotateSingle(parent, last);
              }
              node.isRed = grandparent.child[dir2].isRed = true;
              grandparent.child[dir2].child[LEFT].isRed = false;
              grandparent.child[dir2].child[RIGHT].isRed = false;
            }
          }
        }
      }
    }

    if (found != null) {
      state.found = true;
      state.value = found.getValue();
      /**
       * put the "node" values in "found" (the node with key K) and cut "node"
       * out. However, we do not want to corrupt "found" -- issue 3423. So
       * create a new node "newNode" to replace the "found" node.
       *
       * TODO: (jat's suggestion) Consider using rebalance to move the deleted
       * node to a leaf to avoid the extra traversal in replaceNode.
       */
      if (node != found) {
        Node<K, V> newNode = new Node<K, V>(node.getKey(), node.getValue());
        replaceNode(head, found, newNode);
        if (parent == found) {
          parent = newNode;
        }
      }

      // cut "node" out
      parent.child[parent.child[RIGHT] == node ? RIGHT : LEFT] = node.child[node.child[LEFT] == null
          ? RIGHT : LEFT];
      size--;
    }

    root = head.child[RIGHT];
    if (root != null) {
      root.isRed = false;
    }
    return state.found;
  }

  /**
   * replace 'node' with 'newNode' in the tree rooted at 'head'. Could have
   * avoided this traversal if each node maintained a parent pointer.
   */
  private void replaceNode(Node<K, V> head, Node<K, V> node, Node<K, V> newNode) {
    Node<K, V> parent = head;
    int direction = (parent.getKey() == null || cmp.compare(node.getKey(), parent.getKey()) > 0)
        ? RIGHT : LEFT; // parent.key == null handles the fake root node
    while (parent.child[direction] != node) {
      parent = parent.child[direction];
      assert parent != null;
      direction = cmp.compare(node.getKey(), parent.getKey()) > 0 ? RIGHT : LEFT;
    }
    // replace node with newNode
    parent.child[direction] = newNode;
    newNode.isRed = node.isRed;
    newNode.child[LEFT] = node.child[LEFT];
    newNode.child[RIGHT] = node.child[RIGHT];
    node.child[LEFT] = null;
    node.child[RIGHT] = null;
  }

  /**
   * Perform a double rotation, first rotating the child which will become the
   * root in the opposite direction, then rotating the root in the specified
   * direction.
   *
   * <pre>
   *           A                                               F
   *         B   C    becomes (with rotateDirection=0)       A   C
   *        D E F G                                         B E   G
   *                                                       D
   * </pre>
   *
   * @param tree root of the subtree to rotate
   * @param rotateDirection the direction to rotate: 0=left, 1=right
   * @return the new root of the rotated subtree
   */
  private Node<K, V> rotateDouble(Node<K, V> tree, int rotateDirection) {
    // free the pointer of the new root
    int otherChildDir = otherChild(rotateDirection);
    tree.child[otherChildDir] = rotateSingle(tree.child[otherChildDir], otherChildDir);
    return rotateSingle(tree, rotateDirection);
  }

  /**
   * Perform a single rotation, pushing the root of the subtree to the specified
   * direction.
   *
   * <pre>
   *      A                                              B
   *    B   C     becomes (with rotateDirection=1)     D   A
   *   D E                                              E   C
   * </pre>
   *
   * @param tree the root of the subtree to rotate
   * @param rotateDirection the direction to rotate: 0=left rotation, 1=right
   * @return the new root of the rotated subtree
   */
  private Node<K, V> rotateSingle(Node<K, V> tree, int rotateDirection) {
    int otherChildDir = otherChild(rotateDirection);
    Node<K, V> save = tree.child[otherChildDir];
    tree.child[otherChildDir] = save.child[rotateDirection];
    save.child[rotateDirection] = tree;
    tree.isRed = true;
    save.isRed = false;
    return save;
  }
}
