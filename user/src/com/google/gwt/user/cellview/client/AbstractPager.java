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
   * The {@link PagingListView} being paged.
   */
  private final PagingListView<T> view;

  public AbstractPager(PagingListView<T> view) {
    this.view = view;
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
    int pageSize = view.getPageSize();
    return (view.getPageStart() + pageSize - 1) / pageSize;
  }

  /**
   * Get the number of pages based on the data size.
   * 
   * @return the page count
   */
  public int getPageCount() {
    int pageSize = view.getPageSize();
    return (view.getDataSize() + pageSize - 1) / pageSize;
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
    return view.getPageStart() + view.getPageSize() < view.getDataSize();
  }

  /**
   * Returns true if there is enough data to display a given number of
   * additional pages.
   */
  public boolean hasNextPages(int pages) {
    return view.getPageStart() + pages * view.getPageSize() < view.getDataSize();
  }

  /**
   * Returns true if there is enough data such that the specified page is within
   * range.
   */
  public boolean hasPage(int index) {
    return view.getPageSize() * index < view.getDataSize();
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   */
  public boolean hasPreviousPage() {
    return view.getPageStart() > 0 && view.getDataSize() > 0;
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
    setPageStart(view.getDataSize() - view.getPageSize());
  }

  /**
   * Advance the starting row by 'pageSize' rows.
   */
  public void nextPage() {
    setPageStart(view.getPageStart() + view.getPageSize());
  }

  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    if (isRangeLimited) {
      setPageStart(view.getPageStart());
    }
  }

  /**
   * Move the starting row back by 'pageSize' rows.
   */
  public void previousPage() {
    setPageStart(view.getPageStart() - view.getPageSize());
  }

  /**
   * Go to a specific page.
   * 
   * @param index the page index
   */
  public void setPage(int index) {
    if (!isRangeLimited || !view.isDataSizeExact() || hasPage(index)) {
      // We don't use the local version of setPageStart because the user
      // probably wants to use absolute page indexes.
      view.setPageStart(view.getPageSize() * index);
    }
  }

  /**
   * Set the page start index.
   * 
   * @param index the index
   */
  public void setPageStart(int index) {
    if (isRangeLimited && view.isDataSizeExact()) {
      index = Math.min(index, view.getDataSize() - view.getPageSize());
    }
    index = Math.max(0, index);
    if (index != view.getPageStart()) {
      view.setPageStart(index);
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
