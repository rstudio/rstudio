/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * Encapsulates the direction and velocity of mouse wheel events.  Not all
 * combinations of browser and user input devices can generate all combinations
 * of direction or range of velocity information.
 *
 * @see com.google.gwt.user.client.DOM#eventGetMouseWheelVelocityY
 *   An explanation of the units used for mouse wheel velocity.
 */
public class MouseWheelVelocity {

  /**
   * Stores the Y-axis velocity.
   */
  protected final int vY;

  /**
   *  Construct the higher-level view of the original ONMOUSEWHEEL Event.
   * @param e the event
   */
  public MouseWheelVelocity(Event e) {
    vY = DOM.eventGetMouseWheelVelocityY(e);
  }

  public boolean equals(Object o) {
    if (o instanceof MouseWheelVelocity) {
      MouseWheelVelocity v = (MouseWheelVelocity)o;
      return getVelocityY() == v.getVelocityY();
    }

    return false;
  }

  /**
   * @return The velocity of the mouse wheel event along the Y-axis
   */
  public int getVelocityY() {
    return vY;
  }

  public int hashCode() {
    return getVelocityY();
  }

  /**
   * @return <code>true</code> if the velocity includes a component directed
   *         toword the top of the screen
   */
  public boolean isNorth() {
    return getVelocityY() < 0;
  }

  /**
   * @return <code>true</code> if the velocity includes a component directed
   *         toword the bottom of the screen
   */
  public boolean isSouth() {
    return getVelocityY() > 0;
  }

  public String toString() {
    return "<" + getVelocityY() + ">";
  }
}
