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
 * Linked list implementation. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedList.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class LinkedList<E> extends AbstractSequentialList<E> implements
    List<E>, Queue<E>, Cloneable {

  public LinkedList() {
    // TODO(jat): implement
    throw new UnsupportedOperationException("LinkedList unsupported");
  }

  public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
  }

  @Override
  public boolean add(E o) {
    // TODO(jat): implement
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    // TODO(jat): implement
    return false;
  }

  public void addFirst(E o) {
    // TODO(jat): implement
  }

  public void addLast(E o) {
    // TODO(jat): implement
  }

  @Override
  public void clear() {
    // TODO(jat): implement
  }

  public Object clone() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public boolean contains(Object o) {
    // TODO(jat): implement
    return false;
  }

  public E element() {
    return getFirst();
  }

  public E getFirst() {
    // TODO(jat): implement
    return null;
  }

  public E getLast() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public int indexOf(Object o) {
    // TODO(jat): implement
    return 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    // TODO(jat): implement
    return 0;
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    // TODO(jat): implement
    return null;
  }

  public boolean offer(E o) {
    return add(o);
  }

  public E peek() {
    if (size() == 0) {
      return null;
    } else {
      return getFirst();
    }
  }

  public E poll() {
    if (size() == 0) {
      return null;
    } else {
      return removeFirst();
    }
  }

  public E remove() {
    return removeFirst();
  }

  public E removeFirst() {
    // TODO(jat): implement
    return null;
  }

  public E removeLast() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public int size() {
    // TODO(jat): implement
    return 0;
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object[] toArray() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    // TODO(jat): implement
    return null;
  }

}
