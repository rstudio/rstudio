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
 * Implements a set using a TreeMap. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/TreeSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class TreeSet<E> extends AbstractSet<E> implements SortedSet<E>, Serializable {

  /**
   * TreeSet is stored as a TreeMap of the requested type to a constant Boolean.
   */
  private SortedMap<E, Boolean> map;

  public TreeSet() {
    map = new TreeMap<E, Boolean>();
  }

  public TreeSet(Collection<? extends E> c) {
    this();
    addAll(c);
  }

  public TreeSet(Comparator<? super E> c) {
    if (c == null) {
      map = new TreeMap<E, Boolean>();
    } else {
      map = new TreeMap<E, Boolean>(c);
    }
  }

  public TreeSet(SortedSet<E> s) {
    this(s.comparator());
    // TODO(jat): more efficient implementation
    addAll(s);
  }

  /**
   * Used to wrap subset maps in a new TreeSet.
   * 
   * @param map map to use for backing store
   */
  private TreeSet(SortedMap<E, Boolean> map) {
    this.map = map;
  }

  @Override
  public boolean add(E o) {
    // Use Boolean.FALSE as a convenient non-null value to store in the map
    return map.put(o, Boolean.FALSE) == null;
  }

  @Override
  public void clear() {
    map.clear();
  }

  public Comparator<? super E> comparator() {
    return map.comparator();
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  public E first() {
    return map.firstKey();
  }

  public SortedSet<E> headSet(E toElement) {
    return new TreeSet<E>(map.headMap(toElement));
  }

  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  public E last() {
    return map.lastKey();
  }

  @Override
  public boolean remove(Object o) {
    return map.remove(o) != null;
  }

  @Override
  public int size() {
    return map.size();
  }

  public SortedSet<E> subSet(E fromElement, E toElement) {
    return new TreeSet<E>(map.subMap(fromElement, toElement));
  }

  public SortedSet<E> tailSet(E fromElement) {
    return new TreeSet<E>(map.tailMap(fromElement));
  }
}
