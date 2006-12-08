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
 * Implements a list backed by an array.
 */
public class ArrayList extends AbstractList implements List, RandomAccess,
    Cloneable {

  private Vector vec;

  public ArrayList() {
    vec = new Vector();
  }

  public ArrayList(Collection c) {
    vec = new Vector();
    addAll(c);
  }

  public void add(int index, Object o) {
    vec.add(index, o);
  }

  public boolean add(Object o) {
    return vec.add(o);
  }

  public boolean addAll(Collection c) {
    return vec.addAll(c);
  }

  public boolean addAll(int index, Collection c) {
    return vec.addAll(index, c);
  }

  public void clear() {
    vec.clear();
  }

  public Object clone() {
    return new ArrayList(this);
  }

  public boolean contains(Object elem) {
    return vec.contains(elem);
  }

  public Object get(int index) {
    return vec.get(index);
  }

  public int indexOf(Object elem) {
    return vec.indexOf(elem);
  }

  public boolean isEmpty() {
    return (vec.size() == 0);
  }

  public Iterator iterator() {
    return vec.iterator();
  }

  public int lastIndexOf(Object o) {
    return vec.lastIndexOf(o);
  }

  public Object remove(int index) {
    return vec.remove(index);
  }

  public Object set(int index, Object elem) {
    return vec.set(index, elem);
  }

  public int size() {
    return vec.size();
  }

  public Object[] toArray() {
    return vec.toArray();
  }

  protected void removeRange(int fromIndex, int endIndex) {
    vec.removeRange(fromIndex, endIndex);
  }
}
