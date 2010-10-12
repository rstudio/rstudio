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
package com.google.gwt.view.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Represents a range change event.
 */
public class RangeChangeEvent extends GwtEvent<RangeChangeEvent.Handler> {

  /**
   * Handler type.
   */
  private static Type<Handler> TYPE;

  /**
   * Handler interface for {@link RangeChangeEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link RangeChangeEvent} is fired.
     *
     * @param event the {@link RangeChangeEvent} that was fired
     */
    void onRangeChange(RangeChangeEvent event);
  }

  /**
   * Fires a {@link RangeChangeEvent} on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   * @param range the new range
   */
  public static void fire(HasRows source, Range range) {
    if (TYPE != null) {
      RangeChangeEvent event = new RangeChangeEvent(range);
      source.fireEvent(event);
    }
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

  private final Range range;

  /**
   * Creates a {@link RangeChangeEvent}.
   *
   * @param range the new range
   */
  protected RangeChangeEvent(Range range) {
    this.range = range;
  }

  @Override
  public final Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Gets the new range.
   *
   * @return the new range
   */
  public Range getNewRange() {
    return range;
  }

  @Override
  public String toDebugString() {
    return super.toDebugString() + getNewRange();
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRangeChange(this);
  }
}
