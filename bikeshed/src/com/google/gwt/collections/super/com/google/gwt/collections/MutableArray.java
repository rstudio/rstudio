/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.collections;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * An array whose content and length can change over time. This implementation
 * is used in web mode.
 * 
 * @param <E> The type stored in the array elements
 */
public class MutableArray<E> extends Array<E> {

  JsArray elems;

  private boolean frozen;

  /**
   * Can only be constructed via {@link CollectionFactory}.
   */
  MutableArray() {
    elems = (JsArray) JavaScriptObject.createArray();
  }

  @ConstantTime
  public void add(E elem) {
    Assertions.assertNotFrozen(this);
    jsniAdd(elem);
  }

  @ConstantTime
  public void clear() {
    Assertions.assertNotFrozen(this);
    elems.setLength(0);
  }

  /**
   * Creates an immutable array based on this one. Also marks this object as
   * read-only. After calling {@code freeze()}, only use methods from
   * {@link Array} or the returned {@link ImmutableArray} should be to access
   * the elements of the array is preferred.
   */
  public ImmutableArray<E> freeze() {
    Assertions.markFrozen(this);
    if (size() != 0) {
      return new ImmutableArrayImpl(elems);
    } else {
      return ImmutableArray.getEmptyInstance();
    }
  }

  @Override
  @ConstantTime
  public E get(int index) {
    Assertions.assertIndexInRange(index, 0, size());
    return jsniGet(index);
  }

  /**
   * Inserts {@code element} before the element residing at {@code index}.
   * 
   * @param index in the range [0, this.size()], inclusive; if index is equal to
   *          the array's current size, the result is equivalent to calling
   *          {@link #add(Object)}
   * @param elem the element to insert or {@code null}
   */
  @LinearTime
  public void insert(int index, E elem) {
    Assertions.assertNotFrozen(this);
    Assertions.assertIndexInRange(index, 0, size() + 1);
    jsniInsert(index, elem);
  }

  /**
   * Removes the element at the specified index.
   */
  @LinearTime
  public void remove(int index) {
    Assertions.assertNotFrozen(this);
    Assertions.assertIndexInRange(index, 0, size());
    jsniRemove(index);
  }

  /**
   * Replaces the element at the specified index.
   * 
   * @param index in the range [0, this.size()), exclusive
   * @param elem the element to insert or {@code null}
   */
  @ConstantTime
  public void set(int index, E elem) {
    Assertions.assertNotFrozen(this);
    Assertions.assertIndexInRange(index, 0, size());

    jsniSet(index, elem);
  }

  /**
   * Changes the array size. If {@code newSize} is less than the current size,
   * the array is truncated. If {@code newSize} is greater than the current size
   * the array is grown and the new elements of the array filled up with {@code
   * fillValue}.
   */
  @LinearTime
  public void setSize(int newSize, E fillValue) {
    Assertions.assertNotFrozen(this);
    jsniSetSize(newSize, fillValue);
  }

  @Override
  @ConstantTime
  public int size() {
    return elems.length();
  }

  // Only meant to be called from within Assertions
  boolean isFrozen() {
    return frozen;
  }

  // Only meant to be called from within Assertions
  void markFrozen() {
    frozen = true;
  }

  private Object[] copyNativeArray(JsArray jsElems) {
    if (jsElems == null) {
      return null;
    }

    Object[] jreElems = new Object[jsElems.length()];

    for (int i = 0; i < jsElems.length(); ++i) {
      jreElems[i] = jsElems.get(i);
    }
    return jreElems;
  }

  @ConstantTime
  private native void jsniAdd(E elem) /*-{
    this.@com.google.gwt.collections.MutableArray::elems.push(elem);
  }-*/;

  @ConstantTime
  private native E jsniGet(int index) /*-{
    return this.@com.google.gwt.collections.MutableArray::elems[index];
  }-*/;

  /**
   * Inserts {@code element} before the element residing at {@code index}.
   * 
   * @param index in the range [0, this.size()], inclusive; if index is equal to
   *          the array's current size, the result is equivalent to calling
   *          {@link #add(Object)}
   * @param elem the element to insert or {@code null}
   */
  @LinearTime
  private native void jsniInsert(int index, E elem) /*-{
    this.@com.google.gwt.collections.MutableArray::elems.splice(index, 0, elem);
  }-*/;

  /**
   * Removes the element at the specified index.
   */
  @LinearTime
  private native void jsniRemove(int index) /*-{
    this.@com.google.gwt.collections.MutableArray::elems.splice(index, 1);
  }-*/;

  /**
   * Replaces the element at the specified index.
   * 
   * @param index in the range [0, this.size()), exclusive
   * @param elem the element to insert or {@code null}
   */
  @ConstantTime
  private native void jsniSet(int index, E elem) /*-{
    this.@com.google.gwt.collections.MutableArray::elems[index] = elem;
  }-*/;

  /**
   * Changes the array size. If {@code newSize} is less than the current size,
   * the array is truncated. If {@code newSize} is greater than the current size
   * the array is grown and the new elements of the array filled up with {@code
   * fillValue}.
   */
  @LinearTime
  private native void jsniSetSize(int newSize, E fillValue) /*-{
    var fillStart;
    var i;

    fillStart = this.@com.google.gwt.collections.MutableArray::elems.length;

    this.@com.google.gwt.collections.MutableArray::elems.length = newSize;

    if (fillValue != null) {
      for (i = fillStart; i < newSize; ++i) {
        this.@com.google.gwt.collections.MutableArray::elems[i] = fillValue;
      }
    }
  }-*/;

}
