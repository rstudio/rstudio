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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MouseWheelVelocity;

/**
 * Represents a native mouse wheel event.
 */
public class MouseWheelEvent extends MouseEvent {

  /**
   * Event Type for MouseWheel. Represents the meta-data associated with this
   * event.
   */
  public static final Type<MouseWheelEvent, MouseWheelHandler> TYPE = new Type<MouseWheelEvent, MouseWheelHandler>(
      Event.ONMOUSEWHEEL) {
    @Override
    public void fire(MouseWheelHandler handler, MouseWheelEvent event) {
      handler.onMouseWheel(event);
    }

    @Override
    MouseWheelEvent wrap(Event nativeEvent) {
      return new MouseWheelEvent(nativeEvent);
    }
  };

  /**
   * Constructs a MouseWheelEvent event.
   * 
   * @param nativeEvent the native event object wrapped by this event
   */
  public MouseWheelEvent(Event nativeEvent) {
    super(nativeEvent);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MouseWheelVelocity) {
      MouseWheelVelocity v = (MouseWheelVelocity) o;
      return getDeltaY() == v.getDeltaY();
    }

    return false;
  }

  /**
   * @return the change in the mouse wheel position along the Y-axis; positive
   *         if the mouse wheel is moving north (toward the top of the screen)
   *         or negative if the mouse wheel is moving south (toward the bottom
   *         of the screen)
   */
  public int getDeltaY() {
    return DOM.eventGetMouseWheelVelocityY(getNativeEvent());
  }

  @Override
  public int hashCode() {
    return getDeltaY();
  }

  /**
   * Convenience method that returns <code>true</code> if {@link #getDeltaY()}
   * is a negative value.
   * 
   * @return <code>true</code> if the velocity includes a component directed
   *         toword the top of the screen
   */
  public boolean isNorth() {
    return getDeltaY() < 0;
  }

  /**
   * Convenience method that returns <code>true</code> if {@link #getDeltaY()}
   * is a positive value.
   * 
   * @return <code>true</code> if the velocity includes a component directed
   *         toward the bottom of the screen
   */
  public boolean isSouth() {
    return getDeltaY() > 0;
  }

  @Override
  public String toString() {
    return "<" + getDeltaY() + ">";
  }

  @Override
  protected Type getType() {
    return TYPE;
  }

}
