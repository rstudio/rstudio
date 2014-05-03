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

import com.google.gwt.lang.Array;

import java.io.Serializable;

/**
 * Resizeable array implementation of the List interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/ArrayList.html">[Sun
 * docs]</a>
 *
 * <p>
 * This implementation differs from JDK 1.5 <code>ArrayList</code> in terms of
 * capacity management. There is no speed advantage to pre-allocating array
 * sizes in JavaScript, so this implementation does not include any of the
 * capacity and "growth increment" concepts in the standard ArrayList class.
 * Although <code>ArrayList(int)</code> accepts a value for the initial
 * capacity of the array, this constructor simply delegates to
 * <code>ArrayList()</code>. It is only present for compatibility with JDK
 * 1.5's API.
 * </p>
 *
 * @param <E> the element type.
 */
public class ArrayList<E> extends AbstractList<E> implements List<E>,
    Cloneable, RandomAccess, Serializable {

  private static native void setCapacity(Object[] array, int newSize) /*-{
    array.length = newSize;
  }-*/;

  private static native void splice(Object[] array, int index, int deleteCount) /*-{
    array.splice(index, deleteCount);
  }-*/;

  private static native void splice(Object[] array, int index, int deleteCount,
      Object value) /*-{
    array.splice(index, deleteCount, value);
  }-*/;

  private void insertAt(int index, Object[] values) {
    Array.nativeArrayInsert(values, 0, array, index, values.length);
  }

  /**
   * This field holds a JavaScript array.
   */
  private transient E[] array = (E[]) new Object[0];

  /**
   * Ensures that RPC will consider type parameter E to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private E exposeElement;

  /**
   * The size of the array.
   */
  private int size = 0;

  public ArrayList() {
  }

  public ArrayList(Collection<? extends E> c) {
    // Avoid calling overridable methods from constructors
    insertAt(0, c.toArray());
    size = array.length;
  }

  public ArrayList(int initialCapacity) {
    // Avoid calling overridable methods from constructors
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Initial capacity must not be negative");
    }
    setCapacity(array, initialCapacity);
  }

  @Override
  public boolean add(E o) {
    array[size++] = o;
    return true;
  }

  @Override
  public void add(int index, E o) {
    if (index < 0 || index > size) {
      indexOutOfBounds(index, size);
    }
    splice(array, index, 0, o);
    ++size;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    Object[] cArray = c.toArray();
    int len = cArray.length;
    if (len == 0) {
      return false;
    }
    insertAt(size, cArray);
    size += len;
    return true;
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    if (index < 0 || index > size) {
      indexOutOfBounds(index, size);
    }
    Object[] cArray = c.toArray();
    int len = cArray.length;
    if (len == 0) {
      return false;
    }
    insertAt(index, cArray);
    size += len;
    return true;
  }

  @Override
  public void clear() {
    array = (E[]) new Object[0];
    size = 0;
  }

  public Object clone() {
    return new ArrayList<E>(this);
  }

  @Override
  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }

  public void ensureCapacity(int capacity) {
    if (capacity > size) {
      setCapacity(array, capacity);
    }
  }

  @Override
  public E get(int index) {
    checkIndex(index, size);
    return array[index];
  }

  @Override
  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    return lastIndexOf(o, size() - 1);
  }

  @Override
  public E remove(int index) {
    E previous = get(index);
    splice(array, index, 1);
    --size;
    return previous;
  }

  @Override
  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    remove(i);
    return true;
  }

  @Override
  public E set(int index, E o) {
    E previous = get(index);
    array[index] = o;
    return previous;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Object[] toArray() {
    return Array.cloneSubrange(array, 0, size);
  }

  /*
   * Faster than the iterator-based implementation in AbstractCollection.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] out) {
    if (out.length < size) {
      out = Array.createFrom(out, size);
    }
    for (int i = 0; i < size; ++i) {
      out[i] = (T) array[i];
    }
    if (out.length > size) {
      out[size] = null;
    }
    return out;
  }

  public void trimToSize() {
    setCapacity(array, size);
  }

  @Override
  protected void removeRange(int fromIndex, int endIndex) {
    checkIndex(fromIndex, size + 1);
    if (endIndex < fromIndex || endIndex > size) {
      indexOutOfBounds(endIndex, size);
    }
    int count = endIndex - fromIndex;
    splice(array, fromIndex, count);
    size -= count;
  }

  /**
   * Used by Vector.
   */
  int capacity() {
    return array.length;
  }

  /**
   * Used by Vector.
   */
  int indexOf(Object o, int index) {
    for (; index < size; ++index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Used by Vector.
   */
  int lastIndexOf(Object o, int index) {
    for (; index >= 0; --index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Used by Vector.
   */
  void setSize(int newSize) {
    setCapacity(array, newSize);
    size = newSize;
  }
}
