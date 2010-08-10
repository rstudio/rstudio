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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;

/**
 * An abstract pager that exposes many methods useful for paging.
 */
public abstract class AbstractPager extends Composite {

  /**
   * If true, all operations should be limited to the data size.
   */
  private boolean isRangeLimited = true;

  /**
   * The last row count.
   */
  private int lastRowCount;

  private HandlerRegistration rangeChangeHandler;
  private HandlerRegistration rowCountChangeHandler;
  private HasRows view;

  /**
   * Get the page size.
   *
   * @return the page size, or -1 if the view is not set
   */
  public int getPageSize() {
    return view == null ? -1 : view.getVisibleRange().getLength();
  }

  /**
   * Get the page start index.
   *
   * @return the page start index, or -1 if the view is not set
   */
  public int getPageStart() {
    return view == null ? -1 : view.getVisibleRange().getStart();
  }

  /**
   * Get the {@link HasRows} being paged.
   *
   * @return the {@link HasRows}
   */
  public HasRows getView() {
    return view;
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
   * Set whether or not the page range should be limited to the actual data
   * size. If true, all operations will adjust so that there is always data
   * visible on the page.
   *
   * @param isRangeLimited true to limit the range, false not to
   */
  public void setRangeLimited(boolean isRangeLimited) {
    this.isRangeLimited = isRangeLimited;
  }

  /**
   * Set the {@link HasRows} to be paged.
   *
   * @param view the {@link HasRows}
   */
  public void setView(HasRows view) {
    // Remove the old view.
    if (rangeChangeHandler != null) {
      rangeChangeHandler.removeHandler();
      rangeChangeHandler = null;
    }
    if (rowCountChangeHandler != null) {
      rowCountChangeHandler.removeHandler();
      rangeChangeHandler = null;
    }

    // Set the new view.
    this.view = view;
    if (view != null) {
      rangeChangeHandler = view.addRangeChangeHandler(
          new RangeChangeEvent.Handler() {
            public void onRangeChange(RangeChangeEvent event) {
              if (AbstractPager.this.view != null) {
                onRangeOrRowCountChanged();
              }
            }
          });
      rowCountChangeHandler = view.addRowCountChangeHandler(
          new RowCountChangeEvent.Handler() {
            public void onRowCountChange(RowCountChangeEvent event) {
              if (AbstractPager.this.view != null) {
                handleRowCountChange(
                    event.getNewRowCount(), event.isNewRowCountExact());
              }
            }
          });

      // Initialize the pager.
      onRangeOrRowCountChanged();
    }
  }

  /**
   * Go to the first page.
   */
  protected void firstPage() {
    setPage(0);
  }

  /**
   * <p>
   * Get the current page index.
   * </p>
   * <p>
   * Since the page start index can be set to any value, its possible to be
   * between pages. In this case, the return value is the number of times
   * {@link #previousPage()} can be called.
   * </p>
   *
   * @return the page index, or -1 if the view is not set
   */
  protected int getPage() {
    if (view == null) {
      return -1;
    }
    Range range = view.getVisibleRange();
    int pageSize = range.getLength();
    return (range.getStart() + pageSize - 1) / pageSize;
  }

  /**
   * Get the number of pages based on the data size.
   *
   * @return the page count, or -1 if the view is not set
   */
  protected int getPageCount() {
    if (view == null) {
      return -1;
    }
    int pageSize = getPageSize();
    return (view.getRowCount() + pageSize - 1) / pageSize;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #nextPage()} will succeed in moving the starting point of the table
   * forward.
   */
  protected boolean hasNextPage() {
    if (view == null) {
      return false;
    } else if (!view.isRowCountExact()) {
      return true;
    }
    Range range = view.getVisibleRange();
    return range.getStart() + range.getLength() < view.getRowCount();
  }

  /**
   * Returns true if there is enough data to display a given number of
   * additional pages.
   */
  protected boolean hasNextPages(int pages) {
    if (view == null) {
      return false;
    }
    Range range = view.getVisibleRange();
    return range.getStart() + pages * range.getLength() < view.getRowCount();
  }

  /**
   * Returns true if there is enough data such that the specified page is within
   * range.
   */
  protected boolean hasPage(int index) {
    return view == null ? false : getPageSize() * index < view.getRowCount();
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   */
  protected boolean hasPreviousPage() {
    return view == null ? false : getPageStart() > 0 && view.getRowCount() > 0;
  }

  /**
   * Returns true if there is enough data to display a given number of previous
   * pages.
   */
  protected boolean hasPreviousPages(int pages) {
    if (view == null) {
      return false;
    }
    Range range = view.getVisibleRange();
    return (pages - 1) * range.getLength() < range.getStart();
  }

  /**
   * Go to the last page.
   */
  protected void lastPage() {
    setPage(getPageCount() - 1);
  }

  /**
   * Set the page start to the last index that will still show a full page.
   */
  protected void lastPageStart() {
    if (view != null) {
      setPageStart(view.getRowCount() - getPageSize());
    }
  }

  /**
   * Advance the starting row by 'pageSize' rows.
   */
  protected void nextPage() {
    if (view != null) {
      Range range = view.getVisibleRange();
      setPageStart(range.getStart() + range.getLength());
    }
  }

  /**
   * Called when the range or row count changes. Implement this method to update
   * the pager.
   */
  protected abstract void onRangeOrRowCountChanged();

  /**
   * Move the starting row back by 'pageSize' rows.
   */
  protected void previousPage() {
    if (view != null) {
      Range range = view.getVisibleRange();
      setPageStart(range.getStart() - range.getLength());
    }
  }

  /**
   * Go to a specific page.
   *
   * @param index the page index
   */
  protected void setPage(int index) {
    if (view != null
        && (!isRangeLimited || !view.isRowCountExact() || hasPage(index))) {
      // We don't use the local version of setPageStart because it would
      // constrain the index, but the user probably wants to use absolute page
      // indexes.
      int pageSize = getPageSize();
      view.setVisibleRange(pageSize * index, pageSize);
    }
  }

  /**
   * Set the page size of the view.
   *
   * @param pageSize the new page size
   */
  protected void setPageSize(int pageSize) {
    if (view != null) {
      Range range = view.getVisibleRange();
      int pageStart = range.getStart();
      if (isRangeLimited && view.isRowCountExact()) {
        pageStart = Math.min(pageStart, view.getRowCount() - pageSize);
      }
      pageStart = Math.max(0, pageStart);
      view.setVisibleRange(pageStart, pageSize);
    }
  }

  /**
   * Set the page start index.
   *
   * @param index the index
   */
  protected void setPageStart(int index) {
    if (view != null) {
      Range range = view.getVisibleRange();
      int pageSize = range.getLength();
      if (isRangeLimited && view.isRowCountExact()) {
        index = Math.min(index, view.getRowCount() - pageSize);
      }
      index = Math.max(0, index);
      if (index != range.getStart()) {
        view.setVisibleRange(index, pageSize);
      }
    }
  }

  /**
   * Called when the row count changes. Only called if view is non-null.
   *
   * @param rowCount the new row count
   * @param isExact true if the row count is exact
   */
  private void handleRowCountChange(int rowCount, boolean isExact) {
    int oldRowCount = lastRowCount;
    lastRowCount = view.getRowCount();

    // If the row count has changed, limit the range.
    if (isRangeLimited && oldRowCount != lastRowCount) {
      setPageStart(getPageStart());
    }

    // Call user methods.
    onRangeOrRowCountChanged();
  }
}
