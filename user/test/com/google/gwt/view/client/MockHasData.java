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

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.CellPreviewEvent.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock {@link HasData} used for testing.
 *
 * @param <T> the data type
 */
public class MockHasData<T> implements HasData<T> {

  private static final int DEFAULT_PAGE_SIZE = 10;

  /**
   * A mock {@link RowCountChangeEvent.Handler} used for testing.
   */
  public static class MockRowCountChangeHandler
      implements RowCountChangeEvent.Handler {

    private int lastRowCount = -1;
    private boolean lastRowCountExact;

    public int getLastRowCount() {
      return lastRowCount;
    }

    public boolean isLastRowCountExact() {
      return lastRowCountExact;
    }

    @Override
    public void onRowCountChange(RowCountChangeEvent event) {
      this.lastRowCount = event.getNewRowCount();
      this.lastRowCountExact = event.isNewRowCountExact();
    }

    public void reset() {
      lastRowCount = -1;
      lastRowCountExact = false;
    }
  }

  /**
   * A mock {@link RangeChangeEvent.Handler} used for testing.
   */
  public static class MockRangeChangeHandler
      implements RangeChangeEvent.Handler {

    private Range lastRange;

    public Range getLastRange() {
      return lastRange;
    }

    @Override
    public void onRangeChange(RangeChangeEvent event) {
      this.lastRange = event.getNewRange();
    }

    public void reset() {
      lastRange = null;
    }
  }

  private final HandlerManager handlerManager = new HandlerManager(this);
  private Range lastRange;
  private List<T> lastRowData;

  private int pageStart;
  private int pageSize = DEFAULT_PAGE_SIZE;
  private int rowCount;
  private boolean rowCountExact;
  private HandlerRegistration selectionHandler;
  private SelectionModel<? super T> selectionModel;

  @Override
  public HandlerRegistration addCellPreviewHandler(Handler<T> handler) {
    return handlerManager.addHandler(CellPreviewEvent.getType(), handler);
  }

  @Override
  public HandlerRegistration addRangeChangeHandler(
      RangeChangeEvent.Handler handler) {
    return handlerManager.addHandler(RangeChangeEvent.getType(), handler);
  }

  @Override
  public HandlerRegistration addRowCountChangeHandler(
      RowCountChangeEvent.Handler handler) {
    return handlerManager.addHandler(RowCountChangeEvent.getType(), handler);
  }

  /**
   * Clear the last data set by {@link #setRowData(int, List)}.
   */
  public void clearLastRowDataAndRange() {
    lastRowData = null;
    lastRange = null;
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    handlerManager.fireEvent(event);
  }

  @Override
  public T getVisibleItem(int indexOnPage) {
    return lastRowData.get(indexOnPage);
  }

  @Override
  public int getVisibleItemCount() {
    return lastRowData == null ? 0 : lastRowData.size();
  }

  @Override
  public List<T> getVisibleItems() {
    return lastRowData;
  }

  /**
   * Gets the number of handlers listening to the event type.
   *
   * @param type the event type
   * @return the number of registered handlers
   */
  public int getHandlerCount(Type<?> type) {
    return handlerManager.getHandlerCount(type);
  }

  /**
   * Get the last data set in {@link #setRowData(int, List)}.
   *
   * @return the last data set
   */
  public List<? extends T> getLastRowData() {
    return lastRowData;
  }

  /**
   * Get the last data range set in {@link #setRowData(int, List)}.
   *
   * @return the last data range
   */
  public Range getLastRowDataRange() {
    return lastRange;
  }

  @Override
  public int getRowCount() {
    return rowCount;
  }

  @Override
  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public Range getVisibleRange() {
    return new Range(pageStart, pageSize);
  }

  @Override
  public boolean isRowCountExact() {
    return rowCountExact;
  }

  @Override
  public void setRowData(int start, List<? extends T> values) {
    lastRange = new Range(start, values.size());
    lastRowData = new ArrayList<T>(values);
  }

  @Override
  public final void setRowCount(int count) {
    setRowCount(count, true);
  }

  @Override
  public void setRowCount(int count, boolean isExact) {
    if (this.rowCount == count && this.rowCountExact == isExact) {
      return;
    }
    this.rowCount = count;
    this.rowCountExact = isExact;
    RowCountChangeEvent.fire(this, count, isExact);
  }

  @Override
  public final void setVisibleRange(int start, int length) {
    setVisibleRange(new Range(start, length));
  }

  @Override
  public void setVisibleRange(Range range) {
    setVisibleRange(range, false, false);
  }

  @Override
  public void setVisibleRangeAndClearData(
      Range range, boolean forceRangeChangeEvent) {
    setVisibleRange(range, true, forceRangeChangeEvent);
  }

  @Override
  public void setSelectionModel(SelectionModel<? super T> selectionModel) {
    // Remove the old selection handler.
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }

    // Add the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(
          new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
            }
          });
    }
  }

  private void setVisibleRange(
      Range range, boolean clearData, boolean forceRangeChangeEvent) {
    int start = range.getStart();
    int length = range.getLength();
    if (clearData) {
      lastRowData = null;
    }
    if (!forceRangeChangeEvent && this.pageStart == start
        && this.pageSize == length) {
      return;
    }
    this.pageStart = start;
    this.pageSize = length;
    RangeChangeEvent.fire(this, getVisibleRange());
  }
}
