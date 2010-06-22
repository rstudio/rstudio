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
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel.SelectionChangeHandler;

import java.util.List;

/**
 * A mock {@link PagingListView} used for testing.
 * 
 * @param <T> the data type
 */
public class MockPagingListView<T> implements PagingListView<T> {

  private static final int DEFAULT_PAGE_SIZE = 10;

  private int dataSize;
  private boolean dataSizeExact;
  private Delegate<T> delegate;
  private List<T> lastData;
  private Range lastRange;
  private Pager<T> pager;
  private int pageStart;
  private int pageSize = DEFAULT_PAGE_SIZE;
  private HandlerRegistration selectionHandler;
  private SelectionModel<? super T> selectionModel;

  /**
   * Clear the last data set by {@link #setData(int, int, List)}.
   */
  public void clearLastDataAndRange() {
    lastData = null;
    lastRange = null;
  }

  public int getDataSize() {
    return dataSize;
  }

  /**
   * Get the last data set in {@link #setData(int, int, List)}.
   * 
   * @return the last data set
   */
  public List<T> getLastData() {
    return lastData;
  }

  /**
   * Get the last data range set in {@link #setData(int, int, List)}.
   * 
   * @return the last data range
   */
  public Range getLastDataRange() {
    return lastRange;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPageStart() {
    return pageStart;
  }

  public Range getRange() {
    return new Range(pageStart, pageSize);
  }

  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  public boolean isDataSizeExact() {
    return dataSizeExact;
  }

  public void setData(int start, int length, List<T> values) {
    lastRange = new Range(start, length);
    lastData = values;
  }

  public void setDataSize(int size, boolean isExact) {
    if (this.dataSize == size && this.dataSizeExact == isExact) {
      return;
    }
    this.dataSize = size;
    this.dataSizeExact = isExact;
    updatePager();
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
  }

  public void setPager(Pager<T> pager) {
    this.pager = pager;
  }

  public void setPageSize(int pageSize) {
    setRange(pageStart, pageSize);
  }

  public void setPageStart(int pageStart) {
    setRange(pageStart, pageSize);
  }

  public void setRange(int start, int length) {
    if (this.pageStart == start && this.pageSize == length) {
      return;
    }
    this.pageStart = start;
    this.pageSize = length;
    updatePager();
    updateDelegate();
  }

  public void setSelectionModel(SelectionModel<? super T> selectionModel) {
    // Remove the old selection handler.
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }

    // Add the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new SelectionChangeHandler() {
        public void onSelectionChange(SelectionChangeEvent event) {
        }
      });
    }
  }

  /**
   * Inform the delegate of the new range.
   */
  private void updateDelegate() {
    if (delegate != null) {
      delegate.onRangeChanged(this);
    }
  }

  /**
   * Inform the pager of the new range.
   */
  private void updatePager() {
    if (pager != null) {
      pager.onRangeOrSizeChanged(this);
    }
  }
}
