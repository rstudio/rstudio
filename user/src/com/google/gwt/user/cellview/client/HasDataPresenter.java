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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * <p>
 * Presenter implementation of {@link HasData} that presents data for various
 * cell based widgets. This class contains most of the shared logic used by
 * these widgets, making it easier to test the common code.
 * <p>
 * <p>
 * In proper MVP design, user code would interact with the presenter. However,
 * that would complicate the widget code. Instead, each widget owns its own
 * presenter and contains its own View. The widget forwards commands through to
 * the presenter, which then updates the widget via the view. This keeps the
 * user facing API simpler.
 * <p>
 *
 * @param <T> the data type of items in the list
 */
class HasDataPresenter<T> implements HasData<T> {

  /**
   * Default iterator over DOM elements.
   */
  static class DefaultElementIterator implements ElementIterator {
    private Element current;
    private Element next;
    private final View<?> view;

    public DefaultElementIterator(View<?> view, Element first) {
      this.view = view;
      next = first;
    }

    public boolean hasNext() {
      return next != null;
    }

    public Element next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      current = next;
      next = next.getNextSiblingElement();
      return current;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    /**
     * Set the selection state of the current element.
     *
     * @param selected the selection state
     * @throws IllegalStateException if {@link #next()} has not been called
     */
    public void setSelected(boolean selected) throws IllegalStateException {
      if (current == null) {
        throw new IllegalStateException();
      }
      view.setSelected(current, selected);
    }
  }

  /**
   * An iterator over DOM elements.
   */
  static interface ElementIterator extends Iterator<Element> {
    /**
     * Set the selection state of the current element.
     *
     * @param selected the selection state
     * @throws IllegalStateException if {@link #next()} has not been called
     */
    void setSelected(boolean selected) throws IllegalStateException;
  }

  /**
   * The loading state of the data.
   */
  static enum LoadingState {
    LOADING, // Waiting for data to load.
    PARTIALLY_LOADED, // Partial page data loaded.
    LOADED, // All page data loaded.
    EMPTY; // The data size is 0.
  }

  /**
   * The view that this presenter presents.
   *
   * @param <T> the data type
   */
  static interface View<T> {

    /**
     * Add a handler to the view.
     *
     * @param <H> the handler type
     * @param handler the handler to add
     * @param type the event type
     */
    <H extends EventHandler> HandlerRegistration addHandler(
        final H handler, GwtEvent.Type<H> type);

    /**
     * Check whether or not the cells in the view depend on the selection state.
     *
     * @return true if cells depend on selection, false if not
     */
    boolean dependsOnSelection();

    /**
     * Get the physical child count.
     *
     * @return the child count
     */
    int getChildCount();

    /**
     * Get an iterator over the children of the view.
     *
     * @return the iterator
     */
    ElementIterator getChildIterator();

    /**
     * Called when selection changes.
     */
    void onUpdateSelection();

    /**
     * Construct the HTML that represents the list of values, taking the
     * selection state into account.
     *
     * @param sb the {@link StringBuilder} to build into
     * @param values the values to render
     * @param start the start index that is being rendered
     * @param selectionModel the {@link SelectionModel}
     */
    void render(StringBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel);

    /**
     * Replace all children with the specified html.
     *
     * @param values the values of the new children
     * @param html the html to render in the child
     */
    void replaceAllChildren(List<T> values, String html);

    /**
     * Convert the specified HTML into DOM elements and replace the existing
     * elements starting at the specified index. If the number of children
     * specified exceeds the existing number of children, the remaining children
     * should be appended.
     *
     * @param values the values of the new children
     * @param start the start index to be replaced
     * @param html the HTML to convert
     */
    void replaceChildren(List<T> values, int start, String html);

    /**
     * Re-establish focus on an element within the view if desired.
     */
    void resetFocus();

    /**
     * Set the current loading state of the data.
     *
     * @param state the loading state
     */
    void setLoadingState(LoadingState state);

    /**
     * Update an element to reflect its selected state.
     *
     * @param elem the element to update
     * @param selected true if selected, false if not
     */
    void setSelected(Element elem, boolean selected);
  }

  private final HasData<T> display;

  /**
   * As an optimization, keep track of the last HTML string that we rendered. If
   * the contents do not change the next time we render, then we don't have to
   * set inner html.
   */
  private String lastContents = null;

  private int pageSize;
  private int pageStart = 0;

  /**
   * Set to true when the page start changes, and we need to do a full refresh.
   */
  private boolean pageStartChangedSinceRender;

  private int rowCount = Integer.MIN_VALUE;

  private boolean rowCountIsExact;

  /**
   * The local cache of data in the view. The 0th index in the list corresponds
   * to the value at pageStart.
   */
  private final List<T> rowData = new ArrayList<T>();

  /**
   * A local cache of the currently selected rows. We cannot track selected keys
   * instead because we might end up in an inconsistent state where we render a
   * subset of a list with duplicate values, styling a value in the subset but
   * not styling the duplicate value outside of the subset.
   */
  private final Set<Integer> selectedRows = new HashSet<Integer>();

  private HandlerRegistration selectionHandler;
  private SelectionModel<? super T> selectionModel;
  private final View<T> view;

  /**
   * Construct a new {@link HasDataPresenter}.
   *
   * @param display the display that is being presented
   * @param view the view implementation
   * @param pageSize the default page size
   */
  public HasDataPresenter(HasData<T> display, View<T> view, int pageSize) {
    this.display = display;
    this.view = view;
    this.pageSize = pageSize;
  }

  public HandlerRegistration addRangeChangeHandler(
      RangeChangeEvent.Handler handler) {
    return view.addHandler(handler, RangeChangeEvent.getType());
  }

  public HandlerRegistration addRowCountChangeHandler(
      RowCountChangeEvent.Handler handler) {
    return view.addHandler(handler, RowCountChangeEvent.getType());
  }

  /**
   * Clear the {@link SelectionModel} without updating the view.
   */
  public void clearSelectionModel() {
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }
    selectionModel = null;
  }

  /**
   * @throws UnsupportedOperationException
   */
  public void fireEvent(GwtEvent<?> event) {
    // HasData should fire their own events.
    throw new UnsupportedOperationException();
  }

  /**
   * Get the current page size. This is usually the page size, but can be less
   * if the data size cannot fill the current page.
   *
   * @return the size of the current page
   */
  public int getCurrentPageSize() {
    return Math.min(pageSize, rowCount - pageStart);
  }

  /**
   * Get the overall data size.
   *
   * @return the data size
   */
  public int getRowCount() {
    return rowCount;
  }

  /**
   * Get the list of data within the current range. The 0th index corresponds to
   * the first value on the page. The data may not be complete or may contain
   * null values.
   *
   * @return the list of data for the current page
   */
  public List<T> getRowData() {
    return rowData;
  }

  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  /**
   * @return the range of data being displayed
   */
  public Range getVisibleRange() {
    return new Range(pageStart, pageSize);
  }

  public boolean isRowCountExact() {
    return rowCountIsExact;
  }

  /**
   * Redraw the list with the current data.
   */
  public void redraw() {
    lastContents = null;
    setRowData(pageStart, rowData);
  }

  /**
   * @throws UnsupportedOperationException
   */
  public final void setRowCount(int count) {
    // Views should defer to their own implementation of
    // setRowCount(int, boolean)) per HasRows spec.
    throw new UnsupportedOperationException();
  }

  public void setRowCount(int count, boolean isExact) {
    if (count == this.rowCount && isExact == this.rowCountIsExact) {
      return;
    }
    this.rowCount = count;
    this.rowCountIsExact = isExact;
    updateLoadingState();

    // Redraw the current page if it is affected by the new data size.
    if (updateCachedData()) {
      redraw();
    }

    // Update the pager.
    RowCountChangeEvent.fire(display, count, rowCountIsExact);
  }

  public void setRowData(int start, List<T> values) {
    int valuesLength = values.size();
    int valuesEnd = start + valuesLength;

    // Calculate the bounded start (inclusive) and end index (exclusive).
    int pageEnd = pageStart + pageSize;
    int boundedStart = Math.max(start, pageStart);
    int boundedEnd = Math.min(valuesEnd, pageEnd);
    if (start != pageStart && boundedStart >= boundedEnd) {
      // The data is out of range for the current page.
      // Intentionally allow empty lists that start on the page start.
      return;
    }

    // The data size must be at least as large as the data.
    if (valuesEnd > rowCount) {
      rowCount = valuesEnd;
      RowCountChangeEvent.fire(display, rowCount, rowCountIsExact);
    }

    // Create placeholders up to the specified index.
    int cacheOffset = Math.max(0, boundedStart - pageStart - rowData.size());
    for (int i = 0; i < cacheOffset; i++) {
      rowData.add(null);
    }

    // Insert the new values into the data array.
    for (int i = boundedStart; i < boundedEnd; i++) {
      T value = values.get(i - start);
      int dataIndex = i - pageStart;
      if (dataIndex < rowData.size()) {
        rowData.set(dataIndex, value);
      } else {
        rowData.add(value);
      }

      // Update our local cache of selected rows.
      if (selectionModel != null) {
        if (value != null && selectionModel.isSelected(value)) {
          selectedRows.add(i);
        } else {
          selectedRows.remove(i);
        }
      }
    }

    // Construct a run of elements within the range of the data and the page.
    // boundedStart = start index of the data to replace.
    // boundedSize = the number of items to replace.
    boundedStart = pageStartChangedSinceRender ? pageStart : boundedStart;
    boundedStart -= cacheOffset;
    List<T> boundedValues = rowData.subList(
        boundedStart - pageStart, boundedEnd - pageStart);
    int boundedSize = boundedValues.size();
    StringBuilder sb = new StringBuilder();
    view.render(sb, boundedValues, boundedStart, selectionModel);

    // Update the loading state.
    updateLoadingState();

    // Replace the DOM elements with the new rendered cells.
    int childCount = view.getChildCount();
    if (boundedStart == pageStart
        && (boundedSize >= childCount || boundedSize >= getCurrentPageSize()
            || rowData.size() < childCount)) {
      // If the contents have not changed, we're done.
      String newContents = sb.toString();
      if (!newContents.equals(lastContents)) {
        lastContents = newContents;
        view.replaceAllChildren(boundedValues, newContents);
      }
    } else {
      lastContents = null;
      view.replaceChildren(
          boundedValues, boundedStart - pageStart, sb.toString());
    }

    // Allow the view to reestablish focus after being re-rendered
    view.resetFocus();

    // Reset the pageStartChanged boolean.
    pageStartChangedSinceRender = false;
  }

  /**
   * @throws UnsupportedOperationException
   */
  public final void setVisibleRange(int start, int length) {
    // Views should defer to their own implementation of setVisibleRange(Range)
    // per HasRows spec.
    throw new UnsupportedOperationException();
  }

  public void setVisibleRange(Range range) {
    setVisibleRange(range, false, false);
  }

  public void setVisibleRangeAndClearData(
      Range range, boolean forceRangeChangeEvent) {
    setVisibleRange(range, true, forceRangeChangeEvent);
  }

  public void setSelectionModel(
      final SelectionModel<? super T> selectionModel) {
    clearSelectionModel();

    // Set the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(
          new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
              updateSelection();
            }
          });
    }

    // Update the current selection state based on the new model.
    updateSelection();
  }

  /**
   * Set the visible {@link Range}, optionally clearing data and/or firing a
   * {@link RangeChangeEvent}.
   *
   * @param range the new {@link Range}
   * @param clearData true to clear all data
   * @param forceRangeChangeEvent true to force a {@link RangeChangeEvent}
   */
  private void setVisibleRange(
      Range range, boolean clearData, boolean forceRangeChangeEvent) {
    final int start = range.getStart();
    final int length = range.getLength();

    // Update the page start.
    final boolean pageStartChanged = (pageStart != start);
    if (pageStartChanged) {
      // Trim the data if we aren't clearing it.
      if (!clearData) {
        if (start > pageStart) {
          int increase = start - pageStart;
          if (rowData.size() > increase) {
            // Remove the data we no longer need.
            for (int i = 0; i < increase; i++) {
              rowData.remove(0);
            }
          } else {
            // We have no overlapping data, so just clear it.
            rowData.clear();
          }
        } else {
          int decrease = pageStart - start;
          if ((rowData.size() > 0) && (decrease < pageSize)) {
            // Insert null data at the beginning.
            for (int i = 0; i < decrease; i++) {
              rowData.add(0, null);
            }
          } else {
            // We have no overlapping data, so just clear it.
            rowData.clear();
          }
        }
      }

      // Update the page start.
      pageStart = start;
      pageStartChangedSinceRender = true;
    }

    // Update the page size.
    final boolean pageSizeChanged = (pageSize != length);
    if (pageSizeChanged) {
      pageSize = length;
    }

    // Clear the data.
    if (clearData) {
      rowData.clear();
      selectedRows.clear();
    }

    // Update the loading state.
    updateLoadingState();

    // Redraw with the existing data.
    if (pageStartChanged || clearData || updateCachedData()) {
      redraw();
    }

    // Update the pager and data source if the range changed.
    if (pageStartChanged || pageSizeChanged || forceRangeChangeEvent) {
      RangeChangeEvent.fire(display, getVisibleRange());
    }
  }

  /**
   * Ensure that the cached data is consistent with the data size.
   *
   * @return true if the data was updated, false if not
   */
  private boolean updateCachedData() {
    boolean updated = false;
    int expectedLastIndex = Math.max(
        0, Math.min(pageSize, rowCount - pageStart));
    int lastIndex = rowData.size() - 1;
    while (lastIndex >= expectedLastIndex) {
      rowData.remove(lastIndex);
      selectedRows.remove(lastIndex + pageStart);
      lastIndex--;
      updated = true;
    }
    return updated;
  }

  /**
   * Update the loading state of the view based on the data size and page size.
   */
  private void updateLoadingState() {
    int cacheSize = rowData.size();
    int curPageSize = isRowCountExact() ? getCurrentPageSize() : pageSize;
    if (rowCount == 0) {
      view.setLoadingState(LoadingState.EMPTY);
    } else if (cacheSize >= curPageSize) {
      view.setLoadingState(LoadingState.LOADED);
    } else if (cacheSize == 0) {
      view.setLoadingState(LoadingState.LOADING);
    } else {
      view.setLoadingState(LoadingState.PARTIALLY_LOADED);
    }
  }

  /**
   * Update the table based on the current selection.
   */
  private void updateSelection() {
    view.onUpdateSelection();

    // Determine if our selection states are stale.
    boolean dependsOnSelection = view.dependsOnSelection();
    boolean refreshRequired = false;
    ElementIterator children = view.getChildIterator();
    int row = pageStart;
    for (T value : rowData) {
      // Increment the child.
      if (!children.hasNext()) {
        break;
      }
      children.next();

      // Update the selection state.
      boolean selected = selectionModel == null
          ? false : selectionModel.isSelected(value);
      if (selected != selectedRows.contains(row)) {
        refreshRequired = true;
        if (selected) {
          selectedRows.add(row);
        } else {
          selectedRows.remove(row);
        }
        if (!dependsOnSelection) {
          // The cell doesn't depend on selection, so we only need to update
          // the style.
          children.setSelected(selected);
        }
      }
      row++;
    }

    // Redraw the entire list if needed.
    if (refreshRequired && dependsOnSelection) {
      redraw();
    }
  }
}
