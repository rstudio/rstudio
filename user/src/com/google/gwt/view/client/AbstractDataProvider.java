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
 * A base implementation of a data source for {@link HasData} implementations.
 *
 * @param <T> the data type of records in the list
 */
public abstract class AbstractDataProvider<T> implements ProvidesKey<T> {

  private Set<HasData<T>> displays = new HashSet<HasData<T>>();

  /**
   * The provider of keys for list items.
   */
  private final ProvidesKey<T> keyProvider;

  /**
   * The last row count.
   */
  private int lastRowCount = -1;

  /**
   * Indicates whether or not the last row count is exact.
   */
  private boolean lastRowCountExact;

  /**
   * A mapping of {@link HasData}s to their handlers.
   */
  private Map<HasData<T>, HandlerRegistration> rangeChangeHandlers =
      new HashMap<HasData<T>, HandlerRegistration>();
  
  /**
   * Construct an AbstractDataProvider without a key provider.
   */
  protected AbstractDataProvider() {
    this.keyProvider = null;
  }
  
  /**
   * Construct an AbstractDataProvider with a given key provider.
   * 
   * @param keyProvider a {@link ProvidesKey} object
   */
  protected AbstractDataProvider(ProvidesKey<T> keyProvider) {
    this.keyProvider = keyProvider;
  }

  /**
   * Adds a data display to this adapter. The current range of interest of the
   * display will be populated with data.
   *
   * @param display a {@link HasData}.
   */
  public void addDataDisplay(final HasData<T> display) {
    if (display == null) {
      throw new IllegalArgumentException("display cannot be null");
    } else if (displays.contains(display)) {
      throw new IllegalStateException(
          "The specified display has already been added to this adapter.");
    }

    // Add the display to the set.
    displays.add(display);

    // Add a handler to the display.
    HandlerRegistration handler = display.addRangeChangeHandler(
        new RangeChangeEvent.Handler() {
          public void onRangeChange(RangeChangeEvent event) {
            AbstractDataProvider.this.onRangeChanged(display);
          }
        });
    rangeChangeHandlers.put(display, handler);

    // Update the data size in the display.
    if (lastRowCount >= 0) {
      display.setRowCount(lastRowCount, lastRowCountExact);
    }

    // Initialize the display with the current range.
    onRangeChanged(display);
  }

  /**
   * Get the set of displays currently assigned to this adapter.
   *
   * @return the set of {@link HasData}
   */
  public Set<HasData<T>> getDataDisplays() {
    return Collections.unmodifiableSet(displays);
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
   * Get the current ranges of all displays.
   *
   * @return the ranges
   */
  public Range[] getRanges() {
    Range[] ranges = new Range[displays.size()];
    int i = 0;
    for (HasData<T> display : displays) {
      ranges[i++] = display.getVisibleRange();
    }
    return ranges;
  }

  /**
   * Remove the given data display.
   * 
   * @param display a {@link HasData} instance
   * 
   * @throws IllegalStateException if the display is not present
   */
  public void removeDataDisplay(HasData<T> display) {
    if (!displays.contains(display)) {
      throw new IllegalStateException("HasData not present");
    }
    displays.remove(display);

    // Remove the handler.
    HandlerRegistration handler = rangeChangeHandlers.remove(display);
    handler.removeHandler();
  }

  /**
   * Called when a display changes its range of interest.
   *
   * @param display the display whose range has changed
   */
  protected abstract void onRangeChanged(HasData<T> display);

  /**
   * Inform the displays of the total number of items that are available.
   *
   * @param count the new total row count
   * @param exact true if the count is exact, false if it is an estimate
   */
  protected void updateRowCount(int count, boolean exact) {
    lastRowCount = count;
    lastRowCountExact = exact;

    for (HasData<T> display : displays) {
      display.setRowCount(count, exact);
    }
  }

  /**
   * Inform the displays of the new data.
   *
   * @param start the start index
   * @param values the data values
   */
  protected void updateRowData(int start, List<T> values) {
    for (HasData<T> display : displays) {
      updateRowData(display, start, values);
    }
  }

  /**
   * Informs a single display of new data.
   *
   * @param display the display to be updated
   * @param start the start index
   * @param values the data values
   */
  protected void updateRowData(HasData<T> display, int start, List<T> values) {
    int end = start + values.size();
    Range range = display.getVisibleRange();
    int curStart = range.getStart();
    int curLength = range.getLength();
    int curEnd = curStart + curLength;
    if (start == curStart || (curStart < end && curEnd > start)) {
      // Fire the handler with the data that is in the range.
      // Allow an empty list that starts on the page start.
      int realStart = curStart < start ? start : curStart;
      int realEnd = curEnd > end ? end : curEnd;
      int realLength = realEnd - realStart;
      List<T> realValues = values.subList(
          realStart - start, realStart - start + realLength);
      display.setRowData(realStart, realValues);
    }
  }
}
