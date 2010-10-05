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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.HasKeyProvider;
import com.google.gwt.view.client.ProvidesKey;
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
class HasDataPresenter<T> implements HasData<T>, HasKeyProvider<T>,
    HasKeyboardPagingPolicy {

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
    <H extends EventHandler> HandlerRegistration addHandler(final H handler,
        GwtEvent.Type<H> type);

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
     * @param sb the {@link SafeHtmlBuilder} to build into
     * @param values the values to render
     * @param start the start index that is being rendered
     * @param selectionModel the {@link SelectionModel}
     */
    void render(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel);

    /**
     * Replace all children with the specified html.
     *
     * @param values the values of the new children
     * @param html the html to render in the child
     */
    void replaceAllChildren(List<T> values, SafeHtml html);

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
    void replaceChildren(List<T> values, int start, SafeHtml html);

    /**
     * Re-establish focus on an element within the view if the view already had
     * focus.
     */
    void resetFocus();

    /**
     * Update an element to reflect its keyboard selected state.
     *
     * @param index the index of the element relative to page start
     * @param selected true if selected, false if not
     * @param stealFocus true if the row should steal focus, false if not
     */
    void setKeyboardSelected(int index, boolean selected, boolean stealFocus);

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

  /**
   * The number of rows to jump when PAGE_UP or PAGE_DOWN is pressed and the
   * {@link KeyboardSelectionPolicy} is
   * {@link KeyboardSelectionPolicy.INCREMENT_PAGE}.
   */
  static final int PAGE_INCREMENT = 30;

  private final HasData<T> display;

  /**
   * The current keyboard selected row relative to page start. This value should
   * never be negative.
   */
  private int keyboardSelectedRow = 0;

  /**
   * The last row value that was selected with the keyboard.
   */
  private T keyboardSelectedRowValue;

  private KeyboardPagingPolicy keyboardPagingPolicy = KeyboardPagingPolicy.CHANGE_PAGE;
  private KeyboardSelectionPolicy keyboardSelectionPolicy = KeyboardSelectionPolicy.ENABLED;

  private final ProvidesKey<T> keyProvider;

  /**
   * As an optimization, keep track of the last HTML string that we rendered. If
   * the contents do not change the next time we render, then we don't have to
   * set inner html.
   */
  private SafeHtml lastContents = null;

  private int pageSize;
  private int pageStart = 0;

  /**
   * Set to true when the page start changes, and we need to do a full refresh.
   */
  private boolean pageStartChangedSinceRender;

  private int rowCount = 0;

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
  public HasDataPresenter(HasData<T> display, View<T> view, int pageSize,
      ProvidesKey<T> keyProvider) {
    this.display = display;
    this.view = view;
    this.pageSize = pageSize;
    this.keyProvider = keyProvider;
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

  public KeyboardPagingPolicy getKeyboardPagingPolicy() {
    return keyboardPagingPolicy;
  }

  /**
   * Get the index of the keyboard selected row relative to the page start.
   *
   * @return the row index, or -1 if disabled
   */
  public int getKeyboardSelectedRow() {
    return KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy ? -1
        : keyboardSelectedRow;
  }

  public KeyboardSelectionPolicy getKeyboardSelectionPolicy() {
    return keyboardSelectionPolicy;
  }

  public ProvidesKey<T> getKeyProvider() {
    return keyProvider;
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

  /**
   * Check if the next call to {@link #keyboardNext()} would succeed.
   *
   * @return true if there is another row accessible by the keyboard
   */
  public boolean hasKeyboardNext() {
    if (KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy) {
      return false;
    } else if (keyboardSelectedRow < rowData.size() - 1) {
      return true;
    } else if (!keyboardPagingPolicy.isLimitedToRange()
        && (keyboardSelectedRow + pageStart < rowCount - 1 || !rowCountIsExact)) {
      return true;
    }
    return false;
  }

  /**
   * Check if the next call to {@link #keyboardPrevious()} would succeed.
   *
   * @return true if there is a previous row accessible by the keyboard
   */
  public boolean hasKeyboardPrev() {
    if (KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy) {
      return false;
    } else if (keyboardSelectedRow > 0) {
      return true;
    } else if (!keyboardPagingPolicy.isLimitedToRange() && pageStart > 0) {
      return true;
    }
    return false;
  }

  public boolean isRowCountExact() {
    return rowCountIsExact;
  }

  /**
   * Move keyboard selection to the last row.
   */
  public void keyboardEnd() {
    if (!keyboardPagingPolicy.isLimitedToRange()) {
      setKeyboardSelectedRow(rowCount - 1, true);
    }
  }

  /**
   * Move keyboard selection to the absolute 0th row.
   */
  public void keyboardHome() {
    if (!keyboardPagingPolicy.isLimitedToRange()) {
      setKeyboardSelectedRow(-pageStart, true);
    }
  }

  /**
   * Move keyboard selection to the next row.
   */
  public void keyboardNext() {
    if (hasKeyboardNext()) {
      setKeyboardSelectedRow(keyboardSelectedRow + 1, true);
    }
  }

  /**
   * Move keyboard selection to the next page.
   */
  public void keyboardNextPage() {
    if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
      // 0th index of next page.
      setKeyboardSelectedRow(pageSize, true);
    } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
      setKeyboardSelectedRow(keyboardSelectedRow + PAGE_INCREMENT, true);
    }
  }

  /**
   * Move keyboard selection to the previous row.
   */
  public void keyboardPrev() {
    if (hasKeyboardPrev()) {
      setKeyboardSelectedRow(keyboardSelectedRow - 1, true);
    }
  }

  /**
   * Move keyboard selection to the previous page.
   */
  public void keyboardPrevPage() {
    if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
      // 0th index of previous page.
      setKeyboardSelectedRow(-pageSize, true);
    } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
      setKeyboardSelectedRow(keyboardSelectedRow - PAGE_INCREMENT, true);
    }
  }

  /**
   * Toggle selection of the current keyboard row in the {@link SelectionModel}.
   */
  public void keyboardToggleSelect() {
    if (KeyboardSelectionPolicy.ENABLED == keyboardSelectionPolicy
        && selectionModel != null && keyboardSelectedRow >= 0
        && keyboardSelectedRow < rowData.size()) {
      T value = rowData.get(keyboardSelectedRow);
      if (value != null) {
        selectionModel.setSelected(value, !selectionModel.isSelected(value));
      }
    }
  }

  /**
   * Redraw the list with the current data.
   */
  public void redraw() {
    lastContents = null;
    setRowData(pageStart, rowData);
  }

  public void setKeyboardPagingPolicy(KeyboardPagingPolicy policy) {
    if (policy == null) {
      throw new NullPointerException("KeyboardPagingPolicy cannot be null");
    }
    this.keyboardPagingPolicy = policy;
  }

  /**
   * Set the row index of the keyboard selected element.
   *
   * @param index the row index
   * @param stealFocus true to steal focus
   */
  public void setKeyboardSelectedRow(int index, boolean stealFocus) {
    // Early exit if disabled.
    if (KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy) {
      return;
    }
    boolean isBound = KeyboardSelectionPolicy.BOUND_TO_SELECTION == keyboardSelectionPolicy;

    // Deselect the old index.
    if (keyboardSelectedRow >= 0 && keyboardSelectedRow < view.getChildCount()) {
      view.setKeyboardSelected(keyboardSelectedRow, false, false);
      if (isBound) {
        deselectKeyboardValue();
      }
    }

    // Trim to within bounds.
    int absIndex = pageStart + index;
    if (absIndex < 0) {
      absIndex = 0;
    } else if (absIndex >= rowCount && rowCountIsExact) {
      absIndex = rowCount - 1;
    }
    index = absIndex - pageStart;
    if (keyboardPagingPolicy.isLimitedToRange()) {
      index = Math.max(0, Math.min(index, pageSize - 1));
    }

    // Select the new index.
    int newPageStart = pageStart;
    int newPageSize = pageSize;
    keyboardSelectedRow = 0;
    if (index >= 0 && index < pageSize) {
      keyboardSelectedRow = index;
      if (isBound) {
        selectKeyboardValue(index);
      }
      view.setKeyboardSelected(index, true, stealFocus);
      return;
    } else if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
      // Go to previous page.
      while (index < 0) {
        newPageStart -= pageSize;
        index += pageSize;
      }

      // Go to next page.
      while (index >= pageSize) {
        newPageStart += pageSize;
        index -= pageSize;
      }
    } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
      // Increase range at the beginning.
      while (index < 0) {
        newPageSize += PAGE_INCREMENT;
        newPageStart -= PAGE_INCREMENT;
        index += PAGE_INCREMENT;
      }
      if (newPageStart < 0) {
        index += newPageStart;
        newPageSize += newPageStart;
        newPageStart = 0;
      }

      // Increase range at the end.
      while (index >= newPageSize) {
        newPageSize += PAGE_INCREMENT;
      }
      if (isRowCountExact()) {
        newPageSize = Math.min(newPageSize, rowCount - newPageStart);
        if (index >= rowCount) {
          index = rowCount - 1;
        }
      }
    }

    // Update the range if it changed.
    if (newPageStart != pageStart || newPageSize != pageSize) {
      deselectKeyboardValue();
      keyboardSelectedRow = index;
      setVisibleRange(new Range(newPageStart, newPageSize), false, false);
    }
  }

  public void setKeyboardSelectionPolicy(KeyboardSelectionPolicy policy) {
    if (policy == null) {
      throw new NullPointerException("KeyboardSelectionPolicy cannot be null");
    }
    this.keyboardSelectionPolicy = policy;
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

    // Update the keyboardSelectedRow.
    if (keyboardSelectedRow >= count) {
      keyboardSelectedRow = Math.max(0, count - 1);
    }

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

    // If the keyboard selected row is within the data set, clear it out. If the
    // key still exists, it will be reset below at its new index.
    Object keyboardSelectedKey = null;
    int keyboardSelectedAbsoluteRow = pageStart + keyboardSelectedRow;
    boolean keyboardSelectedInRange = false;
    boolean keyboardSelectedStillExists = false;
    if (keyboardSelectedAbsoluteRow >= boundedStart
        && keyboardSelectedAbsoluteRow < boundedEnd) {
      keyboardSelectedInRange = true;

      // If the value is null, then we will select whatever value is at the
      // selected row.
      if (keyboardSelectedRowValue != null) {
        keyboardSelectedKey = getRowValueKey(keyboardSelectedRowValue);
        keyboardSelectedRow = 0; // Will be set to a non-negative number later.
      }
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

      // Update the keyboard selected index.
      if (keyboardSelectedKey != null && value != null
          && keyboardSelectedKey.equals(getRowValueKey(value))) {
        keyboardSelectedRow = i - pageStart;
        keyboardSelectedStillExists = true;
      }
    }

    // Construct a run of elements within the range of the data and the page.
    // boundedStart = start index of the data to replace.
    // boundedSize = the number of items to replace.
    boundedStart = pageStartChangedSinceRender ? pageStart : boundedStart;
    boundedStart -= cacheOffset;
    List<T> boundedValues = rowData.subList(boundedStart - pageStart,
        boundedEnd - pageStart);
    int boundedSize = boundedValues.size();
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    view.render(sb, boundedValues, boundedStart, selectionModel);

    // Update the loading state.
    updateLoadingState();

    // Replace the DOM elements with the new rendered cells.
    int childCount = view.getChildCount();
    if (boundedStart == pageStart
        && (boundedSize >= childCount || boundedSize >= getCurrentPageSize() || rowData.size() < childCount)) {
      // If the contents have not changed, we're done.
      SafeHtml newContents = sb.toSafeHtml();
      if (!newContents.equals(lastContents)) {
        lastContents = newContents;
        view.replaceAllChildren(boundedValues, newContents);
      }

      // Allow the view to reestablish focus after being re-rendered.
      view.resetFocus();
    } else {
      lastContents = null;
      view.replaceChildren(boundedValues, boundedStart - pageStart,
          sb.toSafeHtml());

      // Only reset focus if needed.
      if (keyboardSelectedStillExists) {
        view.resetFocus();
      }
    }

    // Reset the pageStartChanged boolean.
    pageStartChangedSinceRender = false;

    // Update the keyboard selected value.
    if (keyboardSelectedInRange && !keyboardSelectedStillExists) {
      if (keyboardSelectedKey != null) {
        // We had a value, but its lost.
        deselectKeyboardValue();
      }

      // Select the selected row based off the row index.
      if (KeyboardSelectionPolicy.BOUND_TO_SELECTION == keyboardSelectionPolicy) {
        selectKeyboardValue(keyboardSelectedRow);
      }
    }
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

  public void setVisibleRangeAndClearData(Range range,
      boolean forceRangeChangeEvent) {
    setVisibleRange(range, true, forceRangeChangeEvent);
  }

  public void setSelectionModel(final SelectionModel<? super T> selectionModel) {
    clearSelectionModel();

    // Set the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
        public void onSelectionChange(SelectionChangeEvent event) {
          updateSelection();
        }
      });
    }

    // Update the current selection state based on the new model.
    updateSelection();
  }

  /**
   * Deselect the keyboard selected value.
   */
  private void deselectKeyboardValue() {
    if (selectionModel != null && keyboardSelectedRowValue != null) {
      T curValue = keyboardSelectedRowValue;
      keyboardSelectedRow = 0;
      keyboardSelectedRowValue = null;
      selectionModel.setSelected(curValue, false);
    }
  }

  /**
   * Get the key for the specified row value.
   *
   * @param rowValue the row value
   * @return the key
   */
  private Object getRowValueKey(T rowValue) {
    return keyProvider == null ? rowValue : keyProvider.getKey(rowValue);
  }

  /**
   * Select the value at the keyboard selected row.
   *
   * @param row the row index
   */
  private void selectKeyboardValue(int row) {
    if (selectionModel != null && row >= 0 && row < rowData.size()) {
      keyboardSelectedRowValue = rowData.get(row);
      if (keyboardSelectedRowValue != null) {
        selectionModel.setSelected(keyboardSelectedRowValue, true);
      }
    }
  }

  /**
   * Set the visible {@link Range}, optionally clearing data and/or firing a
   * {@link RangeChangeEvent}.
   *
   * @param range the new {@link Range}
   * @param clearData true to clear all data
   * @param forceRangeChangeEvent true to force a {@link RangeChangeEvent}
   */
  private void setVisibleRange(Range range, boolean clearData,
      boolean forceRangeChangeEvent) {
    final int start = range.getStart();
    final int length = range.getLength();
    if (length < 0) {
      throw new IllegalArgumentException("Range length cannot be less than 1");
    }

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
    int expectedLastIndex = Math.max(0,
        Math.min(pageSize, rowCount - pageStart));
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
    if (rowCount == 0 && rowCountIsExact) {
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
      boolean selected = selectionModel == null ? false
          : selectionModel.isSelected(value);
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
