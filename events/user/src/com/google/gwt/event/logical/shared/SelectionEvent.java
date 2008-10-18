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

package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.AbstractEvent;

/**
 * Fired after an event source has selected a new value.
 * 
 * @param <Value> the type of value the widget has selected
 */
public class SelectionEvent<Value> extends AbstractEvent {

  /**
   * The event type.
   */
  public static final Type<SelectionEvent, SelectionHandler> TYPE = new Type<SelectionEvent, SelectionHandler>() {

    @Override
    protected void fire(SelectionHandler handler, SelectionEvent event) {
      handler.onSelection(event);
    }
  };

  private Value oldValue;
  private Value newValue;

  /**
   * Constructor.
   * 
   * @param oldValue the old value
   * @param newValue the new value
   */

  public SelectionEvent(Value oldValue, Value newValue) {
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Returns the new value.
   * 
   * @return the new value
   */
  public Value getNewValue() {
    assertLive();
    return newValue;
  }

  /**
   * Returns the old value.
   * 
   * @return the old value
   */
  public Value getOldValue() {
    assertLive();
    return oldValue;
  }

  @Override
  public String toDebugString() {
    assertLive();
    return super.toDebugString() + " old = " + oldValue + " new =" + newValue;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
