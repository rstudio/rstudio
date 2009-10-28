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

import com.google.gwt.event.shared.EventHandler;

/**
 * Key up and key down are both events based upon a given key code.
 * 
 * @param <H> handler type
 */
public abstract class KeyCodeEvent<H extends EventHandler> extends KeyEvent<H> {
  /**
   * Does the key code represent an arrow key?
   * 
   * @param keyCode the key code
   * @return if it is an arrow key code
   */
  public static boolean isArrow(int keyCode) {
    switch (keyCode) {
      case KeyCodes.KEY_DOWN:
      case KeyCodes.KEY_RIGHT:
      case KeyCodes.KEY_UP:
      case KeyCodes.KEY_LEFT:
        return true;
      default:
        return false;
    }
  }

  /**
   * Gets the native key code. These key codes are enumerated in the
   * {@link KeyCodes} class.
   * 
   * @return the key code
   */
  public int getNativeKeyCode() {
    return getNativeEvent().getKeyCode();
  }

  /**
   * Is this a key down arrow?
   * 
   * @return whether this is a down arrow key event
   */
  public boolean isDownArrow() {
    return getNativeKeyCode() == KeyCodes.KEY_DOWN;
  }

  /**
   * Is this a left arrow?
   * 
   * @return whether this is a left arrow key event
   */
  public boolean isLeftArrow() {
    return getNativeKeyCode() == KeyCodes.KEY_LEFT;
  }

  /**
   * Is this a right arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isRightArrow() {
    return getNativeKeyCode() == KeyCodes.KEY_RIGHT;
  }

  /**
   * Is this a up arrow?
   * 
   * @return whether this is a right arrow key event
   */
  public boolean isUpArrow() {
    return getNativeKeyCode() == KeyCodes.KEY_UP;
  }

  @Override
  public String toDebugString() {
    return super.toDebugString() + "[" + getNativeKeyCode() + "]";
  }
}
