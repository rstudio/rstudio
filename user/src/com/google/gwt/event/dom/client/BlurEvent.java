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

/**
 * Represents a native blur event.
 */
public class BlurEvent extends DomEvent<BlurHandler> {

  /**
   * Event type for blur events. Represents the meta-data associated with this
   * event.
   */
  private static final Type<BlurHandler> TYPE = new Type<BlurHandler>(BrowserEvents.BLUR,
      new BlurEvent());

  /**
   * Gets the event type associated with blur events.
   * 
   * @return the handler type
   */
  public static Type<BlurHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use
   * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
   * to fire blur events.
   */
  protected BlurEvent() {
  }

  @Override
  public final Type<BlurHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(BlurHandler handler) {
    handler.onBlur(this);
  }

}
