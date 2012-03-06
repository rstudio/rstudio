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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A concrete subclass of {@link AbstractDataProvider} that is backed by an
 * in-memory list.
 * 
 * <p>
 * Modifications (inserts, removes, sets, etc.) to the list returned by
 * {@link #getList()} will be reflected in the model. However, mutations to the
 * items contained within the list will NOT be reflected in the model. You must
 * call {@link List#set(int, Object)} to update the item within the list and
 * push the change to the display, or call {@link #refresh()} to push all rows
 * to the displays. {@link List#set(int, Object)} performs better because it
 * allows the data provider to push only those rows which have changed, and
 * usually allows the display to re-render only a subset of the rows.
 * </p>
 * 
 * <p>
 * <h3>Example</h3> {@example
 * com.google.gwt.examples.view.ListDataProviderExample}
 * </p>
 * 
 * @param <T> the data type of the list
 */
public class ListDataProvider<T> extends AbstractDataProvider<T> {

  /**
   * A wrapper around a list that updates the model on any change.
   */
  private class ListWrapper implements List<T> {

    /**
     * A wrapped ListIterator.
     */
    private final class WrappedListIterator implements ListIterator<T> {

      /**
       * The error message when {@link #add(Object)} or {@link #remove()} is
       * called more than once per call to {@link #next()} or
       * {@link #previous()}.
       */
      private static final String IMPERMEABLE_EXCEPTION =
          "Cannot call add/remove more than once per call to next/previous.";

      /**
       * The index of the object that will be returned by {@link #next()}.
       */
      private int i = 0;

      /**
       * The index of the last object accessed through {@link #next()} or
       * {@link #previous()}.
       */
      private int last = -1;

      private WrappedListIterator() {
      }

      private WrappedListIterator(int start) {
        int size = ListWrapper.this.size();
        if (start < 0 || start > size) {
          throw new IndexOutOfBoundsException(
              "Index: " + start + ", Size: " + size);
        }
        i = start;
      }

      @Override
      public void add(T o) {
        if (last < 0) {
          throw new IllegalStateException(IMPERMEABLE_EXCEPTION);
        }
        ListWrapper.this.add(i++, o);
        last = -1;
      }

      @Override
      public boolean hasNext() {
        return i < ListWrapper.this.size();
      }

      @Override
      public boolean hasPrevious() {
        return i > 0;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return ListWrapper.this.get(last = i++);
      }

      @Override
      public int nextIndex() {
        return i;
      }

      @Override
      public T previous() {
        if (!hasPrevious()) {
          throw new NoSuchElementException();
        }
        return ListWrapper.this.get(last = --i);
      }

      @Override
      public int previousIndex() {
        return i - 1;
      }

      @Override
      public void remove() {
        if (last < 0) {
          throw new IllegalStateException(IMPERMEABLE_EXCEPTION);
        }
        ListWrapper.this.remove(last);
        i = last;
        last = -1;
      }

      @Override
      public void set(T o) {
        if (last == -1) {
          throw new IllegalStateException();
        }
        ListWrapper.this.set(last, o);
      }
    }

    /**
     * The current size of the list.
     */
    private int curSize = 0;

    /**
     * The delegate wrapper.
     */
    private final ListWrapper delegate;

    /**
     * Set to true if the pending flush has been canceled.
     */
    private boolean flushCancelled;

    /**
     * We wait until the end of the current event loop before flushing changes
     * so that we don't spam the displays. This also allows users to clear and
     * replace all of the data without forcing the display back to page 0.
     */
    private ScheduledCommand flushCommand = new ScheduledCommand() {
      @Override
      public void execute() {
        flushPending = false;
        if (flushCancelled) {
          flushCancelled = false;
          return;
        }
        flushNow();
      }
    };

    /**
     * Set to true if a flush is pending.
     */
    private boolean flushPending;

    /**
     * The list that backs the wrapper.
     */
    private List<T> list;

    /**
     * If this is a sublist, the offset it the index relative to the main list.
     */
    private final int offset;

    /**
     * If modified is true, the smallest modified index.
     */
    private int maxModified = Integer.MIN_VALUE;

    /**
     * If modified is true, one past the largest modified index.
     */
    private int minModified = Integer.MAX_VALUE;

    /**
     * True if the list data has been modified.
     */
    private boolean modified;

    public ListWrapper(List<T> list) {
      this(list, null, 0);

      // Initialize the data size based on the size of the input list.
      updateRowCount(list.size(), true);
    }

    /**
     * Construct a new {@link ListWrapper} that delegates flush calls to the
     * specified delegate.
     *
     * @param list the list to wrap
     * @param delegate the delegate
     * @param offset the offset of this list
     */
    private ListWrapper(List<T> list, ListWrapper delegate, int offset) {
      this.list = list;
      this.delegate = delegate;
      this.offset = offset;
    }

    @Override
    public void add(int index, T element) {
      try {
        list.add(index, element);
        minModified = Math.min(minModified, index);
        maxModified = size();
        modified = true;
        flush();
      } catch (IndexOutOfBoundsException e) {
        throw new IndexOutOfBoundsException(e.getMessage());
      }
    }

    @Override
    public boolean add(T e) {
      boolean toRet = list.add(e);
      minModified = Math.min(minModified, size() - 1);
      maxModified = size();
      modified = true;
      flush();
      return toRet;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
      minModified = Math.min(minModified, size());
      boolean toRet = list.addAll(c);
      maxModified = size();
      modified = true;
      flush();
      return toRet;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
      try {
        boolean toRet = list.addAll(index, c);
        minModified = Math.min(minModified, index);
        maxModified = size();
        modified = true;
        flush();
        return toRet;
      } catch (IndexOutOfBoundsException e) {
        throw new IndexOutOfBoundsException(e.getMessage());
      }
    }

    @Override
    public void clear() {
      list.clear();
      minModified = maxModified = 0;
      modified = true;
      flush();
    }

    @Override
    public boolean contains(Object o) {
      return list.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return list.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
      return list.equals(o);
    }

    @Override
    public T get(int index) {
      return list.get(index);
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    @Override
    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
      return listIterator();
    }

    @Override
    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
      return new WrappedListIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      return new WrappedListIterator(index);
    }

    @Override
    public T remove(int index) {
      try {
        T toRet = list.remove(index);
        minModified = Math.min(minModified, index);
        maxModified = size();
        modified = true;
        flush();
        return toRet;
      } catch (IndexOutOfBoundsException e) {
        throw new IndexOutOfBoundsException(e.getMessage());
      }
    }

    @Override
    public boolean remove(Object o) {
      int index = indexOf(o);
      if (index == -1) {
        return false;
      }
      remove(index);
      return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean toRet = list.removeAll(c);
      minModified = 0;
      maxModified = size();
      modified = true;
      flush();
      return toRet;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      boolean toRet = list.retainAll(c);
      minModified = 0;
      maxModified = size();
      modified = true;
      flush();
      return toRet;
    }

    @Override
    public T set(int index, T element) {
      T toRet = list.set(index, element);
      minModified = Math.min(minModified, index);
      maxModified = Math.max(maxModified, index + 1);
      modified = true;
      flush();
      return toRet;
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      return new ListWrapper(list.subList(fromIndex, toIndex), this, fromIndex);
    }

    @Override
    public Object[] toArray() {
      return list.toArray();
    }

    @Override
    public <C> C[] toArray(C[] a) {
      return list.toArray(a);
    }

    /**
     * Flush the data to the model.
     */
    private void flush() {
      // Defer to the delegate.
      if (delegate != null) {
        delegate.minModified = Math.min(
            minModified + offset, delegate.minModified);
        delegate.maxModified = Math.max(
            maxModified + offset, delegate.maxModified);
        delegate.modified = modified || delegate.modified;
        delegate.flush();
        return;
      }

      flushCancelled = false;
      if (!flushPending) {
        flushPending = true;
        Scheduler.get().scheduleFinally(flushCommand);
      }
    }

    /**
     * Flush pending list changes to the displays. By default,
     */
    private void flushNow() {
      // Cancel any pending flush command.
      if (flushPending) {
        flushCancelled = true;
      }

      // Early exit if this list has been replaced in the data provider.
      if (listWrapper != this) {
        return;
      }

      int newSize = list.size();
      if (curSize != newSize) {
        curSize = newSize;
        updateRowCount(curSize, true);
      }

      if (modified) {
        updateRowData(minModified, list.subList(minModified, maxModified));
        modified = false;
      }
      minModified = Integer.MAX_VALUE;
      maxModified = Integer.MIN_VALUE;
    }
  }

  /**
   * The wrapper around the actual list.
   */
  private ListWrapper listWrapper;

  /**
   * Creates an empty model.
   */
  public ListDataProvider() {
    this(new ArrayList<T>(), null);
  }

  /**
   * Creates a list model that wraps the given list.
   * 
   * <p>
   * The wrapped list should no longer be modified as the data provider cannot
   * detect changes to the wrapped list. Instead, call {@link #getList()} to
   * retrieve a wrapper that can be modified and will correctly forward changes
   * to displays.
   * 
   * @param listToWrap the List to be wrapped
   */
  public ListDataProvider(List<T> listToWrap) {
    this(listToWrap, null);
  }

  /**
   * Creates an empty list model that wraps the given collection.
   *
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  public ListDataProvider(ProvidesKey<T> keyProvider) {
    this(new ArrayList<T>(), keyProvider);
  }

  /**
   * Creates a list model that wraps the given list.
   * 
   * <p>
   * The wrapped list should no longer be modified as the data provider cannot
   * detect changes to the wrapped list. Instead, call {@link #getList()} to
   * retrieve a wrapper that can be modified and will correctly forward changes
   * to displays.
   * 
   * @param listToWrap the List to be wrapped
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  public ListDataProvider(List<T> listToWrap, ProvidesKey<T> keyProvider) {
    super(keyProvider);
    listWrapper = new ListWrapper(listToWrap);
  }

  /**
   * Flush pending list changes to the displays. By default, displays are
   * informed of modifications to the underlying list at the end of the current
   * event loop, which makes it possible to perform multiple operations
   * synchronously without repeatedly refreshing the displays. This method can
   * be called to flush the changes immediately instead of waiting until the end
   * of the current event loop.
   */
  public void flush() {
    listWrapper.flushNow();
  }

  /**
   * Get the list that backs this model. Changes to the list will be reflected
   * in the model.
   * 
   * <p>
   * NOTE: Mutations to the items contained within the list will NOT be
   * reflected in the model. You must call {@link List#set(int, Object)} to
   * update the item within the list and push the change to the display, or call
   * {@link #refresh()} to push all rows to the displays.
   * {@link List#set(int, Object)} performs better because it allows the data
   * provider to push only those rows which have changed, and usually allows the
   * display to re-render only a subset of the rows.
   * 
   * @return the list
   * 
   * @see #setList(List)
   */
  public List<T> getList() {
    return listWrapper;
  }

  /**
   * Refresh all of the displays listening to this adapter.
   * 
   * <p>
   * Use {@link #refresh()} to push mutations to the underlying data items
   * contained within the list. The data provider cannot detect changes to data
   * objects within the list, so you must call this method if you modify items.
   * 
   * <p>
   * This is a shortcut for calling {@link List#set(int, Object)} on every item
   * that you modify, but note that calling {@link List#set(int, Object)}
   * performs better because the data provider knows which rows were modified
   * and can push only the modified rows the the displays.
   */
  public void refresh() {
    updateRowData(0, listWrapper);
  }

  /**
   * Replace this model's list with the specified list.
   * 
   * <p>
   * The wrapped list should no longer be modified as the data provider cannot
   * detect changes to the wrapped list. Instead, call {@link #getList()} to
   * retrieve a wrapper that can be modified and will correctly forward changes
   * to displays.
   * 
   * @param listToWrap the model's new list
   * 
   * @see #getList()
   */
  public void setList(List<T> listToWrap) {
    listWrapper = new ListWrapper(listToWrap);
    listWrapper.minModified = 0;
    listWrapper.maxModified = listWrapper.size();
    listWrapper.modified = true;
    flush();
  }

  @Override
  protected void onRangeChanged(HasData<T> display) {
    int size = listWrapper.size();
    if (size > 0) {
      // Do not push data if the data set is empty.
      updateRowData(display, 0, listWrapper);
    }
  }
}
