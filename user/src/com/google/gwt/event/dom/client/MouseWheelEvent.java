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
package com.google.gwt.event.dom.client;

import com.google.gwt.dom.client.BrowserEvents;

/**
 * Represents a native mouse wheel event.
 */
public class MouseWheelEvent extends MouseEvent<MouseWheelHandler> {

  /**
   * Event type for mouse wheel events. Represents the meta-data associated with
   * this event.
   */
  private static final Type<MouseWheelHandler> TYPE = new Type<MouseWheelHandler>(
      BrowserEvents.MOUSEWHEEL, new MouseWheelEvent());

  static {
    /**
     * Hidden type used to ensure DOMMouseScroll gets registered in the type map.
     * This is the special name used on Mozilla browsers for what everyone else
     * calls 'mousewheel'.
     */
    new Type<MouseWheelHandler>("DOMMouseScroll", new MouseWheelEvent());
  }

  /**
   * Gets the event type associated with mouse wheel events.
   * 
   * @return the handler type
   */
  public static Type<MouseWheelHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use
   * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
   * to fire mouse wheel events.
   */
  protected MouseWheelEvent() {
  }

  @Override
  public final Type<MouseWheelHandler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Get the change in the mouse wheel position along the Y-axis; positive if
   * the mouse wheel is moving north (toward the top of the screen) or negative
   * if the mouse wheel is moving south (toward the bottom of the screen).
   * 
   * Note that delta values are not normalized across browsers or OSes.
   * 
   * @return the delta of the mouse wheel along the y axis
   */
  public int getDeltaY() {
    return getNativeEvent().getMouseWheelVelocityY();
  }

  /**
   * Convenience method that returns <code>true</code> if {@link #getDeltaY()}
   * is a negative value (ie, the velocity is directed toward the top of the
   * screen).
   * 
   * @return true if the velocity is directed toward the top of the screen
   */
  public boolean isNorth() {
    return getDeltaY() < 0;
  }

  /**
   * Convenience method that returns <code>true</code> if {@link #getDeltaY()}
   * is a positive value (ie, the velocity is directed toward the bottom of the
   * screen).
   * 
   * @return true if the velocity is directed toward the bottom of the screen
   */
  public boolean isSouth() {
    return getDeltaY() > 0;
  }

  @Override
  protected void dispatch(MouseWheelHandler handler) {
    handler.onMouseWheel(this);
  }
}
