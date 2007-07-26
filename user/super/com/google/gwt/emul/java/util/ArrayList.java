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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.lang.Array;

/**
 * See Sun's JDK 1.4 documentation for documentation.
 * 
 * <p>
 * This implementation differs from JDK 1.4 <code>ArrayList</code> in terms of
 * capacity management. There is no speed advantage to pre-allocating array
 * sizes in JavaScript, so this implementation does not include any of the
 * capacity and "growth increment" concepts in the standard ArrayList class.
 * Although <code>ArrayList(int)</code> accepts a value for the intitial
 * capacity of the array, this constructor simply delegates to
 * <code>ArrayList()</code>. It is only present for compatibility with JDK
 * 1.4's API.
 * </p>
 */
public class ArrayList extends AbstractList implements List, Cloneable,
    RandomAccess {

  private static native void addImpl(JavaScriptObject array, int index, Object o) /*-{
    array.splice(index, 0, o);
  }-*/;

  private static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  private static native Object getImpl(JavaScriptObject array, int index) /*-{
    return array[index];
  }-*/;

  private static native void removeRangeImpl(JavaScriptObject array, int index,
      int count) /*-{
    array.splice(index, count);
  }-*/;

  private static native void setImpl(JavaScriptObject array, int index, Object o) /*-{
    array[index] = o;
  }-*/;

  private static native void setSizeImpl(JavaScriptObject array, int newSize) /*-{
    array.length = newSize;
  }-*/;

  /**
   * This field holds a JavaScript array.
   */
  private transient JavaScriptObject array;

  /**
   * The size of the array.
   */
  private int size;

  {
    clearImpl();
  }

  public ArrayList() {
  }

  public ArrayList(Collection c) {
    addAll(c);
  }

  /**
   * There is no speed advantage to pre-allocating array sizes in JavaScript, so
   * the <code>intialCapacity</code> parameter is ignored. This constructor is
   * only present for compatibility with JDK 1.4's API.
   */
  public ArrayList(int initialCapacity) {
    // initialCapacity is ignored in JS implementation; this constructor is
    // present for JDK 1.4 compatibility
  }

  public void add(int index, Object o) {
    if (index < 0 || index > size) {
      indexOutOfBounds(index);
    }
    addImpl(array, index, o);
    ++size;
  }

  public boolean add(Object o) {
    setImpl(array, size++, o);
    return true;
  }

  public boolean addAll(Collection c) {
    Iterator iter = c.iterator();
    boolean changed = iter.hasNext();
    while (iter.hasNext()) {
      setImpl(array, size++, iter.next());
    }
    return changed;
  }

  public void clear() {
    clearImpl();
  }

  public Object clone() {
    return new ArrayList(this);
  }

  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }

  public Object get(int index) {
    if (index < 0 || index >= size) {
      indexOutOfBounds(index);
    }
    return getImpl(array, index);
  }

  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int lastIndexOf(Object o) {
    return lastIndexOf(o, size() - 1);
  }

  public Object remove(int index) {
    Object previous = get(index);
    removeRangeImpl(array, index, 1);
    --size;
    return previous;
  }

  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    remove(i);
    return true;
  }

  public Object set(int index, Object o) {
    Object previous = get(index);
    setImpl(array, index, o);
    return previous;
  }

  public int size() {
    return size;
  }

  /*
   * Faster than the iterator-based implementation in AbstractCollection.
   */
  public Object[] toArray(Object[] a) {
    if (a.length < size) {
      a = Array.clonify(a, size);
    }
    for (int i = 0; i < size; ++i) {
      a[i] = getImpl(array, i);
    }
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  protected int indexOf(Object o, int index) {
    if (index < 0) {
      indexOutOfBounds(index);
    }
    for (; index < size; ++index) {
      if (equals(o, getImpl(array, index))) {
        return index;
      }
    }
    return -1;
  }

  protected int lastIndexOf(Object o, int index) {
    if (index >= size) {
      indexOutOfBounds(index);
    }
    for (; index >= 0; --index) {
      if (equals(o, getImpl(array, index))) {
        return index;
      }
    }
    return -1;
  }

  protected void removeRange(int fromIndex, int endIndex) {
    if (fromIndex < 0 || fromIndex >= size) {
      indexOutOfBounds(fromIndex);
    }
    if (endIndex < fromIndex || endIndex > size) {
      indexOutOfBounds(endIndex);
    }
    int count = endIndex - fromIndex;
    removeRangeImpl(array, fromIndex, count);
    size -= count;
  }

  /**
   * This function sets the size of the array, and is used by Vector.
   */
  protected void setSize(int newSize) {
    if (newSize < 0) {
      indexOutOfBounds(newSize);
    }
    setSizeImpl(array, newSize);
    // null fill any new slots if size < newSize
    for (; size < newSize; ++size) {
      setImpl(array, size, null);
    }
    // assignment necessary when size > newSize
    size = newSize;
  }

  private void clearImpl() {
    array = JavaScriptObject.createArray();
    size = 0;
  }
}
