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

import java.util.List;

/**
 * A view that can display a range of data.
 * 
 * @param <T> the data type of each row
 */
public interface HasData<T> extends HasRows, HasCellPreviewHandlers<T> {

  /**
   * Get the {@link SelectionModel} used by this {@link HasData}.
   * 
   * @return the {@link SelectionModel}
   * 
   * @see #setSelectionModel(SelectionModel)
   */
  SelectionModel<? super T> getSelectionModel();

  /**
   * Get the row value at the specified visible index. Index 0 corresponds to
   * the first item on the page.
   * 
   * @param indexOnPage the index on the page
   * @return the row value
   */
  T getVisibleItem(int indexOnPage);

  /**
   * Get the number of visible items being displayed. Note that this value might
   * be less than the page size if there is not enough data to fill the page.
   * 
   * @return the number of visible items on the page
   */
  int getVisibleItemCount();

  /**
   * Get an {@link Iterable} composed of all of the visible items.
   * 
   * @return an {@link Iterable} instance
   */
  Iterable<T> getVisibleItems();

  /**
   * <p>
   * Set a values associated with the rows in the visible range.
   * </p>
   * <p>
   * This method <i>does not</i> replace all rows in the display; it replaces
   * the row values starting at the specified start index through the length of
   * the the specified values. You must call {@link #setRowCount(int)} to set
   * the total number of rows in the display. You should also use
   * {@link #setRowCount(int)} to remove rows when the total number of rows
   * decreases.
   * </p>
   * 
   * @param start the start index of the data
   * @param values the values within the range
   */
  void setRowData(int start, List<? extends T> values);

  /**
   * Set the {@link SelectionModel} used by this {@link HasData}.
   * 
   * @param selectionModel the {@link SelectionModel}
   * 
   * @see #getSelectionModel()
   */
  void setSelectionModel(SelectionModel<? super T> selectionModel);

  /**
   * <p>
   * Set the visible range and clear the current visible data.
   * </p>
   * <p>
   * If the second argument <code>forceRangeChangeEvent</code> is true, a
   * {@link RangeChangeEvent} will be fired even if the range does not change.
   * If false, a {@link RangeChangeEvent} will only be fired if the range
   * changes.
   * </p>
   * 
   * @param range the new {@link Range}
   * @param forceRangeChangeEvent true to fire a {@link RangeChangeEvent} even
   *          if the {@link Range} doesn't change
   */
  void setVisibleRangeAndClearData(Range range, boolean forceRangeChangeEvent);
}
