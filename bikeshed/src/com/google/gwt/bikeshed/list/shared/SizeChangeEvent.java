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
package com.google.gwt.bikeshed.list.shared;

/**
 * Fired when the size of a list is known or changed.
 */
public class SizeChangeEvent {

  private final boolean exact;
  private final int size;

  /**
   * Creates a {@link SizeChangeEvent}.
   *
   * @param size the total size of the list
   * @param exact true if this is an exact size
   */
  public SizeChangeEvent(int size, boolean exact) {
    this.size = size;
    this.exact = exact;
  }

  /**
   * Get the length of the changed data set.
   *
   * @return the length of the data set
   */
  public int getSize() {
    return size;
  }

  /**
   * Check if the size is an exact size or an approximation.
   *
   * @return true if exact, false if approximation
   */
  public boolean isExact() {
    return exact;
  }
}
