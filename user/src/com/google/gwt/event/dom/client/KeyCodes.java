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

/**
 * Contains the native key codes previously defined in
 * {@link com.google.gwt.user.client.ui.KeyboardListener}. When converting
 * keyboard listener instances, developers can use the following static import
 * to access these constants:
 * 
 * <pre> import static com.google.gwt.event.dom.client.KeyCodes.*; </pre>
 * 
 * These constants are defined with an int data type in order to be compatible
 * with the constants defined in
 * {@link com.google.gwt.user.client.ui.KeyboardListener}.
 */
public class KeyCodes {
  /**
   * Alt key code.
   */
  public static final int KEY_ALT = 18;

  /**
   * Backspace key code.
   */
  public static final int KEY_BACKSPACE = 8;
  /**
   * Control key code.
   */
  public static final int KEY_CTRL = 17;

  /**
   * Delete key code.
   */
  public static final int KEY_DELETE = 46;

  /**
   * Down arrow code.
   */
  public static final int KEY_DOWN = 40;

  /**
   * End key code.
   */
  public static final int KEY_END = 35;

  /**
   * Enter key code.
   */
  public static final int KEY_ENTER = 13;
  /**
   * Escape key code.
   */
  public static final int KEY_ESCAPE = 27;
  /**
   * Home key code.
   */
  public static final int KEY_HOME = 36;
  /**
   * Left key code.
   */
  public static final int KEY_LEFT = 37;
  /**
   * Page down key code.
   */
  public static final int KEY_PAGEDOWN = 34;
  /**
   * Page up key code.
   */
  public static final int KEY_PAGEUP = 33;
  /**
   * Right arrow key code.
   */
  public static final int KEY_RIGHT = 39;
  /**
   * Shift key code.
   */
  public static final int KEY_SHIFT = 16;

  /**
   * Tab key code.
   */
  public static final int KEY_TAB = 9;
  /**
   * Up Arrow key code.
   */
  public static final int KEY_UP = 38;

  // This class should never be instantiated
  private KeyCodes() {
  }

}
