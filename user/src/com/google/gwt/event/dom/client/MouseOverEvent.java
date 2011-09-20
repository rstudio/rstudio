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
import com.google.gwt.dom.client.EventTarget;

/**
 * Represents a native mouse over event.
 */
public class MouseOverEvent extends MouseEvent<MouseOverHandler> {

  /**
   * Event type for mouse over events. Represents the meta-data associated with
   * this event.
   */
  private static final Type<MouseOverHandler> TYPE = new Type<MouseOverHandler>(
      BrowserEvents.MOUSEOVER, new MouseOverEvent());

  /**
   * Gets the event type associated with mouse over events.
   * 
   * @return the handler type
   */
  public static Type<MouseOverHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use
   * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
   * to fire mouse over events.
   */
  protected MouseOverEvent() {
  }

  @Override
  public final Type<MouseOverHandler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Gets the target from which the mouse pointer was moved.
   * 
   * @return the target from which the mouse pointer was moved
   */
  public EventTarget getRelatedTarget() {
    return getNativeEvent().getRelatedEventTarget();
  }

  @Override
  protected void dispatch(MouseOverHandler handler) {
    handler.onMouseOver(this);
  }
}
