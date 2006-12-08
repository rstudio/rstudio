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

  private Vector fVec;

  public ArrayList() {
    fVec = new Vector();
  }

  public ArrayList(Collection c) {
    fVec = new Vector();
    addAll(c);
  }

  public void add(int index, Object o) {
    fVec.add(index, o);
  }

  public boolean add(Object o) {
    return fVec.add(o);
  }

  public boolean addAll(Collection c) {
    return fVec.addAll(c);
  }

  public boolean addAll(int index, Collection c) {
    return fVec.addAll(index, c);
  }

  public void clear() {
    fVec.clear();
  }

  public Object clone() {
    return new ArrayList(this);
  }

  public boolean contains(Object elem) {
    return fVec.contains(elem);
  }

  public Object get(int index) {
    return fVec.get(index);
  }

  public int indexOf(Object elem) {
    return fVec.indexOf(elem);
  }

  public boolean isEmpty() {
    return (fVec.size() == 0);
  }

  public Iterator iterator() {
    return fVec.iterator();
  }

  public int lastIndexOf(Object o) {
    return fVec.lastIndexOf(o);
  }

  public Object remove(int index) {
    return fVec.remove(index);
  }

  public Object set(int index, Object elem) {
    return fVec.set(index, elem);
  }

  public int size() {
    return fVec.size();
  }

  public Object[] toArray() {
    return fVec.toArray();
  }

  protected void removeRange(int fromIndex, int endIndex) {
    fVec.removeRange(fromIndex, endIndex);
  }
}
