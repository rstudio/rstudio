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

/**
 * To keep performance characteristics in line with Java community expectations, 
 * <code>Vector</code> is a wrapper around <code>ArrayList</code>.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/Vector.html
 * 
 * @param <E> element type.
 */
public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess,
    Cloneable {

  private transient ArrayList<E> arrayList;

  public Vector() {
    arrayList = new ArrayList<E>();
  }

  public Vector(Collection<? extends E> c) {
    arrayList = new ArrayList<E>();
    addAll(c);
  }

 /**
   * There is no speed advantage to pre-allocating array sizes in JavaScript,
   * so the <code>intialCapacity</code> parameter is ignored. This constructor is
   * only present for compatibility with JDK 1.4's API.
   */
  public Vector(int initialCapacity) {
    arrayList = new ArrayList<E>(initialCapacity);
  } 

  public boolean add(E o) {
    return arrayList.add(o);
  }

  public void add(int index, E o) {
    arrayList.add(index, o);
  }

  public boolean addAll(Collection<? extends E> c) {
    return arrayList.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    return arrayList.addAll(index, c);
  }

  public void addElement(E o) {
    add(o);
  }

  public void clear() {
    arrayList.clear();
  }

  public Object clone() {
    return new Vector<E>(this);
  }

  public boolean contains(Object elem) {
    return arrayList.contains(elem);
  }

  public boolean containsAll(Collection<?> c) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("containsAll not implemented");
  }

  public void copyInto(Object[] objs) {
    int i = -1;
    int n = size();
    while (++i < n) {
      objs[i] = get(i);
    }
  } 
  
  public E elementAt(int index) {
    return get(index);
  }

  public Enumeration<E> elements() {
    // TODO(jat): implement
    return null;
  }
  
  public void ensureCapacity(int minCapacity) {
    // TODO(jat): implement
  }

  public E firstElement() {
    return get(0);
  }

  public E get(int index) {
    return arrayList.get(index);
  }

  public int indexOf(Object elem) {
    return arrayList.indexOf(elem);
  }

  public int indexOf(Object elem, int index) {
    return arrayList.indexOf(elem, index);
  }

  public void insertElementAt(E o, int index) {
    add(index, o);
  }

  public boolean isEmpty() {
    return (arrayList.size() == 0);
  }

  public Iterator<E> iterator() {
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

  public E remove(int index) {
    return arrayList.remove(index);
  }

  public boolean removeAll(Collection<?> c) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("removeAll not implemented");
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

  public E set(int index, E elem) {
    return arrayList.set(index, elem);
  }

  public void setElementAt(E o, int index) {
    set(index, o);
  }

  public void setSize(int size) {
    arrayList.setSize(size);
  }

  public int size() {
    return arrayList.size();
  }

  public List<E> subList(int fromIndex, int toIndex) {
    return arrayList.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return arrayList.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return arrayList.toArray(a);
  }

  public String toString() {
    return arrayList.toString();
  }

  /**
   * Currenty ignored.
   */
  public void trimToSize() {
    arrayList.trimToSize();
  }

  protected void removeRange(int fromIndex, int endIndex) {
    arrayList.removeRange(fromIndex, endIndex);
  }
}
