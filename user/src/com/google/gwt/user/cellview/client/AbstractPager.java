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

  // Visible for testing.
  HandlerRegistration rangeChangeHandler;
  HandlerRegistration rowCountChangeHandler;

  private HasRows display;

  /**
   * If true, all operations should be limited to the data size.
   */
  private boolean isRangeLimited = true;

  /**
   * The last row count.
   */
  private int lastRowCount;

  /**
   * Get the {@link HasRows} being paged.
   *
   * @return the {@link HasRows}
   * @see #setDisplay(HasRows)
   */
  public HasRows getDisplay() {
    return display;
  }

  /**
   * Get the page size.
   *
   * @return the page size, or -1 if the display is not set
   * @see #setPageSize(int)
   */
  public int getPageSize() {
    return display == null ? -1 : display.getVisibleRange().getLength();
  }

  /**
   * Get the page start index.
   *
   * @return the page start index, or -1 if the display is not set
   * @see #setPageStart(int)
   */
  public int getPageStart() {
    return display == null ? -1 : display.getVisibleRange().getStart();
  }

  /**
   * Check if the page should be limited to the actual data size. Defaults to
   * true.
   *
   * @return true if the range is limited to the data size
   * @see #setRangeLimited(boolean)
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
   * @see #isRangeLimited()
   */
  public void setRangeLimited(boolean isRangeLimited) {
    this.isRangeLimited = isRangeLimited;
  }

  /**
   * Set the {@link HasRows} to be paged.
   *
   * @param display the {@link HasRows}
   * @see #getDisplay()
   */
  public void setDisplay(HasRows display) {
    // Remove the old handlers.
    if (rangeChangeHandler != null) {
      rangeChangeHandler.removeHandler();
      rangeChangeHandler = null;
    }
    if (rowCountChangeHandler != null) {
      rowCountChangeHandler.removeHandler();
      rowCountChangeHandler = null;
    }

    // Set the new display.
    this.display = display;
    if (display != null) {
      rangeChangeHandler = display.addRangeChangeHandler(new RangeChangeEvent.Handler() {
        @Override
        public void onRangeChange(RangeChangeEvent event) {
          if (AbstractPager.this.display != null) {
            onRangeOrRowCountChanged();
          }
        }
      });
      rowCountChangeHandler = display.addRowCountChangeHandler(new RowCountChangeEvent.Handler() {
        @Override
        public void onRowCountChange(RowCountChangeEvent event) {
          if (AbstractPager.this.display != null) {
            handleRowCountChange(event.getNewRowCount(), event.isNewRowCountExact());
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
   * @return the page index, or -1 if the display is not set
   * @see #setPage(int)
   */
  protected int getPage() {
    if (display == null) {
      return -1;
    }
    Range range = display.getVisibleRange();
    int pageSize = range.getLength();
    return (range.getStart() + pageSize - 1) / pageSize;
  }

  /**
   * Get the number of pages based on the data size.
   *
   * @return the page count, or -1 if the display is not set
   */
  protected int getPageCount() {
    if (display == null) {
      return -1;
    }
    int pageSize = getPageSize();
    return (display.getRowCount() + pageSize - 1) / pageSize;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #nextPage()} will succeed in moving the starting point of the table
   * forward.
   *
   * @return true if there is a next page
   */
  protected boolean hasNextPage() {
    if (display == null) {
      return false;
    } else if (!display.isRowCountExact()) {
      return true;
    }
    Range range = display.getVisibleRange();
    return range.getStart() + range.getLength() < display.getRowCount();
  }

  /**
   * Returns true if there is enough data to display a given number of
   * additional pages.
   *
   * @param pages the number of pages to query
   * @return true if there are {@code pages} next pages
   */
  protected boolean hasNextPages(int pages) {
    if (display == null) {
      return false;
    }
    Range range = display.getVisibleRange();
    return range.getStart() + pages * range.getLength() < display.getRowCount();
  }

  /**
   * Returns true if there is enough data such that the specified page is within
   * range.
   *
   * @param index the page index
   * @return true if the specified page is in range
   */
  protected boolean hasPage(int index) {
    return display == null ? false : getPageSize() * index
        < display.getRowCount();
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   *
   * @return true if there is a previous page
   */
  protected boolean hasPreviousPage() {
    return display == null ? false : getPageStart() > 0
        && display.getRowCount() > 0;
  }

  /**
   * Returns true if there is enough data to display a given number of previous
   * pages.
   *
   * @param pages the number of previous pages to query
   * @return true if there are {@code pages} previous pages
   */
  protected boolean hasPreviousPages(int pages) {
    if (display == null) {
      return false;
    }
    Range range = display.getVisibleRange();
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
    if (display != null) {
      setPageStart(display.getRowCount() - getPageSize());
    }
  }

  /**
   * Advance the starting row by 'pageSize' rows.
   */
  protected void nextPage() {
    if (display != null) {
      Range range = display.getVisibleRange();
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
    if (display != null) {
      Range range = display.getVisibleRange();
      setPageStart(range.getStart() - range.getLength());
    }
  }

  /**
   * Go to a specific page.
   *
   * @param index the page index
   * @see #getPage()
   */
  protected void setPage(int index) {
    if (display != null
        && (!isRangeLimited || !display.isRowCountExact() || hasPage(index))) {
      // We don't use the local version of setPageStart because it would
      // constrain the index, but the user probably wants to use absolute page
      // indexes.
      int pageSize = getPageSize();
      display.setVisibleRange(pageSize * index, pageSize);
    }
  }

  /**
   * Set the page size of the display.
   *
   * @param pageSize the new page size
   * @see #getPageSize()
   */
  protected void setPageSize(int pageSize) {
    if (display != null) {
      Range range = display.getVisibleRange();
      int pageStart = range.getStart();
      if (isRangeLimited && display.isRowCountExact()) {
        pageStart = Math.min(pageStart, display.getRowCount() - pageSize);
      }
      pageStart = Math.max(0, pageStart);
      display.setVisibleRange(pageStart, pageSize);
    }
  }

  /**
   * Set the page start index.
   *
   * @param index the index
   * @see #getPageStart()
   */
  protected void setPageStart(int index) {
    if (display != null) {
      Range range = display.getVisibleRange();
      int pageSize = range.getLength();
      if (isRangeLimited && display.isRowCountExact()) {
        index = Math.min(index, display.getRowCount() - pageSize);
      }
      index = Math.max(0, index);
      if (index != range.getStart()) {
        display.setVisibleRange(index, pageSize);
      }
    }
  }

  /**
   * Called when the row count changes. Only called if display is non-null.
   *
   * @param rowCount the new row count
   * @param isExact true if the row count is exact
   */
  private void handleRowCountChange(int rowCount, boolean isExact) {
    int oldRowCount = lastRowCount;
    lastRowCount = display.getRowCount();

    // If the row count has changed, limit the range.
    if (isRangeLimited && oldRowCount != lastRowCount) {
      setPageStart(getPageStart());
    }

    // Call user methods.
    onRangeOrRowCountChanged();
  }
}
