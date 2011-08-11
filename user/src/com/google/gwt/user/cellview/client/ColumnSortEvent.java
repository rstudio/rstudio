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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.view.client.HasData;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a column sort event.
 */
public class ColumnSortEvent extends GwtEvent<ColumnSortEvent.Handler> {

  /**
   * Handler for {@link ColumnSortEvent}.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when {@link ColumnSortEvent} is fired.
     * 
     * @param event the {@link ColumnSortEvent} that was fired
     */
    void onColumnSort(ColumnSortEvent event);
  }

  /**
   * A default handler used with views attached to asynchronous data providers
   * such as {@link com.google.gwt.view.client.AsyncDataProvider AsyncDataProvider}.
   * This handler calls
   * {@link HasData#setVisibleRangeAndClearData(com.google.gwt.view.client.Range, boolean)},
   * which clears the current data and triggers the data provider's range change
   * handler.
   */
  public static class AsyncHandler implements Handler {
    private final HasData<?> hasData;

    public AsyncHandler(HasData<?> hasData) {
      this.hasData = hasData;
    }

    public void onColumnSort(ColumnSortEvent event) {
      hasData.setVisibleRangeAndClearData(hasData.getVisibleRange(), true);
    }
  }

  /**
   * <p>
   * A default handler used to sort a {@link List} backing a table. If the
   * sorted column has an associated {@link Comparator}, the list is sorted
   * using the comparator.
   * </p>
   * 
   * <p>
   * This can be used in conjunction with
   * {@link com.google.gwt.view.client.ListDataProvider}.
   * </p>
   * 
   * @param <T> the data type of the list
   */
  public static class ListHandler<T> implements Handler {
    private final Map<Column<?, ?>, Comparator<T>> comparators = new HashMap<Column<?, ?>, Comparator<T>>();
    private List<T> list;

    public ListHandler(List<T> list) {
      this.list = list;
    }

    /**
     * Returns the comparator that has been set for the specified column, or 
     * null if no comparator has been set.
     * 
     * @param column the {@link Column}
     */
    public Comparator<T> getComparator(Column<T, ?> column) {
      return comparators.get(column);
    }

    public List<T> getList() {
      return list;
    }

    public void onColumnSort(ColumnSortEvent event) {
      // Get the sorted column.
      Column<?, ?> column = event.getColumn();
      if (column == null) {
        return;
      }

      // Get the comparator.
      final Comparator<T> comparator = comparators.get(column);
      if (comparator == null) {
        return;
      }

      // Sort using the comparator.
      if (event.isSortAscending()) {
        Collections.sort(list, comparator);
      } else {
        Collections.sort(list, new Comparator<T>() {
          public int compare(T o1, T o2) {
            return -comparator.compare(o1, o2);
          }
        });
      }
    }

    /**
     * Set the comparator used to sort the specified column in ascending order.
     * 
     * @param column the {@link Column}
     * @param comparator the {@link Comparator} to use for the {@link Column}
     */
    public void setComparator(Column<T, ?> column, Comparator<T> comparator) {
      comparators.put(column, comparator);
    }

    public void setList(List<T> list) {
      assert list != null : "list cannot be null"; 
      this.list = list;
    }
  }

  /**
   * Handler type.
   */
  private static Type<Handler> TYPE;

  /**
   * Fires a cell preview event on all registered handlers in the handler
   * manager. If no such handlers exist, this implementation will do nothing.
   * 
   * @param source the source of the event
   * @param sortList the {@link ColumnSortList} of sorted columns
   * @return the {@link ColumnSortEvent} that was fired
   */
  public static ColumnSortEvent fire(HasHandlers source, ColumnSortList sortList) {
    ColumnSortEvent event = new ColumnSortEvent(sortList);
    if (TYPE != null) {
      source.fireEvent(event);
    }
    return event;
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<Handler>();
    }
    return TYPE;
  }

  private final ColumnSortList sortList;

  /**
   * Construct a new {@link ColumnSortEvent}.
   * 
   * @param sortList the {@link ColumnSortList}
   */
  protected ColumnSortEvent(ColumnSortList sortList) {
    this.sortList = sortList;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Get the {@link Column} that was sorted.
   * 
   * @return the sorted {@link Column}, or null if not sorted
   */
  public Column<?, ?> getColumn() {
    return (sortList == null || sortList.size() == 0) ? null
        : sortList.get(0).getColumn();
  }

  /**
   * Get the {@link ColumnSortList} that contains the ordered list of sorted
   * columns.
   * 
   * @return the {@link ColumnSortList}
   */
  public ColumnSortList getColumnSortList() {
    return sortList;
  }

  /**
   * Check if the {@link Column} is sorted in ascending order.
   * 
   * @return true if ascending, false if descending or not sorted
   */
  public boolean isSortAscending() {
    return (sortList == null || sortList.size() == 0) ? false
        : sortList.get(0).isAscending();
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onColumnSort(this);
  }
}
