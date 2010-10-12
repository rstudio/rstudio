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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Describes an object that displays a range of rows.
 */
public interface HasRows extends HasHandlers {

  /**
   * Add a {@link RangeChangeEvent.Handler}.
   *
   * @param handler the handler
   * @return a {@link HandlerRegistration} to remove the handler
   */
  HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler);

  /**
   * Add a {@link RowCountChangeEvent.Handler}.
   *
   * @param handler the handler
   * @return a {@link HandlerRegistration} to remove the handler
   */
  HandlerRegistration addRowCountChangeHandler(
      RowCountChangeEvent.Handler handler);

  /**
   * Get the total count of all rows.
   *
   * @return the total row count
   *
   * @see #setRowCount(int)
   */
  int getRowCount();

  /**
   * Get the range of visible rows.
   *
   * @return the visible range
   * 
   * @see #setVisibleRange(Range)
   * @see #setVisibleRange(int, int)
   */
  Range getVisibleRange();

  /**
   * Check if the total row count is exact, or an estimate.
   *
   * @return true if exact, false if an estimate
   */
  boolean isRowCountExact();

  /**
   * Set the exact total count of all rows. This method defers to
   * {@link #setRowCount(int, boolean)}.
   *
   * @param count the exact total count
   *
   * @see #getRowCount()
   */
  void setRowCount(int count);

  /**
   * Set the total count of all rows, specifying whether the count is exact or
   * an estimate.
   *
   * @param count the total count
   * @param isExact true if the count is exact, false if an estimate
   * @see #getRowCount()
   */
  void setRowCount(int count, boolean isExact);

  /**
   * Set the visible range or rows. This method defers to
   * {@link #setVisibleRange(Range)}.
   *
   * @param start the start index
   * @param length the length
   *
   * @see #getVisibleRange()
   */
  // TODO(jlabanca): Should we include setPageStart/Size as shortcut methods?
  void setVisibleRange(int start, int length);

  /**
   * Set the visible range or rows.
   *
   * @param range the visible range
   *
   * @see #getVisibleRange()
   */
  void setVisibleRange(Range range);
}
