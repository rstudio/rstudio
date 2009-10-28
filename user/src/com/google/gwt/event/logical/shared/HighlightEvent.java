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
 * Represents a highlight event.
 * 
 * @param <V> the highlighted value type
 */
public class HighlightEvent<V> extends GwtEvent<HighlightHandler<V>> {

  /**
   * Handler type.
   */
  private static Type<HighlightHandler<?>> TYPE;

  /**
   * Fires a highlight event on all registered handlers in the handler manager.
   * 
   * @param <V> the highlighted value type
   * @param <S> The event source
   * @param source the source of the handlers
   * @param highlighted the value highlighted
   */
  public static <V, S extends HasHighlightHandlers<V> & HasHandlers> void fire(
      S source, V highlighted) {
    if (TYPE != null) {
      HighlightEvent<V> event = new HighlightEvent<V>(highlighted);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<HighlightHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<HighlightHandler<?>>();
    }
    return TYPE;
  }

  private final V highlighted;

  /**
   * Creates a new highlight event.
   * 
   * @param highlighted value highlighted
   */
  protected HighlightEvent(V highlighted) {
    this.highlighted = highlighted;
  }

  // Because of type erasure, our static type is
  // wild carded, yet the "real" type should use our I param.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<HighlightHandler<V>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the value highlighted.
   * 
   * @return value highlighted
   */
  public V getHighlighted() {
    return highlighted;
  }

  @Override
  protected void dispatch(HighlightHandler<V> handler) {
    handler.onHighlight(this);
  }
}
