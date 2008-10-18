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

import com.google.gwt.event.shared.HandlerAdaptor;
import com.google.gwt.event.shared.HasHandlerManager;

/**
 * This is a convenience interface that includes all mouse handlers defined by
 * the core GWT system.
 * 
 * <br/> WARNING, PLEASE READ: As this interface is intended for developers who
 * wish to handle all mouse events in GWT, new mouse event handlers will be
 * added to it. Therefore, updates can cause breaking API changes.
 * 
 */
public interface HasAllMouseHandlers extends HasMouseDownHandlers,
    HasMouseUpHandlers, HasMouseOutHandlers, HasMouseOverHandlers,
    HasMouseMoveHandlers, HasMouseWheelHandlers {

  /**
   * Adaptor used to create and add all the Keyboard events at once.
   * 
   */
  public abstract static class Adaptor extends HandlerAdaptor implements
      HasMouseDownHandlers, HasMouseUpHandlers, HasMouseOutHandlers,
      HasMouseOverHandlers, HasMouseMoveHandlers, HasMouseWheelHandlers {

    /**
     * Convenience method to add all key handlers at once.
     * 
     * @param <EventSourceType> event source type
     * @param <EventHandler> event handler type
     * @param source event source
     * @param handlers handlers to add
     */
    public static <EventSourceType extends HasHandlerManager & HasAllMouseHandlers, EventHandler extends MouseDownHandler & MouseUpHandler & MouseOutHandler & MouseOverHandler & MouseMoveHandler & MouseWheelHandler> void addHandlers(
        EventSourceType source, EventHandler handlers) {
      source.addMouseDownHandler(handlers);
      source.addMouseUpHandler(handlers);
      source.addMouseOutHandler(handlers);
      source.addMouseOverHandler(handlers);
      source.addMouseMoveHandler(handlers);
      source.addMouseWheelHandler(handlers);
    }

    /**
     * Creates an adaptor to implement all the {@link HasAllKeyHandlers} handler
     * types.
     */
    public Adaptor() {
    }
  }

}
