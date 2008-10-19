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
 * Represents a native key press event.
 */
public class KeyPressEvent extends KeyEvent {

  /**
   * Event type for key press events. Represents the meta-data associated with
   * this event.
   */
  public static final Type<KeyPressEvent, KeyPressHandler> TYPE = new Type<KeyPressEvent, KeyPressHandler>(
      Event.ONKEYPRESS, "keypress", new KeyPressEvent()) {
    @Override
    public void fire(KeyPressHandler handler, KeyPressEvent event) {
      handler.onKeyPress(event);
    }
  };

  /**
   * Gets the char code for this event.
   * 
   * @return the char code
   */
  public char getCharCode() {
    return getCharCode(getNativeEvent());
  }

  @Override
  protected Type getType() {
    return TYPE;
  }

  private native char getCharCode(Event e)/*-{
    return e.charCode || e.keyCode;
  }-*/;

}
