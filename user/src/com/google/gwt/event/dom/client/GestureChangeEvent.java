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
 * Represents a native gesture change event.
 */
public class GestureChangeEvent extends DomEvent<GestureChangeHandler> {

  /**
   * Event type for gesture change events. Represents the meta-data associated
   * with this event.
   */
  private static final Type<GestureChangeHandler> TYPE = new Type<
      GestureChangeHandler>(BrowserEvents.GESTURECHANGE, new GestureChangeEvent());

  /**
   * Gets the event type associated with gesture change events.
   *
   * @return the handler type
   */
  public static Type<GestureChangeHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use {@link
   * DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent,
   * com.google.gwt.event.shared.HasHandlers)} to fire gesture change events.
   */
  protected GestureChangeEvent() {
  }

  @Override
  public final Type<GestureChangeHandler> getAssociatedType() {
    return TYPE;
  }

  public double getRotation() {
    return getNativeEvent().getRotation();
  }

  public double getScale() {
    return getNativeEvent().getScale();
  }

  @Override
  protected void dispatch(GestureChangeHandler handler) {
    handler.onGestureChange(this);
  }
}
