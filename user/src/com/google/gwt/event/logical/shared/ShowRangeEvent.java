/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents a show range event. This logical event should be used when a
 * widget displays a range of values to the user.
 * 
 * @param <V> the type of range
 */
public class ShowRangeEvent<V> extends GwtEvent<ShowRangeHandler<V>> {

  /**
   * Handler type.
   */
  private static Type<ShowRangeHandler<?>> TYPE;

  /**
   * Fires a show range event on all registered handlers in the handler manager.
   * 
   * @param <V> the type of range
   * @param <S> the event source
   * @param source the source of the handlers
   * @param start the start of the range
   * @param end the end of the range
   */
  public static <V, S extends HasShowRangeHandlers<V> & HasHandlers> void fire(
      S source, V start, V end) {
    if (TYPE != null) {
      ShowRangeEvent<V> event = new ShowRangeEvent<V>(start, end);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<ShowRangeHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<ShowRangeHandler<?>>();
    }
    return TYPE;
  }

  private final V start;

  private final V end;

  /**
   * Creates a new show range event.
   * 
   * @param start start of the range
   * @param end end of the range
   */
  protected ShowRangeEvent(V start, V end) {
    this.start = start;
    this.end = end;
  }

  // Because of type erasure, our static type is
  // wild carded, yet the "real" type should use our I param.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<ShowRangeHandler<V>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the end of the range.
   * 
   * @return end of the range
   */
  public V getEnd() {
    return end;
  }

  /**
   * Gets the start of the range.
   * 
   * @return start of the range
   */
  public V getStart() {
    return start;
  }

  @Override
  protected void dispatch(ShowRangeHandler<V> handler) {
    handler.onShowRange(this);
  }
}
