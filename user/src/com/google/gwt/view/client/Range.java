/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.view.client;

import java.io.Serializable;

/**
 * The range of interest for a single handler.
 */
public class Range implements Serializable {

  private int length;
  private int start;

  /**
   * Construct a new {@link Range}.
   *
   * @param start the start index
   * @param length the length
   */
  public Range(int start, int length) {
    this.start = start;
    this.length = length;
  }

  /**
   * Used by RPC.
   */
  Range() {
  }

  /**
   * Return true if this ranges's start end length are equal to those of
   * the given object.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Range)) {
      return false;
    }
    Range r = (Range) o;
    return start == r.getStart() && length == r.getLength();
  }

  /**
   * Get the length of the range.
   *
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * Get the start index of the range.
   *
   * @return the start index
   */
  public int getStart() {
    return start;
  }

  /**
   * Return a hash code based on this range's start and length.
   */
  @Override
  public int hashCode() {
    return (length * 31) ^ start;
  }

  /**
   * Returns a String representation for debugging.
   */
  @Override
  public String toString() {
    return "Range(" + start + "," + length + ")";
  }
}
