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
package com.google.gwt.user.client.ui;

import java.util.EventListener;

/**
 * Event listener interface for keyboard events.
 * 
 * @deprecated use {@link com.google.gwt.event.dom.client.KeyDownHandler},
 *             {@link com.google.gwt.event.dom.client.KeyUpHandler} and/or
 *             {@link com.google.gwt.event.dom.client.KeyPressHandler} instead
 */
@Deprecated
public interface KeyboardListener extends EventListener {
  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_ALT}
   *             instead
   */
  @Deprecated
  int KEY_ALT = 18;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodes#KEY_BACKSPACE}
   *             instead
   */
  @Deprecated
  int KEY_BACKSPACE = 8;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_CTRL}
   *             instead
   */
  @Deprecated
  int KEY_CTRL = 17;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_DELETE}
   *             instead
   */
  @Deprecated
  int KEY_DELETE = 46;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_DOWN}
   *             instead
   */
  @Deprecated
  int KEY_DOWN = 40;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_END}
   *             instead
   */
  @Deprecated
  int KEY_END = 35;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_ENTER}
   *             instead
   */
  @Deprecated
  int KEY_ENTER = 13;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_ESCAPE}
   *             instead
   */
  @Deprecated
  int KEY_ESCAPE = 27;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_HOME}
   *             instead
   */
  @Deprecated
  int KEY_HOME = 36;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_LEFT}
   *             instead
   */
  @Deprecated
  int KEY_LEFT = 37;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodes#KEY_PAGEDOWN}
   *             instead
   */
  @Deprecated
  int KEY_PAGEDOWN = 34;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_PAGEUP}
   *             instead
   */
  @Deprecated
  int KEY_PAGEUP = 33;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_RIGHT}
   *             instead
   */
  @Deprecated
  int KEY_RIGHT = 39;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_SHIFT}
   *             instead
   */
  @Deprecated
  int KEY_SHIFT = 16;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_TAB}
   *             instead
   */
  @Deprecated
  int KEY_TAB = 9;

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.KeyCodes#KEY_UP}
   *             instead
   */
  @Deprecated
  int KEY_UP = 38;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodeEvent#isAltKeyDown()}
   *             instead
   */
  @Deprecated
  int MODIFIER_ALT = 4;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodeEvent#isControlKeyDown()}
   *             instead
   */
  @Deprecated
  int MODIFIER_CTRL = 2;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodeEvent#isMetaKeyDown()}
   *             instead
   */
  @Deprecated
  int MODIFIER_META = 8;

  /**
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyCodeEvent#isShiftKeyDown()}
   *             instead
   */
  @Deprecated
  int MODIFIER_SHIFT = 1;

  /**
   * Fired when the user depresses a physical key.
   * 
   * @param sender the widget that was focused when the event occurred.
   * @param keyCode the physical key that was depressed. Constants for this
   *          value are defined in this interface with the KEY prefix.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}
   * 
   * 
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyDownHandler#onKeyDown(com.google.gwt.event.dom.client.KeyDownEvent)}
   *             instead
   */
  @Deprecated
  void onKeyDown(Widget sender, char keyCode, int modifiers);

  /**
   * Fired when a keyboard action generates a character. This occurs after
   * onKeyDown and onKeyUp are fired for the physical key that was pressed.
   * 
   * <p>
   * It should be noted that many browsers do not generate keypress events for
   * non-printing keyCode values, such as {@link KeyboardListener#KEY_ENTER} or
   * arrow keys. These keyCodes can be reliably captured either with
   * {@link KeyboardListener#onKeyDown(Widget, char, int)} or
   * {@link KeyboardListener#onKeyUp(Widget, char, int)}.
   * </p>
   * 
   * @param sender the widget that was focused when the event occurred.
   * @param keyCode the Unicode character that was generated by the keyboard
   *          action.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyPressHandler#onKeyPress(com.google.gwt.event.dom.client.KeyPressEvent)}
   *             instead
   */
  @Deprecated
  void onKeyPress(Widget sender, char keyCode, int modifiers);

  /**
   * Fired when the user releases a physical key.
   * 
   * @param sender the widget that was focused when the event occurred.
   * @param keyCode the physical key that was released. Constants for this value
   *          are defined in this interface with the KEY prefix.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.KeyUpHandler#onKeyUp(com.google.gwt.event.dom.client.KeyUpEvent)}
   *             instead
   */
  @Deprecated
  void onKeyUp(Widget sender, char keyCode, int modifiers);
}
