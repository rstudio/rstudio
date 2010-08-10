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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base implementation of a data source for list views.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <T> the data type of records in the list
 */
// TODO(jlabanca): Rename for AbstractListViewAdapter to something better.
public abstract class AbstractListViewAdapter<T> implements ProvidesKey<T> {

  /**
   * The provider of keys for list items.
   */
  private ProvidesKey<T> keyProvider;

  /**
   * The last data size.
   */
  private int lastDataSize = -1;

  /**
   * Indicates whether or not the last data size is exact.
   */
  private boolean lastDataSizeExact;

  /**
   * A mapping of {@link HasData}s to their handlers.
   */
  private Map<HasData<T>, HandlerRegistration> rangeChangeHandlers =
      new HashMap<HasData<T>, HandlerRegistration>();

  private Set<HasData<T>> views = new HashSet<HasData<T>>();

  /**
   * Adds a view to this adapter. The current range of interest of the view will
   * be populated with data.
   *
   * @param view a {@link HasData}.
   */
  // TODO(jlabanca): Stop using the term view to describe HasData.
  public void addView(final HasData<T> view) {
    if (view == null) {
      throw new IllegalArgumentException("view cannot be null");
    } else if (views.contains(view)) {
      throw new IllegalStateException(
          "The specified view has already been added to this adapter.");
    }

    // Add the view to the set.
    views.add(view);

    // Add a handler to the view.
    HandlerRegistration handler = view.addRangeChangeHandler(
        new RangeChangeEvent.Handler() {
          public void onRangeChange(RangeChangeEvent event) {
            AbstractListViewAdapter.this.onRangeChanged(view);
          }
        });
    rangeChangeHandlers.put(view, handler);

    // Update the data size in the view.
    if (lastDataSize >= 0) {
      view.setRowCount(lastDataSize, lastDataSizeExact);
    }

    // Initialize the view with the current range.
    onRangeChanged(view);
  }

  /**
   * Get the key for a list item. The default implementation returns the item
   * itself.
   *
   * @param item the list item
   * @return the key that represents the item
   */
  public Object getKey(T item) {
    return keyProvider == null ? item : keyProvider.getKey(item);
  }

  /**
   * Get the {@link ProvidesKey} that provides keys for list items.
   *
   * @return the {@link ProvidesKey}
   */
  public ProvidesKey<T> getKeyProvider() {
    return keyProvider;
  }

  /**
   * Get the current ranges of all views.
   *
   * @return the ranges
   */
  public Range[] getRanges() {
    Range[] ranges = new Range[views.size()];
    int i = 0;
    for (HasData<T> view : views) {
      ranges[i++] = view.getVisibleRange();
    }
    return ranges;
  }

  /**
   * Get the set of views currently assigned to this adapter.
   *
   * @return the set of {@link HasData}
   */
  public Set<HasData<T>> getViews() {
    return Collections.unmodifiableSet(views);
  }

  public void removeView(HasData<T> view) {
    if (!views.contains(view)) {
      throw new IllegalStateException("ListView not present");
    }
    views.remove(view);

    // Remove the handler.
    HandlerRegistration handler = rangeChangeHandlers.remove(view);
    handler.removeHandler();
  }

  /**
   * Set the {@link ProvidesKey} that provides keys for list items.
   *
   * @param keyProvider the {@link ProvidesKey}
   */
  public void setKeyProvider(ProvidesKey<T> keyProvider) {
    this.keyProvider = keyProvider;
  }

  /**
   * Called when a view changes its range of interest.
   *
   * @param view the view whose range has changed
   */
  protected abstract void onRangeChanged(HasData<T> view);

  /**
   * Inform the views of the total number of items that are available.
   *
   * @param size the new size
   * @param exact true if the size is exact, false if it is a guess
   */
  protected void updateDataSize(int size, boolean exact) {
    lastDataSize = size;
    lastDataSizeExact = exact;
    for (HasData<T> view : views) {
      view.setRowCount(size, exact);
    }
  }

  /**
   * Inform the views of the new data.
   *
   * @param start the start index
   * @param length the length of the data
   * @param values the data values
   */
  protected void updateViewData(int start, int length, List<T> values) {
    for (HasData<T> view : views) {
      updateViewData(view, start, length, values);
    }
  }

  /**
   * Informs a single view of new data.
   *
   * @param view the view to be updated
   * @param start the start index
   * @param length the length of the data
   * @param values the data values
   */
  protected void updateViewData(
      HasData<T> view, int start, int length, List<T> values) {
    int end = start + length;
    Range range = view.getVisibleRange();
    int curStart = range.getStart();
    int curLength = range.getLength();
    int curEnd = curStart + curLength;
    if (curStart < end && curEnd > start) {
      // Fire the handler with the data that is in the range.
      int realStart = curStart < start ? start : curStart;
      int realEnd = curEnd > end ? end : curEnd;
      int realLength = realEnd - realStart;
      List<T> realValues = values.subList(
          realStart - start, realStart - start + realLength);
      view.setRowValues(realStart, realValues);
    }
  }
}
