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

import java.util.ArrayList;
import java.util.List;

/**
 * An ordered list containing the sort history of {@link Column}s in a table.
 * The 0th item is the {@link ColumnSortInfo} of the most recently sorted
 * column.
 */
public class ColumnSortList {

  /**
   * Information about the sort order of a specific column in a table.
   */
  public static class ColumnSortInfo {

    private final boolean ascending;
    private final Column<?, ?> column;

    /**
     * Construct a new {@link ColumnSortInfo}.
     * 
     * @param column the column index
     * @param ascending true if sorted ascending
     */
    public ColumnSortInfo(Column<?, ?> column, boolean ascending) {
      this.column = column;
      this.ascending = ascending;
    }

    /**
     * Default constructor used for RPC.
     */
    ColumnSortInfo() {
      this(null, true);
    }

    /**
     * Check if this object is equal to another. The objects are equal if the
     * column and ascending values are the equal.
     * 
     * @param obj the object to check for equality
     * @return true if objects are the same
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (!(obj instanceof ColumnSortInfo)) {
        return false;
      }

      ColumnSortInfo other = (ColumnSortInfo) obj;
      return equalsOrBothNull(getColumn(), other.getColumn())
          && isAscending() == other.isAscending();
    }

    /**
     * Get the {@link Column} that was sorted.
     * 
     * @return the {@link Column}
     */
    public Column<?, ?> getColumn() {
      return column;
    }

    @Override
    public int hashCode() {
      return 31 * (column == null ? 0 : column.hashCode()) + (ascending ? 1 : 0);
    }

    /**
     * Check if the column was sorted in ascending or descending order.
     * 
     * @return true if ascending, false if descending
     */
    public boolean isAscending() {
      return ascending;
    }

    private boolean equalsOrBothNull(Object a, Object b) {
      return a == null ? b == null : a.equals(b);
    }
  }

  /**
   * The delegate that handles modifications to the list.
   */
  public static interface Delegate {

    /**
     * Called when the list is modified.
     */
    void onModification();
  }

  /**
   * The delegate that handles modifications.
   */
  private final Delegate delegate;

  /**
   * A List used to manage the insertion/removal of {@link ColumnSortInfo}.
   */
  private final List<ColumnSortInfo> infos = new ArrayList<ColumnSortInfo>();

  /**
   * This limit prevents the infos list to grow over a given size. The default value (0) means
   * that the size can grow indefinitely.
   */
  private int limit = 0;

  /**
   * Construct a new {@link ColumnSortList} without a {@link Delegate}.
   */
  public ColumnSortList() {
    this(null);
  }

  /**
   * Construct a new {@link ColumnSortList} with the specified {@link Delegate}.
   * 
   * @param delegate the {@link Delegate} to inform of modifications
   */
  public ColumnSortList(Delegate delegate) {
    this.delegate = delegate;
  }

  /**
   * Removes all of the elements from this list.
   */
  public void clear() {
    infos.clear();
    fireDelegate();
  }

  /**
   * Check if the specified object equals this list. Two {@link ColumnSortList}
   * are equals if they are the same size, and all entries are
   * <code>equals</code> and in the same order.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof ColumnSortList)) {
      return false;
    }

    // Check the size of the lists.
    ColumnSortList other = (ColumnSortList) obj;
    return infos.equals(other.infos);
  }

  /**
   * Get the {@link ColumnSortInfo} at the specified index.
   * 
   * @param index the index
   * @return the {@link ColumnSortInfo}
   */
  public ColumnSortInfo get(int index) {
    return infos.get(index);
  }

  /**
   * Get the actual limit value
   * 
   * @return the actual limit value
   */
  public int getLimit() {
    return limit;
  }

  @Override
  public int hashCode() {
    return 31 * infos.hashCode() + 13;
  }

  /**
   * Inserts the specified {@link ColumnSortInfo} at the specified position in
   * this list. If the column already exists in the sort info, the index will be
   * adjusted to account for any removed entries.
   * 
   * @param sortInfo the {@link ColumnSortInfo} to add
   */
  public void insert(int index, ColumnSortInfo sortInfo) {
    if (sortInfo == null) {
      throw new IllegalArgumentException("sortInfo cannot be null");
    }

    // Remove sort info for duplicate columns
    Column<?, ?> column = sortInfo.getColumn();
    for (int i = 0; i < infos.size(); i++) {
      ColumnSortInfo curInfo = infos.get(i);
      if (curInfo.getColumn() == column) {
        infos.remove(i);
        if (i < index) {
          index--;
        }
        i--;
      }
    }
    
    if (limit > 0) {
      // at this point, infos.size() must not exceed the limit, a simple condition check is enough
      if (limit == infos.size()) {
        infos.remove(infos.size() - 1);
      }
      // by the contract of the limit, inserting after the limit must not be allowed
      if (index >= limit) {
        throw new IndexOutOfBoundsException("Index: " + index + ", Limit: " + limit);
      }
    }

    // Insert the new sort info
    infos.add(index, sortInfo);
    fireDelegate();
  }

  /**
   * Push a {@link Column} onto the list at index zero, setting ascending to
   * true. If the column already exists, it will be removed from its current
   * position and placed at the start of the list. If the Column is already at
   * the start of the list, its ascending bit will be flipped (ascending to
   * descending and vice versa).
   * 
   * @param column the {@link Column} to push
   * @return the {@link ColumnSortInfo} that was pushed
   */
  public ColumnSortInfo push(Column<?, ?> column) {
    // If the column matches the primary column, toggle the order.
    boolean ascending = (column == null) ? true : column.isDefaultSortAscending();
    if (size() > 0 && get(0).getColumn() == column) {
      ascending = !get(0).isAscending();
    }

    // Push the new column.
    ColumnSortInfo toRet = new ColumnSortInfo(column, ascending);
    push(toRet);
    return toRet;
  }

  /**
   * Push a {@link ColumnSortInfo} onto the list at index zero. If the column
   * already exists, it will be removed from its current position and placed at
   * the start of the list.
   * 
   * @param sortInfo the {@link ColumnSortInfo} to push
   */
  public void push(ColumnSortInfo sortInfo) {
    insert(0, sortInfo);
  }

  /**
   * Remove a {@link ColumnSortInfo} from the list.
   * 
   * @param sortInfo the {@link ColumnSortInfo} to remove
   */
  public boolean remove(ColumnSortInfo sortInfo) {
    boolean toRet = infos.remove(sortInfo);
    fireDelegate();
    return toRet;
  }

  /**
   * Set the limit to a positive value to prevent the growth of the infos list over the given size.
   * This method will check if the actual infos list is over the limit, and it will fire the
   * delegate in the case it should remove items from the list.
   * 
   * The default value (0) means the size can grow indefinitely. 
   * 
   * @param limit the new limit value
   */
  public void setLimit(int limit) {
    this.limit = limit;
    
    if (limit > 0) {
      // checking the list size, as it might have been populated over the limit
      boolean modified = false;
      while (limit < infos.size()) {
        infos.remove(infos.size() - 1);
        modified = true;
      }
      if (modified) {
        fireDelegate();
      }
    }
  }

  /**
   * Get the size of the list.
   * 
   * @return the number of {@link ColumnSortInfo} in the list
   */
  public int size() {
    return infos.size();
  }

  private void fireDelegate() {
    if (delegate != null) {
      delegate.onModification();
    }
  }
}
