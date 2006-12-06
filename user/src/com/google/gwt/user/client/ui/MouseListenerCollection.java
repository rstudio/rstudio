/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import java.util.Iterator;
import java.util.Vector;

/**
 * A helper class for implementers of the SourcesMouseEvents interface. This
 * subclass of Vector assumes that all objects added to it will be of type
 * {@link com.google.gwt.user.client.ui.MouseListener}.
 */
public class MouseListenerCollection extends Vector {

  /**
   * Fires a mouse down event to all listeners.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   */
  public void fireMouseDown(Widget sender, int x, int y) {
    for (Iterator it = iterator(); it.hasNext();) {
      MouseListener listener = (MouseListener) it.next();
      listener.onMouseDown(sender, x, y);
    }
  }

  /**
   * Fires a mouse enter event to all listeners.
   * 
   * @param sender the widget sending the event
   */
  public void fireMouseEnter(Widget sender) {
    for (Iterator it = iterator(); it.hasNext();) {
      MouseListener listener = (MouseListener) it.next();
      listener.onMouseEnter(sender);
    }
  }

  /**
   * A helper for widgets that source mouse events.
   * 
   * @param sender the widget sending the event
   * @param event the {@link Event} received by the widget
   */
  public void fireMouseEvent(Widget sender, Event event) {
    int x = DOM.eventGetClientX(event)
        - DOM.getAbsoluteLeft(sender.getElement());
    int y = DOM.eventGetClientY(event)
        - DOM.getAbsoluteTop(sender.getElement());

    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEDOWN:
        fireMouseDown(sender, x, y);
        break;
      case Event.ONMOUSEUP:
        fireMouseUp(sender, x, y);
        break;
      case Event.ONMOUSEMOVE:
        fireMouseMove(sender, x, y);
        break;
      case Event.ONMOUSEOVER:
        // Only fire the mouseEnter event if it's coming from outside this
        // widget.
        Element from = DOM.eventGetFromElement(event);
        if (!DOM.isOrHasChild(sender.getElement(), from)) {
          fireMouseEnter(sender);
        }
        break;
      case Event.ONMOUSEOUT:
        // Only fire the mouseLeave event if it's actually leaving this
        // widget.
        Element to = DOM.eventGetToElement(event);
        if (!DOM.isOrHasChild(sender.getElement(), to)) {
          fireMouseLeave(sender);
        }
        break;
    }
  }

  /**
   * Fires a mouse leave event to all listeners.
   * 
   * @param sender the widget sending the event
   */
  public void fireMouseLeave(Widget sender) {
    for (Iterator it = iterator(); it.hasNext();) {
      MouseListener listener = (MouseListener) it.next();
      listener.onMouseLeave(sender);
    }
  }

  /**
   * Fires a mouse move event to all listeners.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   */
  public void fireMouseMove(Widget sender, int x, int y) {
    for (Iterator it = iterator(); it.hasNext();) {
      MouseListener listener = (MouseListener) it.next();
      listener.onMouseMove(sender, x, y);
    }
  }

  /**
   * Fires a mouse up event to all listeners.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   */
  public void fireMouseUp(Widget sender, int x, int y) {
    for (Iterator it = iterator(); it.hasNext();) {
      MouseListener listener = (MouseListener) it.next();
      listener.onMouseUp(sender, x, y);
    }
  }
}
