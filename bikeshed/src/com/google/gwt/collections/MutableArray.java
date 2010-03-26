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

/**
 * An array whose content and length can change over time.
 * 
 * @param <E> The type stored in the array elements
 */
public class MutableArray<E> extends Array<E> {

  // The elements in the array
  private E[] elems;

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
    elems = null;
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
    Assertions.assertIndexInRange(index, 0, size());
    if (elems != null && elems.length >= 1) {
      // TODO: replace with splice using JSNI
      int oldLen = elems.length;
      E[] newElems = (E[]) new Object[oldLen - 1];
      System.arraycopy(elems, 0, newElems, 0, index);
      System.arraycopy(elems, index + 1, newElems, index, oldLen - index - 1);
      elems = newElems;
    } else if (elems.length == 1) {
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
    Assertions.assertIndexInRange(index, 0, size());
    if (elems != null) {
      elems[index] = elem;
    } else {
      assert false : "index " + index + " in range [0, " + size() + "), but set(int,E) failed";
    }
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
