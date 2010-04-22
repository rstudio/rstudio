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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SelectionModel;

import java.util.List;

/**
 * A list view.
 * 
 * @param <T> the data type of each row
 */
public interface ListView<T> {

  /**
   * A list view delegate, implemented by classes that supply data to a view.
   * 
   * @param <T> the data type of each row
   */
  public interface Delegate<T> {
    void onRangeChanged(ListView<T> listView);
  }

  void setDelegate(Delegate<T> delegate);

  Range getRange();

  /**
   * Set a range of data in the view.
   * 
   * @param start the start index of the data
   * @param length the length of the data
   * @param values the values within the range
   */
  void setData(int start, int length, List<T> values);

  void setDataSize(int size, boolean isExact);

  /**
   * Set the {@link SelectionModel} used by this {@link ListView}.
   * 
   * @param selectionModel the {@link SelectionModel}
   */
  void setSelectionModel(SelectionModel<? super T> selectionModel);
}
