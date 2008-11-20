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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Represents a native mouse out event.
 */
public class MouseOutEvent extends MouseEvent<MouseOutHandler> {

  /**
   * Event type for mouse out events. Represents the meta-data associated with
   * this event.
   */
  private static final Type<MouseOutHandler> TYPE = new Type<MouseOutHandler>(
      Event.ONMOUSEOUT, "mouseout", new MouseOutEvent());

  /**
   * Gets the event type associated with mouse out events.
   * 
   * @return the handler type
   */
  public static Type<MouseOutHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use
   * {@link DomEvent#fireNativeEvent(Event, com.google.gwt.event.shared.HandlerManager)}
   * to fire mouse out events.
   */
  protected MouseOutEvent() {
  }

  /**
   * Gets the element from which the mouse pointer was moved.
   * 
   * @return the element from which the mouse pointer was moved
   */
  public Element getFromElement() {
    // Use a deferred binding instead of DOMImpl's inefficient switch statement
    return getNativeEvent().getFromElement();
  }

  /**
   * Gets the element to which the mouse pointer was moved.
   * 
   * @return the element to which the mouse pointer was moved
   */
  public Element getToElement() {
    // Use a deferred binding instead of DOMImpl's inefficient switch statement
    return getNativeEvent().getToElement();
  }

  @Override
  protected void dispatch(MouseOutHandler handler) {
    handler.onMouseOut(this);
  }

  @Override
  protected final Type<MouseOutHandler> getAssociatedType() {
    return TYPE;
  }

}
