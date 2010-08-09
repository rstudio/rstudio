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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
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
 * Presenter implementation of {@link PagingListView} that presents data for
 * various cell based widgets. This class contains most of the shared logic used
 * by these widgets, making it easier to test the common code.
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
class PagingListViewPresenter<T> implements PagingListView<T> {

  /**
   * Default iterator over DOM elements.
   */
  static class DefaultElementIterator implements ElementIterator {
    private Element current;
    private Element next;
    private final DefaultView<?> view;

    public DefaultElementIterator(DefaultView<?> view, Element first) {
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
   * The default implementation of View.
   *
   * @param <T> the data type
   */
  abstract static class DefaultView<T> implements View<T> {

    /**
     * The Element that holds the rendered child items.
     */
    private final Element childContainer;

    /**
     * The temporary element use to convert HTML to DOM.
     */
    private final com.google.gwt.user.client.Element tmpElem;

    /**
     * The widget that contains the view.
     */
    private final Widget widget;

    /**
     * Construct a new View.
     *
     * @param childContainer the element that contains the children
     */
    public DefaultView(Widget widget, Element childContainer) {
      this.widget = widget;
      this.childContainer = childContainer;
      tmpElem = Document.get().createDivElement().cast();
    }

    public int getChildCount() {
      return childContainer.getChildCount();
    }

    public ElementIterator getChildIterator() {
      return new DefaultElementIterator(
          this, childContainer.getFirstChildElement());
    }

    public void onUpdateSelection() {
    }

    public void replaceAllChildren(List<T> values, String html) {
      // If the widget is not attached, attach an event listener so we can catch
      // synchronous load events from cached images.
      if (!widget.isAttached()) {
        DOM.setEventListener(widget.getElement(), widget);
      }

      // Render the HTML.
      childContainer.setInnerHTML(CellBasedWidgetImpl.get().processHtml(html));

      // Detach the event listener.
      if (!widget.isAttached()) {
        DOM.setEventListener(widget.getElement(), null);
      }
    }

    public void replaceChildren(List<T> values, int start, String html) {
      // Convert the html to DOM elements.
      Element container = convertToElements(CellBasedWidgetImpl.get().processHtml(html));
      int count = container.getChildCount();

      // Get the first element to be replaced.
      Element toReplace = null;
      if (start < getChildCount()) {
        toReplace = childContainer.getChild(start).cast();
      }

      // Replace the elements.
      for (int i = 0; i < count; i++) {
        if (toReplace == null) {
          // The child will be removed from tmpElem, so always use index 0.
          childContainer.appendChild(container.getChild(0));
        } else {
          Element nextSibling = toReplace.getNextSiblingElement();
          childContainer.replaceChild(container.getChild(0), toReplace);
          toReplace = nextSibling;
        }
      }
    }

    /**
     * Convert the specified HTML into DOM elements and return the parent of the
     * DOM elements.
     *
     * @param html the HTML to convert
     * @return the parent element
     */
    protected Element convertToElements(String html) {
      // Attach an event listener so we can catch synchronous load events from
      // cached images.
      DOM.setEventListener(tmpElem, widget);

      tmpElem.setInnerHTML(html);

      // Detach the event listener.
      DOM.setEventListener(tmpElem, null);

      return tmpElem;
    }

    /**
     * Update an element to reflect its selected state.
     *
     * @param elem the element to update
     * @param selected true if selected, false if not
     */
    protected abstract void setSelected(Element elem, boolean selected);
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
  }

  /**
   * The local cache of data in the view. The 0th index in the list corresponds
   * to the value at pageStart.
   */
  private final List<T> data = new ArrayList<T>();

  private int dataSize = Integer.MIN_VALUE;
  private boolean dataSizeIsExact;
  private Delegate<T> delegate;

  /**
   * As an optimization, keep track of the last HTML string that we rendered. If
   * the contents do not change the next time we render, then we don't have to
   * set inner html.
   */
  private String lastContents = null;

  private final PagingListView<T> listView;
  private Pager<T> pager;
  private int pageSize;
  private int pageStart = 0;

  /**
   * Set to true when the page start changes, and we need to do a full refresh.
   */
  private boolean pageStartChangedSinceRender;

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
   * Construct a new {@link PagingListViewPresenter}.
   *
   * @param listView the listView that is being presented
   * @param view the view implementation
   * @param pageSize the default page size
   */
  public PagingListViewPresenter(
      PagingListView<T> listView, View<T> view, int pageSize) {
    this.listView = listView;
    this.view = view;
    this.pageSize = pageSize;
    updateLoadingState();
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
   * Get the current page size. This is usually the page size, but can be less
   * if the data size cannot fill the current page.
   *
   * @return the size of the current page
   */
  public int getCurrentPageSize() {
    return Math.min(pageSize, dataSize - pageStart);
  }

  /**
   * Get the list of data within the current range. The 0th index corresponds to
   * the first value on the page. The data may not be complete or may contain
   * null values.
   *
   * @return the list of data for the current page
   */
  public List<T> getData() {
    return data;
  }

  /**
   * Get the overall data size.
   *
   * @return the data size
   */
  public int getDataSize() {
    return dataSize;
  }

  /**
   * @return the range of data being displayed
   */
  public Range getRange() {
    return new Range(pageStart, pageSize);
  }

  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  public boolean isDataSizeExact() {
    return dataSizeIsExact;
  }

  /**
   * Redraw the list with the current data.
   */
  public void redraw() {
    lastContents = null;
    setData(pageStart, data.size(), data);
  }

  public void setData(int start, int length, List<T> values) {
    int valuesLength = values.size();
    int valuesEnd = start + valuesLength;

    // Calculate the bounded start (inclusive) and end index (exclusive).
    int pageEnd = pageStart + pageSize;
    int boundedStart = Math.max(start, pageStart);
    int boundedEnd = Math.min(valuesEnd, pageEnd);
    if (boundedStart >= boundedEnd) {
      // The data is out of range for the current page.
      return;
    }

    // The data size must be at least as large as the data.
    if (valuesEnd > dataSize) {
      dataSize = valuesEnd;
      onSizeChanged();
    }

    // Create placeholders up to the specified index.
    int cacheOffset = Math.max(0, boundedStart - pageStart - data.size());
    for (int i = 0; i < cacheOffset; i++) {
      data.add(null);
    }

    // Insert the new values into the data array.
    for (int i = boundedStart; i < boundedEnd; i++) {
      T value = values.get(i - start);
      int dataIndex = i - pageStart;
      if (dataIndex < data.size()) {
        data.set(dataIndex, value);
      } else {
        data.add(value);
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
    boundedStart = pageStartChangedSinceRender ? pageStart : boundedStart;
    boundedStart -= cacheOffset;
    List<T> boundedValues = data.subList(
        boundedStart - pageStart, boundedEnd - pageStart);
    int boundedSize = boundedValues.size();
    StringBuilder sb = new StringBuilder();
    view.render(sb, boundedValues, boundedStart, selectionModel);

    // Update the loading state.
    updateLoadingState();

    // Replace the DOM elements with the new rendered cells.
    int childCount = view.getChildCount();
    if (boundedStart == pageStart
        && (boundedSize >= childCount || boundedSize >= getCurrentPageSize())) {
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
   * Set the overall size of the list.
   *
   * @param size the overall size
   */
  public void setDataSize(int size, boolean isExact) {
    if (size == this.dataSize && isExact == this.dataSizeIsExact) {
      return;
    }
    this.dataSize = size;
    this.dataSizeIsExact = isExact;
    updateLoadingState();

    // Redraw the current page if it is affected by the new data size.
    if (updateCachedData()) {
      redraw();
    }

    // Update the pager.
    onSizeChanged();
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
  }

  public void setPager(PagingListView.Pager<T> pager) {
    this.pager = pager;
  }

  public void setRange(int start, int length) {
    // Update the page start.
    boolean pageStartChanged = false;
    if (pageStart != start) {
      if (start > pageStart) {
        int increase = start - pageStart;
        if (data.size() > increase) {
          // Remove the data we no longer need.
          for (int i = 0; i < increase; i++) {
            data.remove(0);
          }
        } else {
          // We have no overlapping data, so just clear it.
          data.clear();
        }
      } else {
        int decrease = pageStart - start;
        if ((data.size() > 0) && (decrease < pageSize)) {
          // Insert null data at the beginning.
          for (int i = 0; i < decrease; i++) {
            data.add(0, null);
          }
        } else {
          // We have no overlapping data, so just clear it.
          data.clear();
        }
      }
      pageStart = start;
      pageStartChanged = true;
      pageStartChangedSinceRender = true;
    }

    // Update the page size.
    boolean pageSizeChanged = false;
    if (pageSize != length) {
      pageSize = length;
      pageSizeChanged = true;
    }

    // Early exit if the range hasn't changed.
    if (!pageStartChanged && !pageSizeChanged) {
      return;
    }

    // Update the loading state.
    updateLoadingState();

    // Redraw with the existing data.
    boolean dataStale = updateCachedData();
    if (pageStartChanged || dataStale) {
      redraw();
    }

    // Update the pager.
    onSizeChanged();

    // Update the delegate with the new range.
    if (delegate != null) {
      delegate.onRangeChanged(listView);
    }
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
   * Called when pageStart, pageSize, or data size changes.
   */
  private void onSizeChanged() {
    if (pager != null) {
      pager.onRangeOrSizeChanged(listView);
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
        0, Math.min(pageSize, dataSize - pageStart));
    int lastIndex = data.size() - 1;
    while (lastIndex >= expectedLastIndex) {
      data.remove(lastIndex);
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
    int cacheSize = data.size();
    int curPageSize = isDataSizeExact() ? getCurrentPageSize() : pageSize;
    if (dataSize == 0) {
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
    for (T value : data) {
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
