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

import java.io.Serializable;

/**
 * Implements a set in terms of a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable,
    Serializable {

  private transient HashMap<E, Object> map;

  /**
   * Ensures that RPC will consider type parameter E to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private E exposeElement;

  public HashSet() {
    map = new HashMap<E, Object>();
  }

  public HashSet(Collection<? extends E> c) {
    map = new HashMap<E, Object>(c.size());
    addAll(c);
  }

  public HashSet(int initialCapacity) {
    map = new HashMap<E, Object>(initialCapacity);
  }

  public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<E, Object>(initialCapacity, loadFactor);
  }

  /**
   * Protected constructor to specify the underlying map. This is used by
   * LinkedHashSet.
   * 
   * @param map underlying map to use.
   */
  protected HashSet(HashMap<E, Object> map) {
    this.map = map;
  }

  @Override
  public boolean add(E o) {
    Object old = map.put(o, this);
    return (old == null);
  }

  @Override
  public void clear() {
    map.clear();
  }

  public Object clone() {
    return new HashSet<E>(this);
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  @Override
  public boolean remove(Object o) {
    return (map.remove(o) != null);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public String toString() {
    return map.keySet().toString();
  }

}
