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

import java.util.ArrayList;

/**
 * A helper class for implementers of the SourcesMouseWheelEvents interface.
 * This subclass of {@link ArrayList} assumes that all objects added to it will
 * be of type {@link com.google.gwt.user.client.ui.MouseWheelListener}.
 * 
 * @deprecated Widgets should now manage their own handlers via {@link Widget#addDomHandler}
 */
@Deprecated
public class MouseWheelListenerCollection extends ArrayList<MouseWheelListener> {

  /**
   * Fires a mouse wheel event to all listeners.
   * 
   * @param sender the widget sending the event
   * @param velocity the velocity information for the event
   */
  public void fireMouseWheel(Widget sender, MouseWheelVelocity velocity) {
    for (MouseWheelListener listener : this) {
      listener.onMouseWheel(sender, velocity);
    }
  }

  /**
   * A helper for widgets that source mouse events.
   * 
   * @param sender the widget sending the event
   * @param event the {@link Event} received by the widget
   */
  public void fireMouseWheelEvent(Widget sender, Event event) {
    if (DOM.eventGetType(event) == Event.ONMOUSEWHEEL) {
      MouseWheelVelocity velocity = new MouseWheelVelocity(event);
      fireMouseWheel(sender, velocity);
    }
  }
}
