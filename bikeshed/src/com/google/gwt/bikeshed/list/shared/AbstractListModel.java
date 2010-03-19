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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ListModel}.
 * 
 * @param <T> the data type of records in the list
 */
public abstract class AbstractListModel<T> implements ListModel<T> {

  /**
   * The range of interest for a single handler.
   */
  public static class DefaultRange implements Range, Serializable {
    private int start;
    private int length;

    public DefaultRange(int start, int length) {
      this.start = start;
      this.length = length;
    }

    /**
     * Used by RPC.
     */
    DefaultRange() {
    }

    public int getLength() {
      return length;
    }

    public int getStart() {
      return start;
    }
  }

  /**
   * An implementation of {@link ListRegistration} that remembers its own range
   * of interest.
   */
  private class DefaultListRegistration implements ListRegistration {

    private final ListHandler<T> handler;
    private int start;
    private int length = -1;

    /**
     * Construct a new {@link DefaultListRegistration}.
     * 
     * @param handler the handler being registered
     */
    public DefaultListRegistration(ListHandler<T> handler) {
      this.handler = handler;
    }

    public void removeHandler() {
      if (!registrations.contains(this)) {
        throw new IllegalStateException("ListHandler has already been removed.");
      }
      registrations.remove(this);
    }

    public void setRangeOfInterest(int start, int length) {
      this.start = start;
      this.length = length;
      onRangeChanged(start, length);
    }

    protected ListHandler<T> getHandler() {
      return handler;
    }

    protected int getLength() {
      return length;
    }

    protected int getStart() {
      return start;
    }
  }

  /**
   * The handlers that are listening to this model.
   */
  private List<DefaultListRegistration> registrations = new ArrayList<DefaultListRegistration>();

  public ListRegistration addListHandler(ListHandler<T> handler) {
    DefaultListRegistration reg = new DefaultListRegistration(handler);
    registrations.add(reg);
    return reg;
  }

  /**
   * Get the current ranges of all views.
   * 
   * @return the ranges
   */
  public Range[] getRanges() {
    Range[] ranges = new Range[registrations.size()];
    for (int i = 0; i < registrations.size(); i++) {
      DefaultListRegistration reg = registrations.get(i);
      ranges[i] = new DefaultRange(reg.getStart(), reg.getLength());
    }
    return ranges;
  }

  /**
   * Called when a view changes its range of interest.
   */
  protected abstract void onRangeChanged(int start, int length);

  /**
   * Inform the views of the total number of items that are available.
   * 
   * @param size the new size
   * @param exact true if the size is exact, false if it is a guess
   */
  protected void updateDataSize(int size, boolean exact) {
    SizeChangeEvent event = new SizeChangeEvent(size, exact);
    for (DefaultListRegistration reg : registrations) {
      reg.getHandler().onSizeChanged(event);
    }
  }

  /**
   * Inform the views of the new data.
   * 
   * @param start the start index
   * @param length the length of the data
   * @param values the data values
   */
  protected void updateViewData(int start, int length, List<T> values) {
    int end = start + length;
    for (DefaultListRegistration reg : registrations) {
      int curStart = reg.getStart();
      int curLength = reg.getLength();
      int curEnd = curStart + curLength;
      if (curStart < end && curEnd > start) {
        // Fire the handler with the data that is in the range.
        int realStart = curStart < start ? start : curStart;
        int realEnd = curEnd > end ? end : curEnd;
        int realLength = realEnd - realStart;
        List<T> realValues = values.subList(0, realLength);
        ListEvent<T> event = new ListEvent<T>(realStart, realLength, realValues);
        reg.getHandler().onDataChanged(event);
      }
    }
  }
}
