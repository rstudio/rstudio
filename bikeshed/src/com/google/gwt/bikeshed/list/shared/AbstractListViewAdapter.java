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

import com.google.gwt.bikeshed.list.client.ListView;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base implementation of a data source for list views.
 *
 * @param <T> the data type of records in the list
 */
public abstract class AbstractListViewAdapter<T> implements ProvidesKey<T> {

  /**
   * The range of interest for a single handler.
   */
  public static class DefaultRange implements Range, Serializable {
    private int length;
    private int start;

    public DefaultRange(int start, int length) {
      this.start = start;
      this.length = length;
    }

    /**
     * Used by RPC.
     */
    DefaultRange() {
    }

    public int getLength() {
      return length;
    }

    public int getStart() {
      return start;
    }
  }

  private class Delegate implements ListView.Delegate<T> {
    public void onRangeChanged(ListView<T> view) {
      AbstractListViewAdapter.this.onRangeChanged(view);
    }
  }

  private final Delegate delegate = new Delegate();

  private Set<ListView<T>> views = new HashSet<ListView<T>>();

  /**
   * The provider of keys for list items.
   */
  private ProvidesKey<T> keyProvider;

  public void addView(ListView<T> view) {
    if (views.contains(view)) {
      throw new IllegalStateException("ListView already added");
    }
    views.add(view);
    view.setDelegate(delegate);
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
    for (ListView<T> view : views) {
      ranges[i++] = view.getRange();
    }
    return ranges;
  }

  public void removeView(ListView<T> view) {
    if (!views.contains(view)) {
      throw new IllegalStateException("ListView not present");
    }
    views.remove(view);
    view.setDelegate(null);
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
  protected abstract void onRangeChanged(ListView<T> view);

  /**
   * Inform the views of the total number of items that are available.
   *
   * @param size the new size
   * @param exact true if the size is exact, false if it is a guess
   */
  protected void updateDataSize(int size, boolean exact) {
    SizeChangeEvent event = new SizeChangeEvent(size, exact);
    for (ListView<T> view : views) {
      view.onSizeChanged(event);
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
    for (ListView<T> view : views) {
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
  protected void updateViewData(ListView<T> view, int start, int length, List<T> values) {
    int end = start + length;
    Range range = view.getRange();
    int curStart = range.getStart();
    int curLength = range.getLength();
    int curEnd = curStart + curLength;
    if (curStart < end && curEnd > start) {
      // Fire the handler with the data that is in the range.
      int realStart = curStart < start ? start : curStart;
      int realEnd = curEnd > end ? end : curEnd;
      int realLength = realEnd - realStart;
      List<T> realValues = values.subList(realStart - start, realStart
          - start + realLength);
      ListEvent<T> event = new ListEvent<T>(realStart, realLength, realValues);
      view.onDataChanged(event);
    }
  }
}
