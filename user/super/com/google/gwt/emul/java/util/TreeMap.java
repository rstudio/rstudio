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

/**
 * Implements a TreeMap using a red-black tree. This guarantees O(log n)
 * performance on lookups, inserts, and deletes while maintaining linear
 * in-order traversal time. Null keys and values are fully supported if the
 * comparator supports them (the default comparator does not).
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class TreeMap<K extends Comparable<K>, V> extends AbstractMap<K, V>
    implements SortedMap<K, V> {
  /*
   * Implementation derived from public domain C implementation as of 5
   * September 2007 at:
   * http://eternallyconfuzzled.com/tuts/datastructures/jsw_tut_rbtree.aspx
   * written by Julienne Walker.
   * 
   * This version does not require a parent pointer kept in each node.
   * 
   * TODO: should this class be serializable? What to do about the comparator?
   */

  /**
   * An iterator for entries kept in a TreeMap.
   */
  private class EntryIterator implements Iterator<Entry<K, V>> {

    // The most recent node returned from next(); null if it hasn't been called
    // or has been deleted.
    private Node<K, V> current;

    // Lower, inclusive bound on returned keys.
    private K fromKey;

    /*
     * Since we don't keep parent pointers, we maintain our position in the tree
     * via the stack which would be used for iterative in-order traversal. The
     * top of the stack is always the next node to visit.
     */
    private Stack<Node<K, V>> inorderStack;

    // Upper, exclusive bound on returned keys, or null if there is none --
    // used for iterators on submaps.
    private K toKey;

    // The type of range bounds on this iterator.
    private SubMapType type;

    // true if the iterator hasn't been initialized yet.
    private boolean uninitialized;

    /**
     * Create a new iterator with no restrictions on the returned values.
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
      inorderStack = new Stack<Node<K, V>>();
      uninitialized = true;
      this.type = type;
      this.fromKey = fromKey;
      this.toKey = toKey;
    }

    public boolean hasNext() {
      if (uninitialized) {
        initialize();
      }
      if (inorderStack.isEmpty()) {
        return false;
      }
      return inRange(inorderStack.peek().key);
    }

    public Entry<K, V> next() {
      if (uninitialized) {
        initialize();
      }
      /*
       * After returning the current node, push the left children of its right
       * child on the stack.
       */
      current = inorderStack.pop();
      if (current == null || !inRange(current.key)) {
        throw new NoSuchElementException("No more elements");
      }
      pushLeftChildren(current.child[RIGHT]);
      return current;
    }

    public void remove() {
      if (current == null) {
        throw new IllegalStateException("No current element");
      }
      TreeMap.this.remove(current.getKey());
      current = null;
    }

    private void initialize() {
      uninitialized = false;
      pushLeftChildren(root);
      current = null;
      if (type == SubMapType.Tail || type == SubMapType.Range) {
        // TODO(jat): rewrite this similar to getAdjacentEntry()
        while (!inorderStack.isEmpty()) {
          current = inorderStack.pop();
          if (inRange(current.key)) {
            // we found the starting point, so push it back on the stack
            inorderStack.push(current);
            current = null;
            break;
          }
          pushLeftChildren(current.child[RIGHT]);
        }
      }
    }

    private boolean inRange(K key) {
      if (type == SubMapType.Head || type == SubMapType.Range) {
        if (cmp.compare(key, toKey) >= 0) {
          return false;
        }
      }
      if (type == SubMapType.Tail || type == SubMapType.Range) {
        if (cmp.compare(key, fromKey) < 0) {
          return false;
        }
      }
      return true;
    }

    /**
     * Follow the left children of the specified node, pushing them on the stack
     * as we go.
     * 
     * @param node parent node to follow left children from.
     */
    private void pushLeftChildren(Node<K, V> node) {
      while (node != null) {
        inorderStack.push(node);
        node = node.child[LEFT];
      }
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
  };

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

    @SuppressWarnings("unchecked")
    // generic cast
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Node)) {
        return false;
      }
      Node<K, V> other = (Node<K, V>) o; // suppress unchecked
      return Utility.equalsWithNullCheck(key, other.key)
          && Utility.equalsWithNullCheck(value, other.value);
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
      return (isRed ? "R: " : "B: ") + key + "=" + value;
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
      Node<K, V> node = getNodeAtOrAfter(fromKey);
      if (node == null || cmp.compare(node.key, toKey) > 0) {
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
      if (cmp.compare(toKey, this.toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + toKey
            + " greater than " + this.toKey);
      }
      if (type == SubMapType.Range || type == SubMapType.Tail) {
        return TreeMap.this.subMap(fromKey, toKey);
      } else {
        return TreeMap.this.headMap(toKey);
      }
    }

    public K lastKey() {
      Node<K, V> node = getNodeBefore(toKey);
      if (node == null || cmp.compare(node.key, fromKey) < 0) {
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
      if ((type == SubMapType.Range || type == SubMapType.Tail)
          && cmp.compare(newFromKey, fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + newFromKey
            + " less than " + fromKey);
      }
      if ((type == SubMapType.Range || type == SubMapType.Head)
          && cmp.compare(newToKey, toKey) > 0) {
        throw new IllegalArgumentException("subMap: " + newToKey
            + " greater than " + toKey);
      }
      return TreeMap.this.subMap(newFromKey, newToKey);
    }

    public SortedMap<K, V> tailMap(K fromKey) {
      if ((type == SubMapType.Range || type == SubMapType.Tail)
          && cmp.compare(fromKey, this.fromKey) < 0) {
        throw new IllegalArgumentException("subMap: " + fromKey + " less than "
            + this.fromKey);
      }
      if (type == SubMapType.Range || type == SubMapType.Head) {
        return TreeMap.this.subMap(fromKey, toKey);
      } else {
        return TreeMap.this.tailMap(fromKey);
      }
    }

    private boolean inRange(K key) {
      if (type == SubMapType.Head || type == SubMapType.Range) {
        if (cmp.compare(key, toKey) >= 0) {
          return false;
        }
      }
      if (type == SubMapType.Tail || type == SubMapType.Range) {
        if (cmp.compare(key, fromKey) < 0) {
          return false;
        }
      }
      return true;
    }
  }

  private enum SubMapType {
    All, Head, Range, Tail,
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

  // The comparator to use.
  private Comparator<? super K> cmp;

  // The root of the tree.
  private Node<K, V> root;

  // The number of nodes in the tree.
  private int size = 0;

  @SuppressWarnings("unchecked")
  public TreeMap() {
    this((Comparator<? super K>) DEFAULT_COMPARATOR);
  }

  public TreeMap(Comparator<? super K> c) {
    root = null;
    cmp = c;
  }

  public TreeMap(Map<? extends K, ? extends V> map) {
    this();
    putAll(map);
  }

  @SuppressWarnings("unchecked")
  public TreeMap(SortedMap<K, ? extends V> map) {
    cmp = map.comparator();
    if (cmp == null) {
      cmp = (Comparator<? super K>) DEFAULT_COMPARATOR;
    }
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
    if (root == null) {
      throw new NoSuchElementException();
    }
    Node<K, V> node = root;
    while (node.child[LEFT] != null) {
      node = node.child[LEFT];
    }
    return node.key;
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
    if (root == null) {
      throw new NoSuchElementException();
    }
    Node<K, V> node = root;
    while (node.child[RIGHT] != null) {
      node = node.child[RIGHT];
    }
    return node.key;
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
   * Return the first node which compares equal to or greater than the given
   * key.
   * 
   * @param key the key to search for
   * @return the next node, or null if there is none.
   */
  protected Node<K, V> getNodeAtOrAfter(K key) {
    if (root == null) {
      return null;
    }
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (true) {
      int c = cmp.compare(key, node.key);
      if (c == 0) {
        return node;
      } else if (c > 0) {
        node = node.child[RIGHT];
        if (node == null) {
          // ran off the tree and we are past this node, so return best one
          return foundNode;
        }
      } else {
        foundNode = node;
        Node<K, V> nextNode = node.child[LEFT];
        if (nextNode == null) {
          // ran off the tree and we are before this node, so return it
          return node;
        }
        node = nextNode;
      }
    }
  }

  /**
   * Return the last node which is strictly less than the given key.
   * 
   * @param key the key to search for
   * @return the previous node, or null if there is none.
   */
  protected Node<K, V> getNodeBefore(K key) {
    if (root == null) {
      return null;
    }
    Node<K, V> foundNode = null;
    Node<K, V> node = root;
    while (true) {
      int c = cmp.compare(key, node.key);
      if (c <= 0) {
        node = node.child[LEFT];
        if (node == null) {
          // ran off the tree and we are past this node, so return best one
          return foundNode;
        }
      } else {
        foundNode = node;
        Node<K, V> nextNode = node.child[RIGHT];
        if (nextNode == null) {
          // ran off the tree and we are before this node, so return it
          return node;
        }
        node = nextNode;
      }
    }
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
   * Internal helper function for public (@see assertCorrectness()).
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
      int childNum = (c > 0) ? 0 : 1;
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
   * Return true if <code>node</code> is red. Note that null pointers are
   * considered black.
   */
  private boolean isRed(Node<K, V> node) {
    return node != null && node.isRed;
  }

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
      if (state != null) {
        state.found = true;
        state.value = found.value;
      }
      found.key = node.key;
      found.value = node.value;
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
