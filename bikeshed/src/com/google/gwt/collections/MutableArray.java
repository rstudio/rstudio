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

import java.util.Arrays;

/**
 * An array whose content and length can change over time.
 * 
 * @param <E> The type stored in the array elements
 */
public class MutableArray<E> extends Array<E> {

  // TODO: refactor the unchecked elems construction into a separate method
  // The elements in the array
  E[] elems;

  // Tracks when this array is frozen (for assertion enforcement only)
  private boolean frozen;

  /**
   * Can only be constructed via {@link CollectionFactory}.
   */
  MutableArray() {
  }

  @ConstantTime
  public void add(E elem) {
    insert(size(), elem);
  }

  @ConstantTime
  public void clear() {
    Assertions.assertNotFrozen(this);
    elems = null;
  }
  
  /**
   * Creates an immutable array based on this one. Also marks this object as read-only. 
   * After calling {@code freeze()}, only use methods from {@link Array} or the returned 
   * {@link ImmutableArray} should be to access the elements 
   * of the array is preferred.
   */
  public ImmutableArray<E> freeze() {
    Assertions.markFrozen(this);

    ImmutableArray<E> r;
    if (elems != null) {
      r = new ImmutableArrayImpl<E>(elems);
    } else {
      r = ImmutableArray.getEmptyInstance();
    }
    
    return r;
  }

  @Override
  @ConstantTime
  public E get(int index) {
    Assertions.assertIndexInRange(index, 0, size());
    if (elems != null) {
      return elems[index];
    } else {
      return null;
    }
  }

  /**
   * Inserts {@code element} before the element residing at {@code index}.
   * 
   * @param index in the range [0, this.size()], inclusive; if index is equal
   *          to the array's current size, the result is equivalent to calling
   *          {@link #add(Object)}
   * @param elem the element to insert or {@code null}
   */
  @SuppressWarnings("unchecked")
  @LinearTime
  public void insert(int index, E elem) {
    Assertions.assertNotFrozen(this);
    Assertions.assertIndexInRange(index, 0, size() + 1);

    // TODO benchmark to see if we need to grow smartly (2x size?)
    // TODO benchmark to see if we need to consider single element arrays separately
    if (elems != null) {
      int oldLen = elems.length;
      E[] newElems = (E[]) new Object[oldLen + 1];
      System.arraycopy(elems, 0, newElems, 0, index);
      System.arraycopy(elems, index, newElems, index + 1, oldLen - index);
      newElems[index] = elem;
      elems = newElems;
    } else {
      elems = (E[]) (new Object[] {elem});
    }
  }

  /**
   * Removes the element at the specified index.
   */
  @SuppressWarnings("unchecked")
  @LinearTime
  public void remove(int index) {
    Assertions.assertNotFrozen(this);
    Assertions.assertIndexInRange(index, 0, size());
    if (elems != null && elems.length >= 1) {
      // TODO: replace with splice using JSNI
      int oldLen = elems.length;
      E[] newElems = (E[]) new Object[oldLen - 1];
      System.arraycopy(elems, 0, newElems, 0, index);
      System.arraycopy(elems, index + 1, newElems, index, oldLen - index - 1);
      elems = newElems;
    } else if (elems != null && elems.length == 1) {
      elems = null;
    } else {
      assert false : "index " + index + " in range [0, " + size() + "), but remove(int) failed";
    }
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
    if (elems != null) {
      elems[index] = elem;
    } else {
      assert false : "index " + index + " in range [0, " + size() + "), but set(int,E) failed";
    }
  }
  
  /**
   * Changes the array size. If {@code newSize} is less than the current size, the array is 
   * truncated. If {@code newSize} is greater than the current size the array is grown and
   * the new elements of the array filled up with {@code fillValue}.
   */
  @SuppressWarnings("unchecked")
  @LinearTime
  public void setSize(int newSize, E fillValue) {
    Assertions.assertNotFrozen(this);
    assert newSize >= 0 : "expecting newSize >= 0, got " + newSize;
    
    int fillStart;

    if (newSize == size()) {
      return;
    } else if (newSize == 0) {
      elems = null;
      return;
    }
    
    if (elems == null) {
      fillStart = 0;
    } else if (newSize < elems.length) {
      // nothing to fill
      fillStart = newSize;      
    } else {
      fillStart = elems.length;
    }
    
    E[] newElems = (E[]) new Object[newSize];
    
    if (fillStart != 0) {
      System.arraycopy(elems, 0, newElems, 0, fillStart);
    }
    
    Arrays.fill(newElems, fillStart, newSize, fillValue);
    
    elems = newElems;
  }

  @Override
  @ConstantTime
  public int size() {
    if (elems != null) {
      return elems.length;
    } else {
      return 0;
    }
  }

  // Only meant to be called from within Assertions
  boolean isFrozen() {
    return frozen;
  }

  // Only meant to be called from within Assertions
  void markFrozen() {
    frozen = true;
  }
}
