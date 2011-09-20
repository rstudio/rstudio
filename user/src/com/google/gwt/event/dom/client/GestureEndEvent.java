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
package com.google.gwt.event.dom.client;

import com.google.gwt.dom.client.BrowserEvents;

/**
 * Represents a native gesture end event.
 */
public class GestureEndEvent extends DomEvent<GestureEndHandler> {

  /**
   * Event type for gesture end events. Represents the meta-data associated with
   * this event.
   */
  private static final Type<GestureEndHandler> TYPE = new Type<
      GestureEndHandler>(BrowserEvents.GESTUREEND, new GestureEndEvent());

  /**
   * Gets the event type associated with gesture end events.
   *
   * @return the handler type
   */
  public static Type<GestureEndHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use {@link
   * DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent,
   * com.google.gwt.event.shared.HasHandlers)} to fire gesture end events.
   */
  protected GestureEndEvent() {
  }

  @Override
  public final Type<GestureEndHandler> getAssociatedType() {
    return TYPE;
  }

  public double getRotation() {
    return getNativeEvent().getRotation();
  }

  public double getScale() {
    return getNativeEvent().getScale();
  }

  @Override
  protected void dispatch(GestureEndHandler handler) {
    handler.onGestureEnd(this);
  }
}
