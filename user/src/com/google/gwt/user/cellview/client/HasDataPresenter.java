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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.HasKeyProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
 * <p>
 * Updates are not pushed to the view immediately. Instead, the presenter
 * collects updates and resolves them all in a finally command. This reduces the
 * total number of DOM manipulations, and makes it easier to handle side effects
 * in user code triggered by the rendering pass. The view is responsible for
 * called {@link #flush()} to force the presenter to synchronize the view when
 * needed.
 * </p>
 * 
 * @param <T> the data type of items in the list
 */
class HasDataPresenter<T> implements HasData<T>, HasKeyProvider<T>, HasKeyboardPagingPolicy {

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
    <H extends EventHandler> HandlerRegistration addHandler(final H handler, GwtEvent.Type<H> type);

    /**
     * Replace all children with the specified values.
     * 
     * @param values the values of the new children
     * @param selectionModel the {@link SelectionModel}
     * @param stealFocus true if the row should steal focus, false if not
     */
    void replaceAllChildren(List<T> values, SelectionModel<? super T> selectionModel,
        boolean stealFocus);

    /**
     * Replace existing elements starting at the specified index. If the number
     * of children specified exceeds the existing number of children, the
     * remaining children should be appended.
     * 
     * @param values the values of the new children
     * @param start the start index to be replaced, relative to the pageStart
     * @param selectionModel the {@link SelectionModel}
     * @param stealFocus true if the row should steal focus, false if not
     */
    void replaceChildren(List<T> values, int start, SelectionModel<? super T> selectionModel,
        boolean stealFocus);

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
  }

  /**
   * Represents the state of the presenter.
   * 
   * @param <T> the data type of the presenter
   */
  private static class DefaultState<T> implements State<T> {
    int keyboardSelectedRow = 0;
    T keyboardSelectedRowValue = null;
    int pageSize;
    int pageStart = 0;
    int rowCount = 0;
    boolean rowCountIsExact = false;
    final List<T> rowData = new ArrayList<T>();
    final Set<Integer> selectedRows = new HashSet<Integer>();
    T selectedValue = null;
    boolean viewTouched;

    public DefaultState(int pageSize) {
      this.pageSize = pageSize;
    }

    @Override
    public int getKeyboardSelectedRow() {
      return keyboardSelectedRow;
    }

    @Override
    public T getKeyboardSelectedRowValue() {
      return keyboardSelectedRowValue;
    }

    @Override
    public int getPageSize() {
      return pageSize;
    }

    @Override
    public int getPageStart() {
      return pageStart;
    }

    @Override
    public int getRowCount() {
      return rowCount;
    }

    @Override
    public int getRowDataSize() {
      return rowData.size();
    }

    @Override
    public T getRowDataValue(int index) {
      return rowData.get(index);
    }

    @Override
    public List<T> getRowDataValues() {
      return Collections.unmodifiableList(rowData);
    }

    @Override
    public T getSelectedValue() {
      return selectedValue;
    }

    @Override
    public boolean isRowCountExact() {
      return rowCountIsExact;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The set of selected rows is not maintained in the pending state. This
     * method should only be called on the state after it has been resolved.
     * </p>
     */
    @Override
    public boolean isRowSelected(int index) {
      return selectedRows.contains(index);
    }

    @Override
    public boolean isViewTouched() {
      return viewTouched;
    }
  }

  /**
   * Represents the pending state of the presenter.
   * 
   * @param <T> the data type of the presenter
   */
  private static class PendingState<T> extends DefaultState<T> {

    /**
     * A boolean indicating that the user has keyboard selected a new row.
     */
    private boolean keyboardSelectedRowChanged;

    /**
     * A boolean indicating that a change in keyboard selected should cause us
     * to steal focus.
     */
    private boolean keyboardStealFocus = false;

    /**
     * Set to true if a redraw is required.
     */
    private boolean redrawRequired = false;

    /**
     * The list of ranges that have been replaced.
     */
    private final List<Range> replacedRanges = new ArrayList<Range>();

    public PendingState(State<T> state) {
      super(state.getPageSize());
      this.keyboardSelectedRow = state.getKeyboardSelectedRow();
      this.keyboardSelectedRowValue = state.getKeyboardSelectedRowValue();
      this.pageSize = state.getPageSize();
      this.pageStart = state.getPageStart();
      this.rowCount = state.getRowCount();
      this.rowCountIsExact = state.isRowCountExact();
      this.selectedValue = state.getSelectedValue();
      this.viewTouched = state.isViewTouched();

      // Copy the row data.
      int rowDataSize = state.getRowDataSize();
      for (int i = 0; i < rowDataSize; i++) {
        this.rowData.add(state.getRowDataValue(i));
      }

      /*
       * We do not copy the selected rows from the old state. They will be
       * resolved from the SelectionModel.
       */
    }

    /**
     * Update the range of replaced data.
     * 
     * @param start the start index
     * @param end the end index
     */
    public void replaceRange(int start, int end) {
      replacedRanges.add(new Range(start, end - start));
    }
  }

  /**
   * Represents the state of the presenter.
   * 
   * @param <T> the data type of the presenter
   */
  private static interface State<T> {
    /**
     * Get the current keyboard selected row relative to page start. This value
     * should never be negative.
     */
    int getKeyboardSelectedRow();

    /**
     * Get the last row value that was selected with the keyboard.
     */
    T getKeyboardSelectedRowValue();

    /**
     * Get the number of rows in the current page.
     */
    int getPageSize();

    /**
     * Get the absolute start index of the page.
     */
    int getPageStart();

    /**
     * Get the total number of rows.
     */
    int getRowCount();

    /**
     * Get the size of the row data.
     */
    int getRowDataSize();

    /**
     * Get a specific value from the row data.
     */
    T getRowDataValue(int index);

    /**
     * Get all of the row data values in an unmodifiable list.
     */
    List<T> getRowDataValues();

    /**
     * Get the value that is selected in the {@link SelectionModel}.
     */
    T getSelectedValue();

    /**
     * Get a boolean indicating whether the row count is exact or an estimate.
     */
    boolean isRowCountExact();

    /**
     * Check if a row index is selected.
     * 
     * @param index the row index
     * @return true if selected, false if not
     */
    boolean isRowSelected(int index);

    /**
     * Check if the user interacted with the view at some point. Selection is
     * not bound to the keyboard selected row until the view is touched. Once
     * touched, selection is bound from then on.
     */
    boolean isViewTouched();
  }

  /**
   * The number of rows to jump when PAGE_UP or PAGE_DOWN is pressed and the
   * {@link HasKeyboardPagingPolicy.KeyboardPagingPolicy} is
   * {@link HasKeyboardPagingPolicy.KeyboardPagingPolicy#INCREASE_RANGE}.
   */
  static final int PAGE_INCREMENT = 30;

  /**
   * The maximum number of times we can try to
   * {@link #resolvePendingState(JsArrayInteger)} before we assume there is an
   * infinite loop.
   */
  private static final int LOOP_MAXIMUM = 10;

  /**
   * The minimum number of rows that need to be replaced before we do a redraw.
   */
  private static final int REDRAW_MINIMUM = 5;

  /**
   * The threshold of new data after which we redraw the entire view instead of
   * replacing specific rows.
   * 
   * TODO(jlabanca): Find the optimal value for the threshold.
   */
  private static final double REDRAW_THRESHOLD = 0.30;

  /**
   * Sort a native integer array numerically.
   * 
   * @param array the array to sort
   */
  private static native void sortJsArrayInteger(JsArrayInteger array) /*-{
    // sort() sorts lexicographically by default.
    array.sort(function(x, y) {
      return x - y;
    });
  }-*/;

  private final HasData<T> display;

  /**
   * A boolean indicating that we are in the process of resolving state.
   */
  private boolean isResolvingState;

  private KeyboardPagingPolicy keyboardPagingPolicy = KeyboardPagingPolicy.CHANGE_PAGE;
  private KeyboardSelectionPolicy keyboardSelectionPolicy = KeyboardSelectionPolicy.ENABLED;

  private final ProvidesKey<T> keyProvider;

  /**
   * The pending state of the presenter to be pushed to the view.
   */
  private PendingState<T> pendingState;

  /**
   * The command used to resolve the pending state.
   */
  private ScheduledCommand pendingStateCommand;

  /**
   * A counter used to detect infinite loops in
   * {@link #resolvePendingState(JsArrayInteger)}. An infinite loop can occur if
   * user code, such as reading the {@link SelectionModel}, causes the table to
   * have a pending state.
   */
  private int pendingStateLoop = 0;

  private HandlerRegistration selectionHandler;
  private SelectionModel<? super T> selectionModel;

  /**
   * The current state of the presenter reflected in the view. We intentionally
   * use the interface, which only has getters, to ensure that we do not
   * accidently modify the current state.
   */
  private State<T> state;

  private final View<T> view;

  /**
   * Construct a new {@link HasDataPresenter}.
   * 
   * @param display the display that is being presented
   * @param view the view implementation
   * @param pageSize the default page size
   */
  public HasDataPresenter(HasData<T> display, View<T> view, int pageSize, ProvidesKey<T> keyProvider) {
    this.display = display;
    this.view = view;
    this.keyProvider = keyProvider;
    this.state = new DefaultState<T>(pageSize);
  }

  @Override
  public HandlerRegistration addCellPreviewHandler(CellPreviewEvent.Handler<T> handler) {
    return view.addHandler(handler, CellPreviewEvent.getType());
  }

  public HandlerRegistration addLoadingStateChangeHandler(LoadingStateChangeEvent.Handler handler) {
    return view.addHandler(handler, LoadingStateChangeEvent.TYPE);
  }

  @Override
  public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler) {
    return view.addHandler(handler, RangeChangeEvent.getType());
  }

  @Override
  public HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler) {
    return view.addHandler(handler, RowCountChangeEvent.getType());
  }

  /**
   * Clear the row value associated with the keyboard selected row.
   */
  public void clearKeyboardSelectedRowValue() {
    if (getKeyboardSelectedRowValue() != null) {
      ensurePendingState().keyboardSelectedRowValue = null;
    }
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
  @Override
  public void fireEvent(GwtEvent<?> event) {
    // HasData should fire their own events.
    throw new UnsupportedOperationException();
  }

  /**
   * Flush pending changes to the view.
   */
  public void flush() {
    resolvePendingState(null);
  }

  /**
   * Get the current page size. This is usually the page size, but can be less
   * if the data size cannot fill the current page.
   * 
   * @return the size of the current page
   */
  public int getCurrentPageSize() {
    return Math.min(getPageSize(), getRowCount() - getPageStart());
  }

  @Override
  public KeyboardPagingPolicy getKeyboardPagingPolicy() {
    return keyboardPagingPolicy;
  }

  /**
   * Get the index of the keyboard selected row relative to the page start.
   * 
   * @return the row index, or -1 if disabled
   */
  public int getKeyboardSelectedRow() {
    return KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy ? -1 : getCurrentState()
        .getKeyboardSelectedRow();
  }

  /**
   * Get the index of the keyboard selected row relative to the page start as it
   * appears in the view, regardless of whether or not there is a pending
   * change.
   * 
   * @return the row index, or -1 if disabled
   */
  public int getKeyboardSelectedRowInView() {
    return KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy ? -1 : state
        .getKeyboardSelectedRow();
  }

  /**
   * Get the value that the user selected.
   * 
   * @return the value, or null if a value was not selected
   */
  public T getKeyboardSelectedRowValue() {
    return KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy ? null : getCurrentState()
        .getKeyboardSelectedRowValue();
  }

  @Override
  public KeyboardSelectionPolicy getKeyboardSelectionPolicy() {
    return keyboardSelectionPolicy;
  }

  @Override
  public ProvidesKey<T> getKeyProvider() {
    return keyProvider;
  }

  /**
   * Get the overall data size.
   * 
   * @return the data size
   */
  @Override
  public int getRowCount() {
    return getCurrentState().getRowCount();
  }

  @Override
  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public T getVisibleItem(int indexOnPage) {
    return getCurrentState().getRowDataValue(indexOnPage);
  }

  @Override
  public int getVisibleItemCount() {
    return getCurrentState().getRowDataSize();
  }

  @Override
  public List<T> getVisibleItems() {
    return getCurrentState().getRowDataValues();
  }

  /**
   * Return the range of data being displayed.
   */
  @Override
  public Range getVisibleRange() {
    return new Range(getPageStart(), getPageSize());
  }

  /**
   * Check whether or not there is a pending state. If there is a pending state,
   * views might skip DOM updates and wait for the new data to be rendered when
   * the pending state is resolved.
   * 
   * @return true if there is a pending state, false if not
   */
  public boolean hasPendingState() {
    return pendingState != null;
  }

  /**
   * Check whether or not the data set is empty. That is, the row count is
   * exactly 0.
   * 
   * @return true if data set is empty
   */
  public boolean isEmpty() {
    return isRowCountExact() && getRowCount() == 0;
  }

  @Override
  public boolean isRowCountExact() {
    return getCurrentState().isRowCountExact();
  }

  /**
   * Redraw the list with the current data.
   */
  public void redraw() {
    ensurePendingState().redrawRequired = true;
  }

  @Override
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
   * @param forceUpdate force the update even if the row didn't change
   */
  public void setKeyboardSelectedRow(int index, boolean stealFocus, boolean forceUpdate) {
    // Early exit if disabled.
    if (KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy) {
      return;
    }

    // Clip the row index if the paging policy is limited.
    if (keyboardPagingPolicy.isLimitedToRange()) {
      // index will be 0 if visible item count is 0.
      index = Math.max(0, Math.min(index, getVisibleItemCount() - 1));
    }

    // The user touched the view.
    ensurePendingState().viewTouched = true;

    /*
     * Early exit if the keyboard selected row has not changed and the keyboard
     * selected value is already set.
     */
    if (!forceUpdate && getKeyboardSelectedRow() == index && getKeyboardSelectedRowValue() != null) {
      return;
    }

    // Trim to within bounds.
    int pageStart = getPageStart();
    int pageSize = getPageSize();
    int rowCount = getRowCount();
    int absIndex = pageStart + index;
    if (absIndex >= rowCount && isRowCountExact()) {
      absIndex = rowCount - 1;
    }
    index = Math.max(0, absIndex) - pageStart;
    if (keyboardPagingPolicy.isLimitedToRange()) {
      index = Math.max(0, Math.min(index, pageSize - 1));
    }

    // Select the new index.
    int newPageStart = pageStart;
    int newPageSize = pageSize;
    PendingState<T> pending = ensurePendingState();
    pending.keyboardSelectedRow = 0;
    pending.keyboardSelectedRowValue = null;
    pending.keyboardSelectedRowChanged = true;
    if (index >= 0 && index < pageSize) {
      pending.keyboardSelectedRow = index;
      pending.keyboardSelectedRowValue =
          index < pending.getRowDataSize() ? ensurePendingState().getRowDataValue(index) : null;
      pending.keyboardStealFocus = stealFocus;
      return;
    } else if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
      // Go to previous page.
      while (index < 0) {
        int shift = Math.min(pageSize, newPageStart);
        newPageStart -= shift;
        index += shift;
      }

      // Go to next page.
      while (index >= pageSize) {
        newPageStart += pageSize;
        index -= pageSize;
      }
    } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
      // Increase range at the beginning.
      while (index < 0) {
        int shift = Math.min(PAGE_INCREMENT, newPageStart);
        newPageSize += shift;
        newPageStart -= shift;
        index += shift;
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
      pending.keyboardSelectedRow = index;
      setVisibleRange(new Range(newPageStart, newPageSize), false, false);
    }
  }

  @Override
  public void setKeyboardSelectionPolicy(KeyboardSelectionPolicy policy) {
    if (policy == null) {
      throw new NullPointerException("KeyboardSelectionPolicy cannot be null");
    }
    this.keyboardSelectionPolicy = policy;
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public final void setRowCount(int count) {
    // Views should defer to their own implementation of
    // setRowCount(int, boolean)) per HasRows spec.
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRowCount(int count, boolean isExact) {
    if (count == getRowCount() && isExact == isRowCountExact()) {
      return;
    }
    ensurePendingState().rowCount = count;
    ensurePendingState().rowCountIsExact = isExact;

    // Update the cached data.
    updateCachedData();

    // Update the pager.
    RowCountChangeEvent.fire(display, count, isExact);
  }

  @Override
  public void setRowData(int start, List<? extends T> values) {
    int valuesLength = values.size();
    int valuesEnd = start + valuesLength;

    // Calculate the bounded start (inclusive) and end index (exclusive).
    int pageStart = getPageStart();
    int pageEnd = getPageStart() + getPageSize();
    int boundedStart = Math.max(start, pageStart);
    int boundedEnd = Math.min(valuesEnd, pageEnd);
    if (start != pageStart && boundedStart >= boundedEnd) {
      // The data is out of range for the current page.
      // Intentionally allow empty lists that start on the page start.
      return;
    }

    // Create placeholders up to the specified index.
    PendingState<T> pending = ensurePendingState();
    int cacheOffset = Math.max(0, boundedStart - pageStart - getVisibleItemCount());
    for (int i = 0; i < cacheOffset; i++) {
      pending.rowData.add(null);
    }

    // Insert the new values into the data array.
    for (int i = boundedStart; i < boundedEnd; i++) {
      T value = values.get(i - start);
      int dataIndex = i - pageStart;
      if (dataIndex < getVisibleItemCount()) {
        pending.rowData.set(dataIndex, value);
      } else {
        pending.rowData.add(value);
      }
    }

    // Remember the range that has been replaced.
    pending.replaceRange(boundedStart - cacheOffset, boundedEnd);

    // Fire a row count change event after updating the data.
    if (valuesEnd > getRowCount()) {
      setRowCount(valuesEnd, isRowCountExact());
    }
  }

  @Override
  public void setSelectionModel(final SelectionModel<? super T> selectionModel) {
    clearSelectionModel();

    // Set the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler =
          selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
              // Ensure that we resolve selection.
              ensurePendingState();
            }
          });
    }

    // Update the current selection state based on the new model.
    ensurePendingState();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public final void setVisibleRange(int start, int length) {
    // Views should defer to their own implementation of setVisibleRange(Range)
    // per HasRows spec.
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVisibleRange(Range range) {
    setVisibleRange(range, false, false);
  }

  @Override
  public void setVisibleRangeAndClearData(Range range, boolean forceRangeChangeEvent) {
    setVisibleRange(range, true, forceRangeChangeEvent);
  }

  /**
   * Schedules the command.
   * 
   * <p>
   * Protected so that subclasses can override to use an alternative scheduler.
   * </p>
   * 
   * @param command the command to execute
   */
  protected void scheduleFinally(ScheduledCommand command) {
    Scheduler.get().scheduleFinally(command);
  }

  /**
   * Combine the modified row indexes into as many as two {@link Range}s,
   * optimizing to have the fewest unmodified rows within the ranges. Using two
   * ranges covers the most common use cases of selecting one row, selecting a
   * range, moving selection from one row to another, or moving keyboard
   * selection.
   * 
   * <p>
   * Visible for testing.
   * </p>
   * 
   * <p>
   * This method has the side effect of sorting the modified rows.
   * </p>
   * 
   * @param modifiedRows the unordered indexes of modified rows
   * @return up to two ranges that encompass the modified rows
   */
  List<Range> calculateModifiedRanges(JsArrayInteger modifiedRows, int pageStart, int pageEnd) {
    sortJsArrayInteger(modifiedRows);

    int rangeStart0 = -1;
    int rangeEnd0 = -1;
    int rangeStart1 = -1;
    int rangeEnd1 = -1;
    int maxDiff = 0;
    for (int i = 0; i < modifiedRows.length(); i++) {
      int index = modifiedRows.get(i);
      if (index < pageStart || index >= pageEnd) {
        // The index is out of range of the current page.
        continue;
      } else if (rangeStart0 == -1) {
        // Range0 defaults to the first index.
        rangeStart0 = index;
        rangeEnd0 = index;
      } else if (rangeStart1 == -1) {
        // Range1 defaults to the second index.
        maxDiff = index - rangeEnd0;
        rangeStart1 = index;
        rangeEnd1 = index;
      } else {
        int diff = index - rangeEnd1;
        if (diff > maxDiff) {
          // Move the old range1 onto range0 and start range1 from this index.
          rangeEnd0 = rangeEnd1;
          rangeStart1 = index;
          rangeEnd1 = index;
          maxDiff = diff;
        } else {
          // Add this index to range1.
          rangeEnd1 = index;
        }
      }
    }

    // Convert the range ends to exclusive indexes for calculations.
    rangeEnd0 += 1;
    rangeEnd1 += 1;

    // Combine the ranges if they are continuous.
    if (rangeStart1 == rangeEnd0) {
      rangeEnd0 = rangeEnd1;
      rangeStart1 = -1;
      rangeEnd1 = -1;
    }

    // Return the ranges.
    List<Range> toRet = new ArrayList<Range>();
    if (rangeStart0 != -1) {
      int rangeLength0 = rangeEnd0 - rangeStart0;
      toRet.add(new Range(rangeStart0, rangeLength0));
    }
    if (rangeStart1 != -1) {
      int rangeLength1 = rangeEnd1 - rangeStart1;
      toRet.add(new Range(rangeStart1, rangeLength1));
    }
    return toRet;
  }

  /**
   * Ensure that a pending {@link DefaultState} exists and return it.
   * 
   * @return the pending state
   */
  private PendingState<T> ensurePendingState() {
    // Create the pending state if needed.
    if (pendingState == null) {
      pendingState = new PendingState<T>(state);
    }

    /*
     * Schedule a command to resolve the pending state. If a command is already
     * scheduled, we reschedule a new one to ensure that it happens after any
     * existing finally commands (such as SelectionModel commands).
     */
    pendingStateCommand = new ScheduledCommand() {
      @Override
      public void execute() {
        // Verify that this command was the last one scheduled.
        if (pendingStateCommand == this) {
          resolvePendingState(null);
        }
      }
    };
    scheduleFinally(pendingStateCommand);

    // Return the pending state.
    return pendingState;
  }

  /**
   * Find the index within the {@link State} of the best match for the specified
   * row value. The best match is a row value with the same key, closest to the
   * initial index.
   * 
   * @param state the state to search
   * @param value the value to find
   * @param initialIndex the initial index of the value
   * @return the best match index, or -1 if not found
   */
  private int findIndexOfBestMatch(State<T> state, T value, int initialIndex) {
    // Get the key for the value.
    Object key = getRowValueKey(value);
    if (key == null) {
      return -1;
    }

    int bestMatchIndex = -1;
    int bestMatchDiff = Integer.MAX_VALUE;
    int rowDataCount = state.getRowDataSize();
    for (int i = 0; i < rowDataCount; i++) {
      T curValue = state.getRowDataValue(i);
      Object curKey = getRowValueKey(curValue);
      if (key.equals(curKey)) {
        int diff = Math.abs(initialIndex - i);
        if (diff < bestMatchDiff) {
          bestMatchIndex = i;
          bestMatchDiff = diff;
        }
      }
    }
    return bestMatchIndex;
  }

  /**
   * Get the current state of the presenter.
   * 
   * @return the pending state if one exists, otherwise the state
   */
  private State<T> getCurrentState() {
    return pendingState == null ? state : pendingState;
  }

  private int getPageSize() {
    return getCurrentState().getPageSize();
  }

  private int getPageStart() {
    return getCurrentState().getPageStart();
  }

  /**
   * Get the key for the specified row value.
   * 
   * @param rowValue the row value
   * @return the key
   */
  private Object getRowValueKey(T rowValue) {
    return (keyProvider == null || rowValue == null) ? rowValue : keyProvider.getKey(rowValue);
  }

  /**
   * Resolve the pending state and push updates to the view.
   * 
   * @param modifiedRows the modified rows that need to be updated, or null if
   *          none. The modified rows may be mutated.
   * @return true if the state changed, false if not
   */
  private boolean resolvePendingState(JsArrayInteger modifiedRows) {
    pendingStateCommand = null;

    /*
     * We are already resolving state. New changes will be flushed after the
     * current flush is finished.
     */
    if (isResolvingState) {
      return false;
    }
    isResolvingState = true;

    // Early exit if there is no pending state.
    if (pendingState == null) {
      isResolvingState = false;
      pendingStateLoop = 0;
      return false;
    }

    /*
     * Check for an infinite loop. This can happen if user code accessed in this
     * method modifies the pending state and flushes changes.
     */
    pendingStateLoop++;
    if (pendingStateLoop > LOOP_MAXIMUM) {
      isResolvingState = false;
      pendingStateLoop = 0; // Let user code handle exception and try again.
      throw new IllegalStateException(
          "A possible infinite loop has been detected in a Cell Widget. This "
              + "usually happens when your SelectionModel triggers a "
              + "SelectionChangeEvent when SelectionModel.isSelection() is "
              + "called, which causes the table to redraw continuously.");
    }

    /*
     * Swap the states in case user code triggers more changes, which will
     * create a new pendingState.
     */
    State<T> oldState = state;
    PendingState<T> newState = pendingState;
    state = pendingState;
    pendingState = null;

    /*
     * Keep track of the absolute indexes of modified rows.
     * 
     * Use a native array to avoid dynamic casts associated with emulated Java
     * Collections.
     */
    if (modifiedRows == null) {
      modifiedRows = JavaScriptObject.createArray().cast();
    }

    // Get the values used for calculations.
    int pageStart = newState.getPageStart();
    int pageSize = newState.getPageSize();
    int pageEnd = pageStart + pageSize;
    int rowDataCount = newState.getRowDataSize();

    /*
     * Resolve keyboard selection. If the row value still exists, use its index.
     * If the row value exists in multiple places, use the closest index. If the
     * row value no longer exists, use the current index.
     */
    newState.keyboardSelectedRow =
        Math.max(0, Math.min(newState.keyboardSelectedRow, rowDataCount - 1));
    if (KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy) {
      // Clear the keyboard selected state.
      newState.keyboardSelectedRow = 0;
      newState.keyboardSelectedRowValue = null;
    } else if (newState.keyboardSelectedRowChanged) {
      // Choose the row value based on the index.
      newState.keyboardSelectedRowValue =
          rowDataCount > 0 ? newState.getRowDataValue(newState.keyboardSelectedRow) : null;
    } else if (newState.keyboardSelectedRowValue != null) {
      // Choose the index based on the row value.
      int bestMatchIndex =
          findIndexOfBestMatch(newState, newState.keyboardSelectedRowValue,
              newState.keyboardSelectedRow);
      if (bestMatchIndex >= 0) {
        // A match was found.
        newState.keyboardSelectedRow = bestMatchIndex;
        newState.keyboardSelectedRowValue =
            rowDataCount > 0 ? newState.getRowDataValue(newState.keyboardSelectedRow) : null;
      } else {
        // No match was found, so reset to 0.
        newState.keyboardSelectedRow = 0;
        newState.keyboardSelectedRowValue = null;
      }
    }

    /*
     * Update the SelectionModel based on the keyboard selected value. We only
     * bind to selection after the user has interacted with the widget at least
     * once. This prevents values from being selected by default.
     */
    try {
      if (KeyboardSelectionPolicy.BOUND_TO_SELECTION == keyboardSelectionPolicy
          && selectionModel != null && newState.viewTouched) {
        T oldValue = oldState.getSelectedValue();
        Object oldKey = getRowValueKey(oldValue);
        T newValue =
            rowDataCount > 0 ? newState.getRowDataValue(newState.getKeyboardSelectedRow()) : null;
        Object newKey = getRowValueKey(newValue);
        /*
         * Do not deselect the old value unless we have a new value to select,
         * or we will have a null selection event while we wait for asynchronous
         * data to load.
         */
        if (newKey != null) {
          // Check both values for selection before setting selection, or the
          // selection model may resolve state early.
          boolean oldValueWasSelected =
              (oldValue == null) ? false : selectionModel.isSelected(oldValue);
          boolean newValueWasSelected =
              (newValue == null) ? false : selectionModel.isSelected(newValue);

          if (!newKey.equals(oldKey)) {
            // Deselect the old value.
            if (oldValueWasSelected) {
              selectionModel.setSelected(oldValue, false);
            }

            // Select the new value.
            newState.selectedValue = newValue;
            if (newValue != null && !newValueWasSelected) {
              selectionModel.setSelected(newValue, true);
            }
          } else if (!newValueWasSelected) {
            // The value was programmatically deselected.
            newState.selectedValue = null;
          }
        }
      }
    } catch (RuntimeException e) {
      // Unlock the rendering loop if the user SelectionModel throw an error.
      isResolvingState = false;
      pendingStateLoop = 0;
      throw e;
    }

    // If the keyboard row changes, add it to the modified set.
    boolean keyboardRowChanged =
        newState.keyboardSelectedRowChanged
            || (oldState.getKeyboardSelectedRow() != newState.keyboardSelectedRow)
            || (oldState.getKeyboardSelectedRowValue() == null && newState.keyboardSelectedRowValue != null);

    /*
     * Resolve selection. Check the selection status of all row values in the
     * pending state and compare them to the status in the old state. If we know
     * longer have a SelectionModel but had selected rows, we still need to
     * update the rows.
     */
    Set<Integer> newlySelectedRows = new HashSet<Integer>();
    try {
      for (int i = pageStart; i < pageStart + rowDataCount; i++) {
        // Check the new selection state.
        T rowValue = newState.getRowDataValue(i - pageStart);
        boolean isSelected =
            (rowValue != null && selectionModel != null && selectionModel.isSelected(rowValue));

        // Compare to the old selection state.
        boolean wasSelected = oldState.isRowSelected(i);
        if (isSelected) {
          newState.selectedRows.add(i);
          newlySelectedRows.add(i);
          if (!wasSelected) {
            modifiedRows.push(i);
          }
        } else if (wasSelected) {
          modifiedRows.push(i);
        }
      }
    } catch (RuntimeException e) {
      // Unlock the rendering loop if the user SelectionModel throw an error.
      isResolvingState = false;
      pendingStateLoop = 0;
      throw e;
    }

    // Add the replaced ranges as modified rows.
    boolean replacedEmptyRange = false;
    for (Range replacedRange : newState.replacedRanges) {
      int start = replacedRange.getStart();
      int length = replacedRange.getLength();
      // If the user set an empty range, pass it through to the view.
      if (length == 0) {
        replacedEmptyRange = true;
      }
      for (int i = start; i < start + length; i++) {
        modifiedRows.push(i);
      }
    }

    // Add keyboard rows to modified rows if we are going to render anyway.
    if (modifiedRows.length() > 0 && keyboardRowChanged) {
      modifiedRows.push(oldState.getKeyboardSelectedRow());
      modifiedRows.push(newState.keyboardSelectedRow);
    }

    /*
     * We called methods in user code that could modify the view, so early exit
     * if there is a new pending state waiting to be resolved.
     */
    if (pendingState != null) {
      isResolvingState = false;
      // Do not reset pendingStateLoop, or we will not detect the infinite loop.

      // Propagate modifications to the temporary pending state into the new
      // pending state instance.
      pendingState.selectedValue = newState.selectedValue;
      pendingState.selectedRows.addAll(newlySelectedRows);
      if (keyboardRowChanged) {
        pendingState.keyboardSelectedRowChanged = true;
      }
      if (newState.keyboardStealFocus) {
        pendingState.keyboardStealFocus = true;
      }

      /*
       * Add the keyboard selected rows to the modified rows so they can be
       * re-rendered in the new state. These rows may already be added, but
       * modifiedRows can contain duplicates.
       */
      modifiedRows.push(oldState.getKeyboardSelectedRow());
      modifiedRows.push(newState.keyboardSelectedRow);

      /*
       * Make a recursive call to resolve the state again, using the new pending
       * state that was just created. If we are successful, then the modified
       * rows will be redrawn. If we are not successful, then we still need to
       * redraw the modified rows.
       */
      if (resolvePendingState(modifiedRows)) {
        return true;
      }
    }

    // Calculate the modified ranges.
    List<Range> modifiedRanges = calculateModifiedRanges(modifiedRows, pageStart, pageEnd);
    Range range0 = modifiedRanges.size() > 0 ? modifiedRanges.get(0) : null;
    Range range1 = modifiedRanges.size() > 1 ? modifiedRanges.get(1) : null;
    int replaceDiff = 0; // The total number of rows to replace.
    for (Range range : modifiedRanges) {
      replaceDiff += range.getLength();
    }

    /*
     * Check the various conditions that require redraw.
     */
    int oldPageStart = oldState.getPageStart();
    int oldPageSize = oldState.getPageSize();
    int oldRowDataCount = oldState.getRowDataSize();
    boolean redrawRequired = newState.redrawRequired;
    if (pageStart != oldPageStart) {
      // Redraw if pageStart changes.
      redrawRequired = true;
    } else if (rowDataCount < oldRowDataCount) {
      // Redraw if we have trimmed the row data.
      redrawRequired = true;
    } else if (range1 == null && range0 != null && range0.getStart() == pageStart
        && (replaceDiff >= oldRowDataCount || replaceDiff > oldPageSize)) {
      // Redraw if the new data completely overlaps the old data.
      redrawRequired = true;
    } else if (replaceDiff >= REDRAW_MINIMUM && replaceDiff > REDRAW_THRESHOLD * oldRowDataCount) {
      /*
       * Redraw if the number of modified rows represents a large portion of the
       * view, defined as greater than 30% of the rows (minimum of 5).
       */
      redrawRequired = true;
    } else if (replacedEmptyRange && oldRowDataCount == 0) {
      /*
       * If the user replaced an empty range, pass it to the view. This is a
       * useful edge case that provides consistency in the way data is pushed to
       * the view.
       */
      redrawRequired = true;
    }

    // Update the loading state in the view.
    updateLoadingState();

    /*
     * Push changes to the view.
     */
    try {
      if (redrawRequired) {
        // Redraw the entire content.
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        view.replaceAllChildren(newState.rowData, selectionModel, newState.keyboardStealFocus);
        view.resetFocus();
      } else if (range0 != null) {
        // Surgically replace specific rows.

        // Replace range0.
        {
          int absStart = range0.getStart();
          int relStart = absStart - pageStart;
          SafeHtmlBuilder sb = new SafeHtmlBuilder();
          List<T> replaceValues = newState.rowData.subList(relStart, relStart + range0.getLength());
          view.replaceChildren(replaceValues, relStart, selectionModel, newState.keyboardStealFocus);
        }

        // Replace range1 if it exists.
        if (range1 != null) {
          int absStart = range1.getStart();
          int relStart = absStart - pageStart;
          SafeHtmlBuilder sb = new SafeHtmlBuilder();
          List<T> replaceValues = newState.rowData.subList(relStart, relStart + range1.getLength());
          view.replaceChildren(replaceValues, relStart, selectionModel, newState.keyboardStealFocus);
        }

        view.resetFocus();
      } else if (keyboardRowChanged) {
        // Update the keyboard selected rows without redrawing.
        // Deselect the old keyboard row.
        int oldSelectedRow = oldState.getKeyboardSelectedRow();
        if (oldSelectedRow >= 0 && oldSelectedRow < rowDataCount) {
          view.setKeyboardSelected(oldSelectedRow, false, false);
        }

        // Select the new keyboard row.
        int newSelectedRow = newState.getKeyboardSelectedRow();
        if (newSelectedRow >= 0 && newSelectedRow < rowDataCount) {
          view.setKeyboardSelected(newSelectedRow, true, newState.keyboardStealFocus);
        }
      }
    } catch (Error e) {
      // Force the error into the dev mode console.
      throw new RuntimeException(e);
    } finally {
      /*
       * We are done resolving state, so unlock the rendering loop. We unlock
       * the loop even if user rendering code throws an error to avoid throwing
       * an additional, misleading IllegalStateException.
       */
      isResolvingState = false;
    }

    /*
     * Make a recursive call to resolve any pending state. We don't expect
     * pending state here, but its always possible that pushing the changes into
     * the view could update the presenter. If there is no new state, the
     * recursive call will reset the pendingStateLoop.
     */
    resolvePendingState(null);
    return true;
  }

  /**
   * Set the visible {@link Range}, optionally clearing data and/or firing a
   * {@link RangeChangeEvent}.
   * 
   * @param range the new {@link Range}
   * @param clearData true to clear all data
   * @param forceRangeChangeEvent true to force a {@link RangeChangeEvent}
   */
  private void setVisibleRange(Range range, boolean clearData, boolean forceRangeChangeEvent) {
    final int start = range.getStart();
    final int length = range.getLength();
    if (start < 0) {
      throw new IllegalArgumentException("Range start cannot be less than 0");
    }
    if (length < 0) {
      throw new IllegalArgumentException("Range length cannot be less than 0");
    }

    // Update the page start.
    final int pageStart = getPageStart();
    final int pageSize = getPageSize();
    final boolean pageStartChanged = (pageStart != start);
    if (pageStartChanged) {
      PendingState<T> pending = ensurePendingState();

      // Trim the data if we aren't clearing it.
      if (!clearData) {
        if (start > pageStart) {
          int increase = start - pageStart;
          if (getVisibleItemCount() > increase) {
            // Remove the data we no longer need.
            for (int i = 0; i < increase; i++) {
              pending.rowData.remove(0);
            }
          } else {
            // We have no overlapping data, so just clear it.
            pending.rowData.clear();
          }
        } else {
          int decrease = pageStart - start;
          if ((getVisibleItemCount() > 0) && (decrease < pageSize)) {
            // Insert null data at the beginning.
            for (int i = 0; i < decrease; i++) {
              pending.rowData.add(0, null);
            }

            // Remember the inserted range because we might return to the same
            // pageStart in this event loop, which means we won't do a full
            // redraw, but still need to replace the inserted nulls in the view.
            pending.replaceRange(start, start + decrease);
          } else {
            // We have no overlapping data, so just clear it.
            pending.rowData.clear();
          }
        }
      }

      // Update the page start.
      pending.pageStart = start;
    }

    // Update the page size.
    final boolean pageSizeChanged = (pageSize != length);
    if (pageSizeChanged) {
      ensurePendingState().pageSize = length;
    }

    // Clear the data.
    if (clearData) {
      ensurePendingState().rowData.clear();
    }

    // Trim the row values if needed.
    updateCachedData();

    // Update the pager and data source if the range changed.
    if (pageStartChanged || pageSizeChanged || forceRangeChangeEvent) {
      RangeChangeEvent.fire(display, getVisibleRange());
    }
  }

  /**
   * Ensure that the cached data is consistent with the data size.
   */
  private void updateCachedData() {
    int pageStart = getPageStart();
    int expectedLastIndex = Math.max(0, Math.min(getPageSize(), getRowCount() - pageStart));
    int lastIndex = getVisibleItemCount() - 1;
    while (lastIndex >= expectedLastIndex) {
      ensurePendingState().rowData.remove(lastIndex);
      lastIndex--;
    }
  }

  /**
   * Update the loading state of the view based on the data size and page size.
   */
  private void updateLoadingState() {
    int cacheSize = getVisibleItemCount();
    int curPageSize = isRowCountExact() ? getCurrentPageSize() : getPageSize();
    if (cacheSize >= curPageSize) {
      view.setLoadingState(LoadingState.LOADED);
    } else if (cacheSize == 0) {
      view.setLoadingState(LoadingState.LOADING);
    } else {
      view.setLoadingState(LoadingState.PARTIALLY_LOADED);
    }
  }
}
