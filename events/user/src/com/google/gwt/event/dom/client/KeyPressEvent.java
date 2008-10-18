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
      Event.ONKEYPRESS) {
    @Override
    public void fire(KeyPressHandler handler, KeyPressEvent event) {
      handler.onKeyPress(event);
    }

    @Override
    KeyPressEvent wrap(Event nativeEvent) {
      return new KeyPressEvent(nativeEvent);
    }
  };
  private static final int OTHER_KEY_DOWN = 63233;
  private static final int OTHER_KEY_LEFT = 63234;
  private static final int OTHER_KEY_RIGHT = 63235;
  private static final int OTHER_KEY_UP = 63232;

  /**
   * Constructor.
   * 
   * @param nativeEvent the native event object
   */
  public KeyPressEvent(Event nativeEvent) {
    super(nativeEvent);
  }

  /**
   * Gets the char code for this event.
   * 
   * @return the char code
   */
  public char getCharCode() {
    return getCharCode(getNativeEvent());
  }

  /**
   * Is this a key down arrow?
   * 
   * @return whether this is a down arrow key event
   */
  public boolean isDownArrow() {
    return getKeyCode() == KeyCodeEvent.KEY_DOWN
        || getKeyCode() == OTHER_KEY_DOWN;
  }

  /**
   * Is this a left arrow?
   * 
   * @return whether this is a left arrow key event
   */
  public boolean isLeftArrow() {
    return getKeyCode() == KeyCodeEvent.KEY_LEFT
        || getKeyCode() == OTHER_KEY_LEFT;
  }

  /**
   * Is this a right arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isRightArrow() {
    return getKeyCode() == KeyCodeEvent.KEY_RIGHT
        || getKeyCode() == OTHER_KEY_RIGHT;
  }

  /**
   * Is this a up arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isUpArrow() {
    return getKeyCode() == KeyCodeEvent.KEY_UP || getKeyCode() == OTHER_KEY_UP;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }

  private native char getCharCode(Event e)/*-{
    return e.charCode || e.keyCode;
  }-*/;

  private int getKeyCode() {
    return getNativeEvent().getKeyCode();
  }

}
