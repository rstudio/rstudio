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

  public HashSet() {
    fMap = new HashMap();
  }

  public HashSet(Collection c) {
    fMap = new HashMap(c.size());
    addAll(c);
  }

  public HashSet(int initialCapacity) {
    fMap = new HashMap(initialCapacity);
  }

  public HashSet(int initialCapacity, float loadFactor) {
    fMap = new HashMap(initialCapacity, loadFactor);
  }

  public boolean add(Object o) {
    Object old = fMap.put(o, Boolean.valueOf(true));
    return (old == null);
  }

  public void clear() {
    fMap.clear();
  }

  public Object clone() {
    return new HashSet(this);
  }

  public boolean contains(Object o) {
    return fMap.containsKey(o);
  }

  public boolean isEmpty() {
    return fMap.isEmpty();
  }

  public Iterator iterator() {
    return fMap.keySet().iterator();
  }

  public boolean remove(Object o) {
    return (fMap.remove(o) != null);
  }

  public int size() {
    return fMap.size();
  }

  public String toString() {
    return fMap.keySet().toString();
  }

  private HashMap fMap;

}
