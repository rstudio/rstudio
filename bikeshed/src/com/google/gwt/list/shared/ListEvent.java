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
package com.google.gwt.list.shared;

import com.google.gwt.event.shared.GwtEvent;

import java.util.List;

/**
 * Represents a list event.
 * 
 * @param <T> the type of data in the list
 */
public class ListEvent<T> extends GwtEvent<ListHandler<T>> {

  /**
   * Handler type.
   */
  private static Type<ListHandler<?>> TYPE;

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<ListHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<ListHandler<?>>();
    }
    return TYPE;
  }

  private final int length;
  private final int start;
  private final List<T> values;

  /**
   * Creates a {@link ListEvent}.
   * 
   * @param start the start index of the data
   * @param length the length of the data
   * @param values the new values
   */
  public ListEvent(int start, int length, List<T> values) {
    this.start = start;
    this.length = length;
    this.values = values;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Type<ListHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Get the length of the changed data set.
   * 
   * @return the length of the data set
   */
  public int getLength() {
    return length;
  }

  /**
   * Get the start index of the changed data.
   * 
   * @return the start index
   */
  public int getStart() {
    return start;
  }

  /**
   * Gets the value.
   * 
   * @return the value
   */
  public List<T> getValues() {
    return values;
  }

  @Override
  protected void dispatch(ListHandler<T> handler) {
    handler.onDataChanged(this);
  }
}
