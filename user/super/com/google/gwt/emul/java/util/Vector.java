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
 * Differences include capacity management and range checking.
 * <p>
 * <b>Capacity</b> There is no speed advantage to pre-allocating array sizes in
 * JavaScript, so this implementation does not include any of the capacity and
 * "growth increment" concepts in the standard Vector class.
 * </p>
 * <p>
 * <b>Range checking</b> For increased performance, this implementation does
 * not check for index validity.
 * </p>
 */
public class Vector extends AbstractList implements List, Cloneable,
    RandomAccess {

  protected static boolean equals(Object a, Object b) {
    return (a == null ? b == null : a.equals(b));
  }

  public Vector() {
    initArray();
  }

  public Vector(Collection c) {
    initArray();
    addAll(c);
  }

  public native void add(int index, Object o) /*-{
    var a = this.array;
    this.array = a.slice(0, index).concat(o, a.slice(index)); 
  }-*/;

  public native boolean add(Object o) /*-{
    var a = this.array;
    a[a.length] = o;
    return true; 
  }-*/;

  public boolean addAll(Collection c) {
    return super.addAll(c);
  }

  public boolean addAll(int index, Collection c) {
    return super.addAll(index, c);
  }

  public void addElement(Object o) {
    add(o);
  }

  public native void clear() /*-{
    this.array.length = 0;
  }-*/;

  public Object clone() {
    return new Vector(this);
  }

  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }

  public boolean containsAll(Collection c) {
    return super.containsAll(c);
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

  public boolean equals(Object o) {
    return super.equals(o);
  }

  public Object firstElement() {
    return get(0);
  }

  public Object get(int index) {
    if ((index < 0) || (index >= size())) {
      throw new NoSuchElementException();
    }
    return _get(index);
  }

  public int hashCode() {
    return super.hashCode();
  }

  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  public native int indexOf(Object o, int startIndex) /*-{
    var a = this.array;
    var i = startIndex-1;
    var n = a.length;
    while (++i < n) {
      if (@java.util.Vector::equals(Ljava/lang/Object;Ljava/lang/Object;)(a[i],o))
        return i;
    }
    return -1;
  }-*/;

  public void insertElementAt(Object o, int index) {
    add(index, o);
  }

  public native boolean isEmpty() /*-{
    return (this.array.length == 0);
  }-*/;

  public Object lastElement() {
    if (isEmpty()) {
      throw new NoSuchElementException("last");
    } else {
      return get(size() - 1);
    }
  }

  public int lastIndexOf(Object o) {
    return lastIndexOf(o, size() - 1);
  }

  public native int lastIndexOf(Object o, int startIndex) /*-{
    var a = this.array;
    var i = startIndex;
    while (i >= 0) {
      if (@java.util.Vector::equals(Ljava/lang/Object;Ljava/lang/Object;)(a[i],o))
        return i;
      --i;
    }
    return -1;
  }-*/;

  public native Object remove(int index) /*-{
    var a = this.array;
    var old = a[index];
    this.array = a.slice(0, index).concat(a.slice(index+1));
    return old; 
  }-*/;

  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    remove(i);
    return true;
  }

  public boolean removeAll(Collection c) {
    return super.removeAll(c);
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

  public boolean retainAll(Collection c) {
    return super.retainAll(c);
  }

  public native Object set(int index, Object o) /*-{
    var a = this.array;
    var old = a[index];
    a[index] = o;
    return old;
  }-*/;

  public void setElementAt(Object o, int index) {
    set(index, o);
  }

  public native void setSize(int newSize) /*-{
    // Make sure to null-fill any newly created slots (otherwise,
    // get() can return 'undefined').
    for (var i = this.array.length; i < newSize; ++i)
      this.array[i] = null;

    // Also make sure to clean up orphaned slots (or we'll end up
    // leaving garbage uncollected).
    for (var i = this.array.length - 1; i >= newSize; --i)
      delete this.array[i];

    this.array.length = newSize;
  }-*/;

  public native int size() /*-{
    return this.array.length; 
  }-*/;

  public Object[] toArray() {
    return super.toArray();
  }

  public String toString() {
    return super.toString();
  }

  protected native void removeRange(int fromIndex, int endIndex) /*-{
    var a = this.array;
    this.array = a.slice(0, fromIndex).concat(a.slice(endIndex));
  }-*/;

  // CHECKSTYLE_OFF: Underscore prefix is an old convention that could be cleaned up.
  private native Object _get(int index) /*-{
    return this.array[index];
  }-*/;
  // CHECKSTYLE_ON

  private native void initArray() /*-{
    this.array = new Array(); 
  }-*/;

}
