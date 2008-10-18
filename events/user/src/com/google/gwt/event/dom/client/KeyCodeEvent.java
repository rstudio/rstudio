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
 * Key up and key down are both events based upon a given key code.
 */
public abstract class KeyCodeEvent extends KeyEvent implements HasKeyCodes {
  /**
   * Does the key code represent an arrow key?
   * 
   * @param keyCode the key code
   * @return if it is an arrow key code
   */
  public static boolean isArrow(int keyCode) {
    switch (keyCode) {
      case KEY_DOWN:
      case KEY_RIGHT:
      case KEY_UP:
      case KEY_LEFT:
        return true;
      default:
        return false;
    }
  }

  /**
   * Constructor.
   * 
   * @param nativeEvent the wrapped native event
   */
  protected KeyCodeEvent(Event nativeEvent) {
    super(nativeEvent);
  }

  /**
   * Gets the current key code.
   * 
   * @return the key code
   */
  public int getKeyCode() {
    return getNativeEvent().getKeyCode();
  }

  /**
   * Is the key code alpha-numeric (i.e. A-z or 0-9)?
   * 
   * @return is the key code alpha numeric.
   */
  public boolean isAlphaNumeric() {
    int keycode = getKeyCode();
    return (48 <= keycode && keycode <= 57) || (65 <= keycode && keycode <= 90);
  }

  /**
   * Is this a key down arrow?
   * 
   * @return whether this is a down arrow key event
   */
  public boolean isDownArrow() {
    return getKeyCode() == KEY_DOWN;
  }

  /**
   * Is this a left arrow?
   * 
   * @return whether this is a left arrow key event
   */
  public boolean isLeftArrow() {
    return getKeyCode() == KEY_LEFT;
  }

  /**
   * Is this a right arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isRightArrow() {
    return getKeyCode() == KEY_RIGHT;
  }

  /**
   * Is this a up arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isUpArrow() {
    return getKeyCode() == KEY_UP;
  }

}
