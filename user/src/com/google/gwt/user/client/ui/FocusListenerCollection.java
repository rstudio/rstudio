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
 * A helper class for implementers of the
 * {@link com.google.gwt.user.client.ui.SourcesFocusEvents} interface. This
 * subclass of {@link ArrayList} assumes that all objects added to it will be of
 * type {@link com.google.gwt.user.client.ui.FocusListener}
 * 
 * @deprecated Widgets should now manage their own handlers via {@link Widget#addDomHandler}
 */
@Deprecated
public class FocusListenerCollection extends ArrayList<FocusListener> {

  /**
   * Fires a focus event to all listeners.
   * 
   * @param sender the widget sending the event.
   */
  public void fireFocus(Widget sender) {
    for (FocusListener listener : this) {
      listener.onFocus(sender);
    }
  }

  /**
   * A helper for widgets that source focus events.
   * 
   * @param sender the widget sending the event.
   * @param event the {@link Event DOM event} received by the widget.
   */
  public void fireFocusEvent(Widget sender, Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONFOCUS:
        fireFocus(sender);
        break;

      case Event.ONBLUR:
        fireLostFocus(sender);
        break;
    }
  }

  /**
   * Fires a lost-focus event to all listeners.
   * 
   * @param sender the widget sending the event.
   */
  public void fireLostFocus(Widget sender) {
    for (FocusListener listener : this) {
      listener.onLostFocus(sender);
    }
  }
}
