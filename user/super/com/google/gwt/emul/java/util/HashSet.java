/*
 * Copyright 2006 Google Inc.
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
 * Implements a set in terms of a hash table.
 */
public class HashSet extends AbstractSet implements Set, Cloneable {

  private HashMap map;

  public HashSet() {
    map = new HashMap();
  }

  public HashSet(Collection c) {
    map = new HashMap(c.size());
    addAll(c);
  }

  public HashSet(int initialCapacity) {
    map = new HashMap(initialCapacity);
  }

  public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap(initialCapacity, loadFactor);
  }

  public boolean add(Object o) {
    Object old = map.put(o, Boolean.valueOf(true));
    return (old == null);
  }

  public void clear() {
    map.clear();
  }

  public Object clone() {
    return new HashSet(this);
  }

  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Iterator iterator() {
    return map.keySet().iterator();
  }

  public boolean remove(Object o) {
    return (map.remove(o) != null);
  }

  public int size() {
    return map.size();
  }

  public String toString() {
    return map.keySet().toString();
  }

}
