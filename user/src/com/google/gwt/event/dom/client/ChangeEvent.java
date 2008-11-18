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

import com.google.gwt.user.client.Event;

/**
 * Represents a native change event.
 */
public class ChangeEvent extends DomEvent<ChangeHandler> {

  /**
   * Event type for change events. Represents the meta-data associated with this
   * event.
   */
  private static final Type<ChangeHandler> TYPE = new Type<ChangeHandler>(
      Event.ONCHANGE, "change", new ChangeEvent());

  /**
   * Gets the event type associated with change events.
   * 
   * @return the handler type
   */
  public static Type<ChangeHandler> getType() {
    return TYPE;
  }

  /**
   * Protected constructor, use
   * {@link DomEvent#fireNativeEvent(Event, com.google.gwt.event.shared.HandlerManager)}
   * to fire change events.
   */
  protected ChangeEvent() {
  }

  @Override
  protected void dispatch(ChangeHandler handler) {
    handler.onChange(this);
  }

  @Override
  protected final Type<ChangeHandler> getAssociatedType() {
    return TYPE;
  }

}
