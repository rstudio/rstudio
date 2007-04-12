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

/**
 * See Sun's JDK 1.4 documentation for documentation.   
 * 
 * Differences between this implementation and JDK 1.4 <code>ArrayList</code>
 * include capacity management and range checking.
 * <p>
 * <b>Capacity</b> There is no speed advantage to pre-allocating array sizes in
 * JavaScript, so this implementation does not include any of the capacity and
 * "growth increment" concepts in the standard ArrayList class. Although
 * <code>ArrayList(int)</code> accepts a value for the intitial capacity of
 * the array, this constructor simply delegates to <code>ArrayList()</code>.
 * It is only present for compatibility with JDK 1.4's API.
 * </p>
 * <p>
 * <b>Dual endedness</b> For increased performance, this implementation supports
 * constant time insertion and deletion from either end.
 * </p>
 */
public class ArrayList extends AbstractList implements List, Cloneable,
    RandomAccess {
  /*
   * Implementation notes:  
   *   Currently if one uses an ArrayList as a ring buffer, adding from one end,
   *   and deleting from the other, the indexes will increase (or decrease) 
   *   without ever being normalized.  Back of the envelope calculations 
   *   indicate that at 30 indexes per second, it will take a year of solid run
   *   time to chew through the billion indexes needed to get into trouble.
   *   Given that it seemed better not to rebalance than to charge everyone the
   *   extra code size the rebalancing code would represent. 
   */

  protected static boolean equals(Object a, Object b) {
    return (a == null ? b == null : a.equals(b));
  } 
  /** 
   * This field holds the javascript array, and is not private to avoid Eclipse
   * warnings. 
   */
  JavaScriptObject array;
  /** 
   * This field holds the last populated index of the array and is not private
   * to avoid Eclipse warnings.
   */
  int endIndex;
  /** 
   * This field holds the first populated index of the array and is not private
   * to avoid Eclipse warnings.
   */
  int startIndex;

  public ArrayList() {
    initArray();
  }

  public ArrayList(Collection c) {
    initArray();
    addAll(c);
  }

  /**
   * There is no speed advantage to pre-allocating array sizes in JavaScript,
   * so the <code>intialCapacity</code> parameter is ignored. This constructor is
   * only present for compatibility with JDK 1.4's API.
   */
  public ArrayList(int initialCapacity) {
    // initialCapacity is ignored in JS implementation; this constructor is
    // present for JDK 1.4 compatibility
    this();
  }

  public native void add(int index, Object o) /*-{
    var array  = this.@java.util.ArrayList::array;
    var endIndex = this.@java.util.ArrayList::endIndex;
    var startIndex = this.@java.util.ArrayList::startIndex;
    // This if is not an else if to call attention to the early return.
    if (index + startIndex == endIndex) {
      // If we are at the end simply set the next element to hold the value.
      array[endIndex] = o;
      this.@java.util.ArrayList::endIndex++;
      return;
    }
    if (index == 0) {
      // If we are adding at the beginning, simply set the new element, and 
      // move the beginning back.
      array[--this.@java.util.ArrayList::startIndex] = o;
      return;
    }
    
    // Somewhere in the middle, so do range checking and the splice.
    // Range checking, must be more permissive since one can add off the end.
    this.@java.util.ArrayList::verifyIndexOneExtra(I)(index);
    array.splice(index + startIndex, 0, o);
    // The end of the array moved forward if we got here so record that.
    this.@java.util.ArrayList::endIndex++;
  }-*/;

  public boolean add(Object o) {
    add(size(),o);
    return true;
  }
 
  public void clear() {
    setSize(0);
  }
  
  public Object clone() {
    return new ArrayList(this);
  }

  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }
 
  public native Object get(int index) /*-{
    this.@java.util.ArrayList::verifyIndex(I)(index);
    var startIndex = this.@java.util.ArrayList::startIndex;
    return this.@java.util.ArrayList::array[index + startIndex];
  }-*/;
  
  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  public native boolean isEmpty() /*-{
    return (this.@java.util.ArrayList::endIndex == this.@java.util.ArrayList::startIndex);
  }-*/;

  public int lastIndexOf(Object o) {
     return lastIndexOf(o, size() - 1);
  }

  public Object remove(int index) {
    Object old = get(index);
    removeRange(index,index + 1);
    return old;
  } 

  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    remove(i);
    return true;
  }

  public native Object set(int index, Object o) /*-{
    this.@java.util.ArrayList::verifyIndex(I)(index);
    var array = this.@java.util.ArrayList::array;
    var startIndex = this.@java.util.ArrayList::startIndex;
    var old = array[index + startIndex];
    array[index + startIndex] = o;
    return old;
  }-*/;

  public native int size() /*-{
    return this.@java.util.ArrayList::endIndex - this.@java.util.ArrayList::startIndex; 
  }-*/;
  
  protected native void removeRange(int fromIndex, int toIndex) /*-{
    this.@java.util.ArrayList::verifyIndexOneExtra(I)(fromIndex);
    this.@java.util.ArrayList::verifyIndexOneExtra(I)(toIndex);
    var array = this.@java.util.ArrayList::array;
    var startIndex = this.@java.util.ArrayList::startIndex;
    var endIndex = this.@java.util.ArrayList::endIndex;
    if (fromIndex == 0) {
      // Chop off the beginning.
      for (var i = startIndex; i < (toIndex + startIndex); i++) {
        delete array[i];
      }
      this.@java.util.ArrayList::startIndex += (toIndex - fromIndex);
    } else if (toIndex + startIndex == endIndex) {
      // Chop off the end.
      for (var i = (fromIndex + startIndex); i < endIndex; i++) {
        delete array[i];
      }
      this.@java.util.ArrayList::endIndex = (fromIndex + startIndex);
    } else {
      // Splice from the middle.
      var numToRemove = toIndex - fromIndex;
      array.splice(fromIndex + startIndex, numToRemove);
      this.@java.util.ArrayList::endIndex -= numToRemove;
    }
  }-*/;
  
  native int indexOf(Object o, int index) /*-{
    var array = this.@java.util.ArrayList::array;
    var startIndex = this.@java.util.ArrayList::startIndex;
    var i = index + startIndex;
    var endIndex = this.@java.util.ArrayList::endIndex;            
    while (i < endIndex) {
      if (@java.util.ArrayList::equals(Ljava/lang/Object;Ljava/lang/Object;)(array[i],o)) {
        return i - startIndex;
      }
      ++i;
    }
    return -1;
  }-*/;
  
  /**
   * Throws an <code>indexOutOfBoundsException</code>, and is not 
   * private to avoid eclipse warnings.
   */
  void indexOutOfBounds(int i) {
    throw new IndexOutOfBoundsException("Size: " + this.size() + " Index: " + i);
  }
  
  /**
   * Computes the last index of an element given an offset, and is
   * not private to avoid eclipse warnings.
   */
  native int lastIndexOf(Object o, int index) /*-{
    var array = this.@java.util.ArrayList::array;
    var startIndex = this.@java.util.ArrayList::startIndex;
    var i = index + startIndex;
    while (i >= startIndex) {
      if (@java.util.ArrayList::equals(Ljava/lang/Object;Ljava/lang/Object;)(array[i],o)) {
        return i - startIndex;
      }
      --i;
    }
    return -1;
  }-*/;

  /**
   * This function sets the size of the array, and is used in Vector.
   */
  native void setSize(int newSize) /*-{
    // Make sure to null fill any newly created slots (otherwise,
    // get() can return 'undefined').
    var endIndex = this.@java.util.ArrayList::endIndex;
    var startIndex = this.@java.util.ArrayList::startIndex;
    var array = this.@java.util.ArrayList::array;
    var newEnd = newSize + startIndex;
    for (var i = endIndex; i < newEnd; ++i) {
      array[i] = null;
    }

    // Also make sure to clean up orphaned slots (or we'll end up
    // leaving garbage uncollected).
    for (var i = endIndex - 1; i >= newEnd; --i) {
      delete array[i];
    }
    this.@java.util.ArrayList::endIndex = newEnd;
  }-*/;
  
  native void verifyIndex(int index) /*-{
    var endIndex = this.@java.util.ArrayList::endIndex;
    var startIndex = this.@java.util.ArrayList::startIndex;
    if (index < 0 || index + startIndex >= endIndex) {
      this.@java.util.ArrayList::indexOutOfBounds(I)(index);
    }
  }-*/;
  
  native void verifyIndexOneExtra(int index) /*-{
    var endIndex = this.@java.util.ArrayList::endIndex;
    var startIndex = this.@java.util.ArrayList::startIndex;
    if (index < 0 || index + startIndex > endIndex) {
      this.@java.util.ArrayList::indexOutOfBounds(I)(index);
    }
  }-*/;
 
  private native void initArray() /*-{
    this.@java.util.ArrayList::array = new Array();
    var HALFWAY_INDEX = 1000000000; // Halfway through the address space
    // Javascript arrays are sparse, so this wastes no space
    this.@java.util.ArrayList::startIndex = HALFWAY_INDEX;
    this.@java.util.ArrayList::endIndex = HALFWAY_INDEX;
  }-*/;
}
