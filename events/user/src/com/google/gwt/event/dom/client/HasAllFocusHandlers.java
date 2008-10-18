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
 * This is a convenience interface that includes all focus handlers defined by
 * the core GWT system.
 * 
 * <br/> WARNING, PLEASE READ: As this interface is intended for developers who
 * wish to handle all focus events in GWT, in the unlikely event that a new
 * focus event is added, this interface will change.
 * 
 */
public interface HasAllFocusHandlers extends HasFocusHandlers, HasBlurHandlers {
  /**
   * Adaptor used to implement both {@link FocusHandler} and {@link BlurHandler}
   * .
   */
  public abstract static class Adaptor extends HandlerAdaptor implements
      FocusHandler, BlurHandler {

    /**
     * Convenience method to add both focus handlers at once to an event source.
     * 
     * @param <EventSourceType> the event source to add the handlers to.
     * @param <EventHandlerType>
     * @param source the event source
     * @param handlers the focus handlers
     */
    public static <EventSourceType extends HasHandlerManager & HasAllFocusHandlers, EventHandlerType extends BlurHandler & FocusHandler> void addHandlers(
        EventSourceType source, EventHandlerType handlers) {
      source.addBlurHandler(handlers);
      source.addFocusHandler(handlers);
    }

    /**
     * Constructor.
     */
    public Adaptor() {
    }
  }
}
