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
 * Convenience interface used to implement all key handlers at once. In the
 * unlikely event that more key handler subtypes are added to GWT, this
 * interface will be expanded, so only implement this interface if you wish to
 * have your widget break if a new key event type is introduced.
 * 
 */
public interface HasAllKeyHandlers extends HasKeyUpHandlers,
    HasKeyDownHandlers, HasKeyPressHandlers {

  /**
   * Adaptor used to create and add all the Keyboard events at once.
   */
  public abstract static class Adaptor extends HandlerAdaptor implements
      KeyDownHandler, KeyUpHandler, KeyPressHandler {

    /**
     * Convenience method to add all key handlers at once.
     * 
     * @param <EventSourceType> event source type
     * @param <EventHandler> event handler type
     * @param source event source
     * @param handlers handlers to add
     */
    public static <EventSourceType extends HasHandlerManager & HasAllKeyHandlers, EventHandler extends KeyDownHandler & KeyUpHandler & KeyPressHandler> void addHandlers(
        EventSourceType source, EventHandler handlers) {
      source.addKeyDownHandler(handlers);
      source.addKeyPressHandler(handlers);
      source.addKeyUpHandler(handlers);
    }

    /**
     * Constructor.
     */
    public Adaptor() {
    }
  }
}
