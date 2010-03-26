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
package com.google.gwt.bikeshed.list.shared;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A {@link ListModel} that is backed by a list.
 * 
 * @param <T> the data type of the list
 */
public class ListListModel<T> extends AbstractListModel<T> {

  /**
   * A wrapper around a list that updates the model on any change.
   */
  private class ListWrapper implements List<T> {

    /**
     * The current size of the list.
     */
    private int curSize = 0;

    /**
     * We wait until the end of the current event loop before flushing changes
     * so that we don't spam the views. This also allows users to clear and
     * replace all of the data without forcing the view back to page 0.
     */
    private Command flushCommand = new Command() {
      public void execute() {
        int newSize = list.size();
        if (curSize != newSize) {
          curSize = newSize;
          updateDataSize(curSize, true);
        }
        
        if (modified) {
          int length = maxModified - minModified;
          updateViewData(minModified, length, list.subList(minModified, maxModified));
          modified = false;
        }
        minModified = Integer.MAX_VALUE;
        maxModified = Integer.MIN_VALUE;
        flushPending = false;
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
     * If modified is true, the smallest modified index.
     */
    private int maxModified;

    /**
     * If modified is true, one past the largest modified index.
     */
    private int minModified;

    /**
     * True if the list data has been modified.
     */
    private boolean modified;

    public ListWrapper(List<T> list) {
      this.list = list;
      minModified = 0;
      maxModified = list.size();
      modified = true;
    }

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

    public boolean add(T e) {
      boolean toRet = list.add(e);
      minModified = Math.min(minModified, size() - 1);
      maxModified = size();
      modified = true;
      return flush(toRet);
    }

    public boolean addAll(Collection<? extends T> c) {
      minModified = Math.min(minModified, size());
      boolean toRet = list.addAll(c);
      maxModified = size();
      modified = true;
      return flush(toRet);
    }

    public boolean addAll(int index, Collection<? extends T> c) {
      try {
        boolean toRet = list.addAll(index, c);
        minModified = Math.min(minModified, index);
        maxModified = size();
        modified = true;
        return flush(toRet);
      } catch (IndexOutOfBoundsException e) {
        throw new IndexOutOfBoundsException(e.getMessage());
      }
    }

    public void clear() {
      list.clear();
      minModified = maxModified = 0;
      modified = true;
      flush();
    }

    public boolean contains(Object o) {
      return list.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
      return list.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
      return list.equals(o);
    }

    public T get(int index) {
      return list.get(index);
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    public boolean isEmpty() {
      return list.isEmpty();
    }

    public Iterator<T> iterator() {
      // TODO(jlabanca): Wrap the iterator
      return list.iterator();
    }

    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    public ListIterator<T> listIterator() {
      // TODO(jlabanca): Wrap the iterator
      return list.listIterator();
    }

    public ListIterator<T> listIterator(int index) {
      // TODO(jlabanca): Wrap the iterator
      return list.listIterator(index);
    }

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

    public boolean remove(Object o) {
      int index = indexOf(o);
      if (index == -1) {
        return false;
      }
      remove(index);
      return true;
    }

    public boolean removeAll(Collection<?> c) {
      boolean toRet = list.removeAll(c);
      minModified = 0;
      maxModified = size();
      modified = true;
      return flush(toRet);
    }

    public boolean retainAll(Collection<?> c) {
      boolean toRet = list.retainAll(c);
      minModified = 0;
      maxModified = size();
      modified = true;
      return flush(toRet);
    }

    public T set(int index, T element) {
      T toRet = list.set(index, element);
      minModified = Math.min(minModified, index);
      maxModified = Math.max(maxModified, index + 1);
      modified = true;
      flush();
      return toRet;
    }

    public int size() {
      return list.size();
    }

    public List<T> subList(int fromIndex, int toIndex) {
      // TODO(jlabanca): wrap the sublist
      return list.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
      return list.toArray();
    }

    public <C> C[] toArray(C[] a) {
      return list.toArray(a);
    }

    /**
     * Flush the data to the model and return the boolean.
     * 
     * @param toRet the boolean to return
     */
    private boolean flush(boolean toRet) {
      flush();
      return toRet;
    }

    /**
     * Flush the data to the model.
     */
    private void flush() {
      if (!flushPending) {
        flushPending = true;
        DeferredCommand.addCommand(flushCommand);
      }
    }
  }

  /**
   * The wrapper around the actual list.
   */
  private ListWrapper listWrapper = new ListWrapper(new ArrayList<T>());

  /**
   * Get the list that backs this model. Changes to the list will be reflected
   * in the model.
   * 
   * @return the list
   */
  public List<T> getList() {
    return listWrapper;
  }

  @Override
  protected void onRangeChanged(int start, int length) {
    // TODO - update size only when needed
    // TODO - update only relevant range of data
    updateDataSize(listWrapper.size(), true);
    updateViewData(0, listWrapper.size(), listWrapper);
  }
}
