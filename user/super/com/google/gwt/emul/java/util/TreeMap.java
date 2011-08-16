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
 * Implements a TreeMap using a red-black tree. This guarantees O(log n)
 * performance on lookups, inserts, and deletes while maintaining linear
 * in-order traversal time. Null keys and values are fully supported if the
 * comparator supports them (the default comparator does not).
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TreeMap<K, V> extends AbstractMap<K, V> implements
    SortedMap<K, V>, Serializable {
  /*
   * Implementation derived from public domain C implementation as of 5
   * September 2007 at:
   * http://eternallyconfuzzled.com/tuts/datastructures/jsw_tut_rbtree.aspx
   * written by Julienne Walker.
   *
   * This version does not require a parent pointer kept in each node.
   */

  /**
   * Iterator for <code>EntrySet</code>.
   */
  private final class EntryIterator implements Iterator<Entry<K, V>> {
    private final Iterator<Map.Entry<K, V>> iter;
    private Map.Entry<K, V> last = null;

    /**
     * Constructor for <code>EntrySetIterator</code>.
     */
    public EntryIterator() {
      this(SubMapType.All, null, null);
    }

    /**
     * Create an iterator which may return only a restricted range.
     *
     * @param fromKey the first key to return in the iterator.
     * @param toKey the upper bound of keys to return.
     */
    public EntryIterator(SubMapType type, K fromKey, K toKey) {
      List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>();
      inOrderAdd(list, type, TreeMap.this.root, fromKey, toKey);
      this.iter = list.iterator();
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public Map.Entry<K, V> next() {
      return last = iter.next();
    }

    public void remove() {
      iter.remove();
      TreeMap.this.remove(last.getKey());
    }

    private void inOrderAdd(List<Map.Entry<K, V>> list, SubMapType type,
        Node<K, V> current, K fromKey, K toKey) {
      if (current == null) {
        return;
      }
      if (current.child[LEFT] != null) {
        inOrderAdd(list, type, current.child[LEFT], fromKey, toKey);
      }
      if (inRange(type, current.getKey(), fromKey, toKey)) {
        list.add(current);
      }
      if (current.child[RIGHT] != null) {
        inOrderAdd(list, type, current.child[RIGHT], fromKey, toKey);
      }
    }

    private boolean inRange(SubMapType type, K key, K fromKey, K toKey) {
      if (type.toKeyValid()) {
        if (cmp.compare(key, toKey) >= 0) {
          return false;
        }
      }
      if (type.fromKeyValid()) {
        if (cmp.compare(key, fromKey) < 0) {
          return false;
        }
      }
      return true;
    }
  }

  private final class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public void clear() {
      TreeMap.this.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<K, V> entry = (Entry<K, V>) o; // suppress unchecked
      Entry<K, V> lookupEntry = getEntry(entry.getKey());
      return lookupEntry != null
          && Utility.equalsWithNullCheck(lookupEntry.getValue(),
              entry.getValue());
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
      /*
       * TODO(jat): is this safe since we can copy a predecessor's data to an
       * interior node when it is deleted? I think so since we can only go
       * through the iterator in ascending order, so we will have passed the one
       * that was copied by the time we can delete a node that will make that
       * copy.
       */
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<K, V> entry = (Map.Entry<K, V>) o; // suppress unchecked
      State<V> state = new State<V>();
      state.matchValue = true;
      state.value = entry.getValue();
      return removeWithState(entry.getKey(), state);
    }

    @Override
    public int size() {
      return TreeMap.this.size();
    }
  }

  /**
   * Tree node.
   *
   * @param <K> key type
   * @param <V> value type
   */
  private static class Node<K, V> implements Entry<K, V> {
    /*
     * The children are kept in an array to minimize the normal duplication of
     * code.
     */
    protected Node<K, V>[] child;
    protected boolean isRed;
    protected K key;
    protected V value;

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
    @SuppressWarnings("unchecked")
    // create array of generic elements
    public Node(K key, V value, boolean isRed) {
      this.key = key;
      this.value = value;
      child = new Node[2]; // suppress unchecked
      this.isRed = isRed;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
      return Utility.equalsWithNullCheck(key, other.getKey())
          && Utility.equalsWithNullCheck(value, other.getValue());
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      int keyHash = (key != null ? key.hashCode() : 0);
      int valueHash = (value != null ? value.hashCode() : 0);
      return keyHash ^ valueHash;
    }

    public V setValue(V value) {
      V old = this.value;
      this.value = value;
      return old;
    }

    @Override
    public String toString() {
      // for compatibility with the real Jre: issue 3422
      return key + "=" + value;
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
      return "State: mv=" + matchValue + " value=" + value + " done=" + done
          + " found=" + found;
    }
  }

  private class SubMap extends AbstractMap<K, V> implements SortedMap<K, V> {

    // valid only if type is Range or Tail
    public final K fromKey;

    // valid only if type is Range or Head
    public final K toKey;

    public final SubMapType type;

    SubMap(SubMapType type, K fromKey, K toKey) {
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
      this.toKey = toKey;
    }

    public Comparator<? super K> comparator() {
      return TreeMap.this.comparator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object k) {
      K key = (K) k; // suppress unchecked
      if (!inRange(key)) {
        return false;
      }
      return TreeMap.this.containsKey(k);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
      return new AbstractSet<Entry<K, V>>() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o) {
          if (!(o instanceof Map.Entry)) {
            return false;
          }
          Map.Entry<K, V> entry = (Entry<K, V>) o; // suppress unchecked
          K key = entry.getKey();
          if (!inRange(key)) {
            return false;
          }
          Entry<K, V> lookupEntry = getEntry(key);
          return lookupEntry != null
              && Utility.equalsWithNullCheck(lookupEntry.getValue(),
                  entry.getValue());
        }

        @Override
        public boolean isEmpty() {
          return SubMap.this.isEmpty();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return new EntryIterator(type, fromKey, toKey);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
          if (!(o instanceof Map.Entry)) {
            return false;
          }
          Map.Entry<K, V> entry = (Map.Entry<K, V>) o; // suppress unchecked
          if (!inRange(entry.getKey())) {
            return false;
          }
          State<V> state = new State<V>();
          state.matchValue = true;
          state.value = entry.getValue();
          return removeWithState(entry.getKey(), state);
        }

        @Override
        public int size() {
          // TODO(jat): more efficient way to do this?
          int n = 0;
          Iterator<Entry<K, V>> it = iterator();
          while (it.hasNext()) {
            it.next();
            n++;
          }
          return n;
        }
      };
    }

    public K firstKey() {
      Node<K, V> node = throwNSE(getFirstSubmapNode());
      if (type.toKeyValid() && cmp.compare(node.key, toKey) > 0) {
        throw new NoSuchElementException();
      }
      return node.key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object k) {
      K key = (K) k; // suppress unchecked
      if (!inRange(key)) {
        return null;
      }
      return TreeMap.this.get(key);
    }

    public SortedMap<K, V> headMap(K toKey) {
      if (type.toKeyValid() && cmp.compare(toKey, this.toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + toKey
            + " greater than " + this.toKey);
      }
      if (type.fromKeyValid()) {
        return TreeMap.this.subMap(fromKey, toKey);
      } else {
        return TreeMap.this.headMap(toKey);
      }
    }

    @Override
    public boolean isEmpty() {
      return getFirstSubmapNode() == null;
    }

    public K lastKey() {
      Node<K, V> node = throwNSE(getLastSubmapNode());
      if (type.fromKeyValid() && cmp.compare(node.key, fromKey) < 0) {
        throw new NoSuchElementException();
      }
      return node.key;
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
      K key = (K) k; // suppress unchecked
      if (!inRange(key)) {
        return null;
      }
      return TreeMap.this.remove(key);
    }

    public SortedMap<K, V> subMap(K newFromKey, K newToKey) {
      if (type.fromKeyValid() && cmp.compare(newFromKey, fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + newFromKey
            + " less than " + fromKey);
      }
      if (type.toKeyValid() && cmp.compare(newToKey, toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + newToKey
            + " greater than " + toKey);
      }
      return TreeMap.this.subMap(newFromKey, newToKey);
    }

    public SortedMap<K, V> tailMap(K fromKey) {
      if (type.fromKeyValid() && cmp.compare(fromKey, this.fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + fromKey + " less than "
            + this.fromKey);
      }
      if (type.toKeyValid()) {
        return TreeMap.this.subMap(fromKey, toKey);
      } else {
        return TreeMap.this.tailMap(fromKey);
      }
    }

    private Node<K, V> getFirstSubmapNode() {
      Node<K, V> node;
      if (type.fromKeyValid()) {
        node = getNodeAtOrAfter(fromKey);
      } else {
        node = getFirstNode();
      }
      // The map is empty if the first key after fromKey is out of range.
      return node != null && inRange(node.getKey()) ? node : null;
    }

    private Node<K, V> getLastSubmapNode() {
      Node<K, V> node;
      if (type.toKeyValid()) {
        node = getNodeBefore(toKey);
      } else {
        node = getLastNode();
      }
      // The map is empty if the last key before toKey is out of range.
      return node != null && inRange(node.getKey()) ? node : null;
    }

    private boolean inRange(K key) {
      if (type.toKeyValid()) {
        if (cmp.compare(key, toKey) >= 0) {
          return false;
        }
      }
      if (type.fromKeyValid()) {
        if (cmp.compare(key, fromKey) < 0) {
          return false;
        }
      }
      return true;
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

  /**
   * Default comparator, requires the key type implement Comparable. Will fail
   * on null values.
   */
  @SuppressWarnings("unchecked")
  private static Comparator<?> DEFAULT_COMPARATOR = new Comparator<Comparable>() {
    public int compare(Comparable a, Comparable b) {
      // Explicit null check to match JRE specs
      if (a == null || b == null) {
        throw new NullPointerException();
      }
      return a.compareTo(b);
    }
  };

  private static final int LEFT = 0;
  private static final int RIGHT = 1;

  private static int otherChild(int child) {
    assert (child == 0 || child == 1);
    return 1 - child;
  }

  /**
   * Throw a NoSuchElementException if the specified node is null.
   *
   * Used to clean up error checking at use sites.
   *
   * @param node node to check
   * @param <NK> key type
   * @param <NV> value type
   * @return node, guaranteed to be non-null
   * @throws NoSuchElementException if node is null
   */
  private static <NK, NV> Node<NK, NV> throwNSE(Node<NK, NV> node) {
    if (node == null) {
      throw new NoSuchElementException();
    }
    return node;
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
    if (c == null) {
      c = (Comparator<? super K>) DEFAULT_COMPARATOR;
    }
    cmp = c;
  }

  public TreeMap(Map<? extends K, ? extends V> map) {
    this();
    putAll(map);
  }

  @SuppressWarnings("unchecked")
  public TreeMap(SortedMap<K, ? extends V> map) {
    this(map.comparator());
    putAll(map); // TODO(jat): more efficient init from sorted map
  }

  @Override
  public void clear() {
    root = null;
    size = 0;
  }

  public Comparator<? super K> comparator() {
    if (cmp == DEFAULT_COMPARATOR) {
      return null;
    }
    return cmp;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(Object key) {
    return getEntry((K) key) != null; // suppress unchecked cast
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  public K firstKey() {
    return throwNSE(getFirstNode()).key;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(Object k) {
    K key = (K) k; // suppress unchecked

    /*
     * Don't bother validating the key as getEntry does that internally if the
     * map is non-empty. This is against the spec but matches JRE 1.5 behavior.
     */

    Node<K, V> entry = getEntry(key);
    return entry != null ? entry.getValue() : null;
  }

  public SortedMap<K, V> headMap(K toKey) {
    return new SubMap(SubMapType.Head, null, toKey);
  }

  public K lastKey() {
    return throwNSE(getLastNode()).key;
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
  public V remove(Object keyObj) {
    K key = (K) keyObj; // suppress unchecked cast
    State<V> state = new State<V>();
    removeWithState(key, state);
    return state.value;
  }

  @Override
  public int size() {
    return size;
  }

  public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
    return new SubMap(SubMapType.Range, fromKey, toKey);
  }

  public SortedMap<K, V> tailMap(K fromKey) {
    return new SubMap(SubMapType.Tail, fromKey, null);
  }

  /**
   * Returns the first node which compares equal to or greater than the given
   * key.
   *
   * @param key the key to search for
   * @return the next node, or null if there is none
   */
  protected Node<K, V> getNodeAtOrAfter(K key) {
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (node != null) {
      int c = cmp.compare(key, node.key);
      if (c == 0) {
        return node;
      } else if (c > 0) {
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
  protected Node<K, V> getNodeBefore(K key) {
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (node != null) {
      int c = cmp.compare(key, node.key);
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

    if (tree.child[LEFT] != null
        && cmp.compare(tree.child[LEFT].key, tree.key) > 0) {
      throw new RuntimeException("Left child " + tree.child[LEFT]
          + " larger than " + tree);
    }
    if (tree.child[RIGHT] != null
        && cmp.compare(tree.child[RIGHT].key, tree.key) < 0) {
      throw new RuntimeException("Right child " + tree.child[RIGHT]
          + " smaller than " + tree);
    }

    int leftHeight = assertCorrectness(tree.child[LEFT], tree.isRed);
    int rightHeight = assertCorrectness(tree.child[RIGHT], tree.isRed);
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
  private Node<K, V> getEntry(K key) {
    Node<K, V> tree = root;
    while (tree != null) {
      int c = cmp.compare(key, tree.key);
      if (c == 0) {
        return tree;
      }
      if (c < 0) {
        tree = tree.child[LEFT];
      } else {
        tree = tree.child[RIGHT];
      }
    }
    return null;
  }

  /**
   * Returns the left-most node of the tree, or null if empty.
   */
  private Node<K, V> getFirstNode() {
    if (root == null) {
      return null;
    }
    Node<K, V> node = root;
    while (node.child[LEFT] != null) {
      node = node.child[LEFT];
    }
    return node;
  }

  /**
   * Returns the right-most node of the tree, or null if empty.
   */
  private Node<K, V> getLastNode() {
    if (root == null) {
      return null;
    }
    Node<K, V> node = root;
    while (node.child[RIGHT] != null) {
      node = node.child[RIGHT];
    }
    return node;
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
      int c = cmp.compare(tree.key, newNode.key);
      if (c == 0) {
        state.value = tree.value;
        state.found = true;
        tree.value = newNode.value;
        return tree;
      }
      int childNum = (c > 0) ? LEFT : RIGHT;
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
    Node<K, V> node;
    Node<K, V> found = null;
    Node<K, V> parent = null;
    Node<K, V> grandparent = null;

    // create a fake tree root to minimize special cases for changing the root
    Node<K, V> head = new Node<K, V>(null, null);
    int dir = 1;
    head.child[RIGHT] = root;

    node = head;
    while (node.child[dir] != null) {
      int last = dir;
      grandparent = parent;
      parent = node;
      node = node.child[dir];
      int c = cmp.compare(node.key, key);
      dir = c < 0 ? RIGHT : LEFT;
      if (c == 0 && (!state.matchValue || node.value.equals(state.value))) {
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
      state.value = found.value;
      /**
       * put the "node" values in "found" (the node with key K) and cut "node"
       * out. However, we do not want to corrupt "found" -- issue 3423. So
       * create a new node "newNode" to replace the "found" node.
       *
       * TODO: (jat's suggestion) Consider using rebalance to move the deleted
       * node to a leaf to avoid the extra traversal in replaceNode.
       */
      if (node != found) {
        Node<K, V> newNode = new Node<K, V>(node.key, node.value);
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
    int direction = (parent.key == null || cmp.compare(node.key, parent.key) > 0)
        ? RIGHT : LEFT; // parent.key == null handles the fake root node
    while (parent.child[direction] != node) {
      parent = parent.child[direction];
      assert parent != null;
      direction = cmp.compare(node.key, parent.key) > 0 ? RIGHT : LEFT;
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
    tree.child[otherChild(rotateDirection)] = rotateSingle(
        tree.child[otherChild(rotateDirection)], otherChild(rotateDirection));
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
    Node<K, V> save = tree.child[otherChild(rotateDirection)];
    tree.child[otherChild(rotateDirection)] = save.child[rotateDirection];
    save.child[rotateDirection] = tree;
    tree.isRed = true;
    save.isRed = false;
    return save;
  }

}
