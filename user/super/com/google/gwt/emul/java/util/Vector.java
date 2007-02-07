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
 * To keep performance characteristics in line with Java community expectations, 
 * <code>Vector</code> is a wrapper around <code>ArrayList</code>.  
 *
 */
public class Vector extends AbstractList implements List, RandomAccess,
    Cloneable {

  private ArrayList arrayList;

  public Vector() {
    arrayList = new ArrayList();
  }

  public Vector(Collection c) {
    arrayList = new ArrayList();
    addAll(c);
  }

  public void add(int index, Object o) {
    arrayList.add(index, o);
  }

  public boolean add(Object o) {
    return arrayList.add(o);
  }

  public boolean addAll(Collection c) {
    return arrayList.addAll(c);
  }

  public boolean addAll(int index, Collection c) {
    return arrayList.addAll(index, c);
  }

  public void addElement(Object o) {
    add(o);
  }

  public void clear() {
    arrayList.clear();
  }

  public Object clone() {
    return new Vector(this);
  }

  public boolean contains(Object elem) {
    return arrayList.contains(elem);
  }

  public void copyInto(Object[] objs) {
    int i = -1;
    int n = size();
    while (++i < n) {
      objs[i] = get(i);
    }
  } 
  
  public Object elementAt(int index) {
    return get(index);
  }

  public Object firstElement() {
    return get(0);
  }

  public Object get(int index) {
    return arrayList.get(index);
  }

  public int indexOf(Object elem) {
    return arrayList.indexOf(elem);
  }

  public int indexOf(Object elem, int index) {
    return arrayList.indexOf(elem, index);
  }

  public void insertElementAt(Object o, int index) {
    add(index, o);
  }

  public boolean isEmpty() {
    return (arrayList.size() == 0);
  }

  public Iterator iterator() {
    return arrayList.iterator();
  }

  public Object lastElement() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("last");
    } else {
      return get(size() - 1);
    }
  }

  public int lastIndexOf(Object o) {
    return arrayList.lastIndexOf(o);
  }

  public int lastIndexOf(Object o, int index) {
    return arrayList.lastIndexOf(o, index);
  }

  public Object remove(int index) {
    return arrayList.remove(index);
  }

  public void removeAllElements() {
    clear();
  }

  public boolean removeElement(Object o) {
    return remove(o);
  }

  public void removeElementAt(int index) {
    remove(index);
  }

  public Object set(int index, Object elem) {
    return arrayList.set(index, elem);
  }

  public void setElementAt(Object o, int index) {
    set(index, o);
  }

  public void setSize(int size) {
    arrayList.setSize(size);
  }

  public int size() {
    return arrayList.size();
  }

  public Object[] toArray() {
    return arrayList.toArray();
  }

  protected void removeRange(int fromIndex, int endIndex) {
    arrayList.removeRange(fromIndex, endIndex);
  }
}
