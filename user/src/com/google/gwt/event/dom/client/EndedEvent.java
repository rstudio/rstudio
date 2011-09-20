/*
 * Copyright 2011 Google Inc.
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

/**
 * Represents a native media ended event.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change.
 * </span>
 * </p>
 */
public class EndedEvent extends DomEvent<EndedHandler> {

  /**
   * Event type for media ended events. Represents the meta-data associated with
   * this event.
   */
  private static final Type<EndedHandler> TYPE = new Type<
      EndedHandler>(BrowserEvents.ENDED, new EndedEvent());

  /**
   * Gets the event type associated with media ended events.
   *
   * @return the handler type
   */
  public static Type<EndedHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use {@link
   * DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent,
   * com.google.gwt.event.shared.HasHandlers)} to fire media ended events.
   */
  protected EndedEvent() {
  }

  @Override
  public final Type<EndedHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(EndedHandler handler) {
    handler.onEnded(this);
  }
}