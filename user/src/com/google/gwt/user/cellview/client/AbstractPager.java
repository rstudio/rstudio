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
package com.google.gwt.user.cellview.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.PagingListView.Pager;

/**
 * An abstract pager that exposes many methods useful for paging.
 * 
 * @param <T> the type of the PagingListView being controlled
 */
public abstract class AbstractPager<T> extends Composite implements Pager<T> {

  /**
   * If true, all operations should be limited to the data size.
   */
  private boolean isRangeLimited = true;

  /**
   * The last data size.
   */
  private int lastDataSize;

  /**
   * The {@link PagingListView} being paged.
   */
  private final PagingListView<T> view;

  public AbstractPager(PagingListView<T> view) {
    this.view = view;
    this.lastDataSize = view.getDataSize();
    view.setPager(this);
  }

  /**
   * Go to the first page.
   */
  public void firstPage() {
    setPage(0);
  }

  /**
   * Get the current page index.
   * 
   * Since the page start index can be set to any value, its possible to be
   * between pages. In this case, the return value is the number of times
   * {@link #previousPage()} can be called.
   * 
   * @return the page index
   */
  public int getPage() {
    Range range = view.getRange();
    int pageSize = range.getLength();
    return (range.getStart() + pageSize - 1) / pageSize;
  }

  /**
   * Get the number of pages based on the data size.
   * 
   * @return the page count
   */
  public int getPageCount() {
    int pageSize = getPageSize();
    return (view.getDataSize() + pageSize - 1) / pageSize;
  }

  /**
   * Get the page size.
   * 
   * @return the page size
   */
  public int getPageSize() {
    return view.getRange().getLength();
  }

  /**
   * Get the page start index.
   * 
   * @return the page start index
   */
  public int getPageStart() {
    return view.getRange().getStart();
  }

  /**
   * Get the {@link PagingListView} being paged.
   * 
   * @return the {@link PagingListView}
   */
  public PagingListView<T> getPagingListView() {
    return view;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #nextPage()} will succeed in moving the starting point of the table
   * forward.
   */
  public boolean hasNextPage() {
    if (!view.isDataSizeExact()) {
      return true;
    }
    Range range = view.getRange();
    return range.getStart() + range.getLength() < view.getDataSize();
  }

  /**
   * Returns true if there is enough data to display a given number of
   * additional pages.
   */
  public boolean hasNextPages(int pages) {
    Range range = view.getRange();
    return range.getStart() + pages * range.getLength() < view.getDataSize();
  }

  /**
   * Returns true if there is enough data such that the specified page is within
   * range.
   */
  public boolean hasPage(int index) {
    return getPageSize() * index < view.getDataSize();
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   */
  public boolean hasPreviousPage() {
    return getPageStart() > 0 && view.getDataSize() > 0;
  }

  /**
   * Returns true if there is enough data to display a given number of previous
   * pages.
   */
  public boolean hasPreviousPages(int pages) {
    Range range = view.getRange();
    return (pages - 1) * range.getLength() < range.getStart();
  }

  /**
   * Check if the page should be limited to the actual data size. Defaults to
   * true.
   * 
   * @return true if the range is limited to the data size
   */
  public boolean isRangeLimited() {
    return isRangeLimited;
  }

  /**
   * Go to the last page.
   */
  public void lastPage() {
    setPage(getPageCount() - 1);
  }

  /**
   * Set the page start to the last index that will still show a full page.
   */
  public void lastPageStart() {
    setPageStart(view.getDataSize() - getPageSize());
  }

  /**
   * Advance the starting row by 'pageSize' rows.
   */
  public void nextPage() {
    Range range = view.getRange();
    setPageStart(range.getStart() + range.getLength());
  }

  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    int oldDataSize = lastDataSize;
    lastDataSize = listView.getDataSize();

    // If the data size has changed, limit the range. If the page start or size
    // was changed through the pager, it will already be limited.
    if (isRangeLimited && oldDataSize != lastDataSize) {
      setPageStart(getPageStart());
    }
  }

  /**
   * Move the starting row back by 'pageSize' rows.
   */
  public void previousPage() {
    Range range = view.getRange();
    setPageStart(range.getStart() - range.getLength());
  }

  /**
   * Go to a specific page.
   * 
   * @param index the page index
   */
  public void setPage(int index) {
    if (!isRangeLimited || !view.isDataSizeExact() || hasPage(index)) {
      // We don't use the local version of setPageStart because it would
      // constrain the index, but the user probably wants to use absolute page
      // indexes.
      int pageSize = getPageSize();
      view.setRange(pageSize * index, pageSize);
    }
  }

  /**
   * Set the page size of the view.
   * 
   * @param pageSize the new page size
   */
  public void setPageSize(int pageSize) {
    Range range = view.getRange();
    int pageStart = range.getStart();
    if (isRangeLimited && view.isDataSizeExact()) {
      pageStart = Math.min(pageStart, view.getDataSize() - pageSize);
    }
    pageStart = Math.max(0, pageStart);
    view.setRange(pageStart, pageSize);
  }

  /**
   * Set the page start index.
   * 
   * @param index the index
   */
  public void setPageStart(int index) {
    Range range = view.getRange();
    int pageSize = range.getLength();
    if (isRangeLimited && view.isDataSizeExact()) {
      index = Math.min(index, view.getDataSize() - pageSize);
    }
    index = Math.max(0, index);
    if (index != range.getStart()) {
      view.setRange(index, pageSize);
    }
  }

  /**
   * Set whether or not the page range should be limited to the actual data
   * size. If true, all operations will adjust so that there is always data
   * visible on the page.
   * 
   * @param isRangeLimited
   */
  public void setRangeLimited(boolean isRangeLimited) {
    this.isRangeLimited = isRangeLimited;
  }
}
