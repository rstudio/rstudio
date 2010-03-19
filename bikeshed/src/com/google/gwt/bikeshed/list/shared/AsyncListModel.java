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

import java.util.List;

/**
 * An implementation of {@link AbstractListModel} that allows the data to be
 * modified.
 * 
 * @param <T> the data type of records in the list
 */
public abstract class AsyncListModel<T> extends AbstractListModel<T> {
  
  /**
   * Inform the views of the total number of items that are available.
   * 
   * @param size the new size
   * @param exact true if the size is exact, false if it is a guess
   */
  public void updateDataSize(int size, boolean exact) {
    super.updateDataSize(size, exact);
  }

  /**
   * Inform the views of the new data.
   * 
   * @param start the start index
   * @param length the length of the data
   * @param values the data values
   */
  public void updateViewData(int start, int length, List<T> values) {
    super.updateViewData(start, length, values);
  }
}
