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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.AbstractListViewAdapter.DefaultRange;
import com.google.gwt.view.client.ListView.Delegate;
import com.google.gwt.view.client.PagingListView.Pager;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel.SelectionChangeHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link com.google.gwt.user.cellview.client.CellList}. This
 * class is subject to change or deletion. Do not rely on this class.
 * 
 * @param <T> the data type of items in the list
 */
public abstract class CellListImpl<T> {

  /**
   * The Element that holds the rendered child items.
   */
  private Element childContainer;

  /**
   * The local cache of data in the view. The 0th index in the list corresponds
   * to the data at pageStart.
   */
  private final List<T> data = new ArrayList<T>();

  private int dataSize;

  /**
   * A boolean indicating whether or not the data size has ever been set. If the
   * data size has never been set, then we will always pass it along to the
   * view.
   */
  private boolean dataSizeInitialized;

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

  /**
   * The number of elements to show on the page.
   */
  private int pageSize;

  /**
   * The start index of the current page.
   */
  private int pageStart = 0;

  /**
   * Set to true when the page start changes, and we need to do a full refresh.
   */
  private boolean pageStartChanged;

  /**
   * Indicates whether or not a redraw is scheduled.
   */
  private boolean redrawScheduled;

  /**
   * The command used to refresh or redraw the page. If both are scheduled, the
   * refresh will take priority.
   */
  private final Scheduler.ScheduledCommand refreshCommand = new Scheduler.ScheduledCommand() {
    public void execute() {
      // We clear the variables before making the refresh/redraw call so another
      // refresh/redraw can be scheduled synchronously.
      boolean wasRefreshScheduled = refreshScheduled;
      boolean wasRedrawScheduled = redrawScheduled;
      refreshScheduled = false;
      redrawScheduled = false;
      if (wasRefreshScheduled && delegate != null) {
        // Refresh takes priority over redraw.
        delegate.onRangeChanged(listView);
      } else if (wasRedrawScheduled) {
        setData(data, pageStart);
      }
    }
  };

  /**
   * Indicates whether or not a refresh is scheduled.
   */
  private boolean refreshScheduled;

  /**
   * A local cache of the currently selected rows. We cannot track selected keys
   * instead because we might end up in an inconsistent state where we render a
   * subset of a list with duplicate values, styling a value in the subset but
   * not styling the duplicate value outside of the subset.
   */
  private final Set<Integer> selectedRows = new HashSet<Integer>();

  private HandlerRegistration selectionHandler;

  private SelectionModel<? super T> selectionModel;

  /**
   * The temporary element use to convert HTML to DOM.
   */
  private final Element tmpElem;

  public CellListImpl(PagingListView<T> listView, int pageSize,
      Element childContainer) {
    this.childContainer = childContainer;
    this.listView = listView;
    this.pageSize = pageSize;
    tmpElem = Document.get().createDivElement();
  }

  public boolean dataSizeIsExact() {
    return dataSizeIsExact;
  }

  /**
   * Get the list of data within the current range. The data may not be
   * complete.
   * 
   * @return the list of data
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
   * Get the number of items that are within the current page and data range.
   * 
   * @return the number of displayed items
   */
  public int getDisplayedItemCount() {
    return Math.min(pageSize, dataSize - pageStart);
  }

  /**
   * @return the page size
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * @return the start index of the current page (inclusive)
   */
  public int getPageStart() {
    return pageStart;
  }

  /**
   * @return the range of data being displayed
   */
  public Range getRange() {
    return new DefaultRange(pageStart, pageSize);
  }

  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  /**
   * Redraw the list with the current data.
   */
  public void redraw() {
    lastContents = null;
    scheduleRefresh(true);
  }

  /**
   * Request data from the delegate.
   */
  public void refresh() {
    scheduleRefresh(false);
  }

  /**
   * Set the data in the list.
   * 
   * @param values the new data
   * @param valuesStart the start index of the values
   */
  public void setData(List<T> values, int valuesStart) {
    int valuesLength = values.size();
    int valuesEnd = valuesStart + valuesLength;

    // Calculate the bounded start (inclusive) and end index (exclusive).
    int pageEnd = pageStart + pageSize;
    int boundedStart = Math.max(valuesStart, pageStart);
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
    int lastCacheIndex = pageStart + data.size();
    while (lastCacheIndex < boundedStart) {
      data.add(null);
      lastCacheIndex++;
    }

    // Insert the new values into the data array.
    for (int i = boundedStart; i < boundedEnd; i++) {
      T value = values.get(i - valuesStart);
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
    boundedStart = pageStartChanged ? pageStart : boundedStart;
    List<T> boundedValues = data.subList(boundedStart - pageStart, boundedEnd
        - pageStart);
    int boundedSize = boundedValues.size();
    StringBuilder sb = new StringBuilder();
    emitHtml(sb, boundedValues, boundedStart, selectionModel);

    // Replace the DOM elements with the new rendered cells.
    int childCount = childContainer.getChildCount();
    if (boundedStart == pageStart
        && (boundedSize >= childCount || boundedSize >= getDisplayedItemCount())) {
      // If the contents have changed, we're done.
      String newContents = sb.toString();
      if (!newContents.equals(lastContents)) {
        lastContents = newContents;
        childContainer = renderChildContents(newContents);
      }
    } else {
      lastContents = null;
      Element container = convertToElements(sb.toString());
      Element toReplace = null;
      int realStart = boundedStart - pageStart;
      if (realStart < childCount) {
        toReplace = childContainer.getChild(realStart).cast();
      }
      for (int i = boundedStart; i < boundedEnd; i++) {
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

    // Reset the pageStartChanged boolean.
    pageStartChanged = false;
  }

  /**
   * Set the overall size of the list.
   * 
   * @param size the overall size
   */
  public void setDataSize(int size, boolean isExact) {
    if (dataSizeInitialized && size == this.dataSize) {
      return;
    }
    dataSizeInitialized = true;
    this.dataSize = size;
    this.dataSizeIsExact = isExact;
    this.lastContents = null;
    updateDataAndView();
    onSizeChanged();
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
  }

  public void setPager(PagingListView.Pager<T> pager) {
    this.pager = pager;
  }

  /**
   * Set the number of items to show on each page.
   * 
   * @param pageSize the page size
   */
  public void setPageSize(int pageSize) {
    if (pageSize == this.pageSize) {
      return;
    }
    this.pageSize = pageSize;
    updateDataAndView();
    onSizeChanged();
    refresh();
  }

  /**
   * Set the start index of the range.
   * 
   * @param pageStart the start index
   */
  public void setPageStart(int pageStart) {
    if (pageStart == this.pageStart) {
      return;
    } else if (pageStart > this.pageStart) {
      if (data.size() > pageStart - this.pageStart) {
        // Remove the data we no longer need.
        for (int i = this.pageStart; i < pageStart; i++) {
          data.remove(0);
        }
      } else {
        // We have no overlapping data, so just clear it.
        data.clear();
      }
    } else {
      if ((data.size() > 0) && (this.pageStart - pageStart < pageSize)) {
        // Insert null data at the beginning.
        for (int i = pageStart; i < this.pageStart; i++) {
          data.add(0, null);
        }
      } else {
        // We have no overlapping data, so just clear it.
        data.clear();
      }
    }

    // Update the start index.
    this.pageStart = pageStart;
    this.pageStartChanged = true;
    updateDataAndView();
    onSizeChanged();

    // Refresh the view with the data that is currently available.
    setData(data, pageStart);

    // Send a request for new data in the range.
    refresh();
  }

  /**
   * Set the {@link SelectionModel}, optionally triggering an update.
   * 
   * @param selectionModel the new {@link SelectionModel}
   * @param updateSelection true to update selection
   */
  public void setSelectionModel(final SelectionModel<? super T> selectionModel,
      boolean updateSelection) {
    // Remove the old selection model.
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }

    // Set the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new SelectionChangeHandler() {
        public void onSelectionChange(SelectionChangeEvent event) {
          updateSelection();
        }
      });
    }

    // Update the current selection state based on the new model.
    if (updateSelection) {
      updateSelection();
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
    tmpElem.setInnerHTML(html);
    return tmpElem;
  }

  /**
   * Check whether or not the cells in the list depend on the selection state.
   * 
   * @return true if cells depend on selection, false if not
   */
  protected abstract boolean dependsOnSelection();

  /**
   * Construct the HTML that represents the list of items.
   * 
   * @param sb the {@link StringBuilder} to build into
   * @param values the values to render
   * @param start the start index
   * @param selectionModel the {@link SelectionModel}
   */
  protected abstract void emitHtml(StringBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel);

  /**
   * Called when pageStart, pageSize, or data size changes.
   */
  protected void onSizeChanged() {
    // Inform the pager about a change in page start, page size, or data size
    if (pager != null) {
      pager.onRangeOrSizeChanged(listView);
    }
  }

  /**
   * Remove the last element from the list.
   */
  protected void removeLastItem() {
    childContainer.getLastChild().removeFromParent();
  }

  /**
   * Set the contents of the child container.
   * 
   * @param html the html to render in the child
   * @return the new child container
   */
  protected Element renderChildContents(String html) {
    childContainer.setInnerHTML(html);
    return childContainer;
  }

  /**
   * Mark an element as selected or unselected. This is called when a cells
   * selection state changes, but the cell does not depend on selection.
   * 
   * @param elem the element to modify
   * @param selected true if selected, false if not
   */
  protected abstract void setSelected(Element elem, boolean selected);

  /**
   * Update the table based on the current selection.
   */
  protected void updateSelection() {
    // Determine if our selection states are stale.
    boolean dependsOnSelection = dependsOnSelection();
    boolean refreshRequired = false;
    Element cellElem = childContainer.getFirstChildElement();
    int row = pageStart;
    for (T value : data) {
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
          if (cellElem != null) {
            // TODO: do a better check?
            // The cell doesn't depend on selection, so we only need to update
            // the style.
            setSelected(cellElem, selected);
          }
        }
      }
      if (cellElem == null) {
        // TODO: do a better check?
        break;
      }
      cellElem = cellElem.getNextSiblingElement();
      row++;
    }

    // Refresh the entire list if needed.
    if (refreshRequired && dependsOnSelection) {
      setData(data, pageStart);
    }
  }

  /**
   * Schedule a redraw or refresh.
   * 
   * @param redrawOnly if true, only schedule a redraw
   */
  private void scheduleRefresh(boolean redrawOnly) {
    if (!refreshScheduled && !redrawScheduled) {
      Scheduler.get().scheduleDeferred(refreshCommand);
    }
    if (redrawOnly) {
      redrawScheduled = true;
    } else {
      refreshScheduled = true;
    }
  }

  /**
   * Ensure that the data and the view are in a consistent state.
   */
  private void updateDataAndView() {
    // Update the data size.
    int expectedLastIndex = Math.max(0,
        Math.min(pageSize, dataSize - pageStart));
    int lastIndex = data.size() - 1;
    while (lastIndex >= expectedLastIndex) {
      data.remove(lastIndex);
      selectedRows.remove(lastIndex + pageStart);
      lastIndex--;
    }

    // Update the DOM.
    int expectedChildCount = data.size();
    int childCount = childContainer.getChildCount();
    while (childCount > expectedChildCount) {
      removeLastItem();
      childCount--;
    }
  }
}
