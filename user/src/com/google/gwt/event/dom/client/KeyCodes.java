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
   * Key code for A
   */
  public static final int KEY_A = 65;
  /**
   * Key code for B
   */
  public static final int KEY_B = 66;
  /**
   * Key code for C
   */
  public static final int KEY_C = 67;
  /**
   * Key code for D
   */
  public static final int KEY_D = 68;
  /**
   * Key code for E
   */
  public static final int KEY_E = 69;
  /**
   * Key code for F
   */
  public static final int KEY_F = 70;
  /**
   * Key code for G
   */
  public static final int KEY_G = 71;
  /**
   * Key code for H
   */
  public static final int KEY_H = 72;
  /**
   * Key code for I
   */
  public static final int KEY_I = 73;
  /**
   * Key code for J
   */
  public static final int KEY_J = 74;
  /**
   * Key code for K
   */
  public static final int KEY_K = 75;
  /**
   * Key code for L
   */
  public static final int KEY_L = 76;
  /**
   * Key code for M
   */
  public static final int KEY_M = 77;
  /**
   * Key code for N
   */
  public static final int KEY_N = 78;
  /**
   * Key code for O
   */
  public static final int KEY_O = 79;
  /**
   * Key code for P
   */
  public static final int KEY_P = 80;
  /**
   * Key code for Q
   */
  public static final int KEY_Q = 81;
  /**
   * Key code for R
   */
  public static final int KEY_R = 82;
  /**
   * Key code for S
   */
  public static final int KEY_S = 83;
  /**
   * Key code for T
   */
  public static final int KEY_T = 84;
  /**
   * Key code for U
   */
  public static final int KEY_U = 85;
  /**
   * Key code for V
   */
  public static final int KEY_V = 86;
  /**
   * Key code for W
   */
  public static final int KEY_W = 87;
  /**
   * Key code for X
   */
  public static final int KEY_X = 88;
  /**
   * Key code for Y
   */
  public static final int KEY_Y = 89;
  /**
   * Key code for Z
   */
  public static final int KEY_Z = 90;

  /**
   * Key code number 0
   */
  public static final int KEY_ZERO = 48;
  /**
   * Key code number 1
   */
  public static final int KEY_ONE = 49;
  /**
   * Key code number 2
   */
  public static final int KEY_TWO = 50;
  /**
   * Key code number 3
   */
  public static final int KEY_THREE = 51;
  /**
   * Key code number 4
   */
  public static final int KEY_FOUR = 52;
  /**
   * Key code number 5
   */
  public static final int KEY_FIVE = 53;
  /**
   * Key code number 6
   */
  public static final int KEY_SIX = 54;
  /**
   * Key code number 7
   */
  public static final int KEY_SEVEN = 55;
  /**
   * Key code number 8
   */
  public static final int KEY_EIGHT = 56;
  /**
   * Key code number 9
   */
  public static final int KEY_NINE = 57;

  /**
   * Key code for number 0 on numeric keyboard
   */
  public static final int KEY_NUM_ZERO = 96;
  /**
   * Key code for number 1 on numeric keyboard
   */
  public static final int KEY_NUM_ONE = 97;
  /**
   * Key code for number 2 on numeric keyboard
   */
  public static final int KEY_NUM_TWO = 98;
  /**
   * Key code for number 3 on numeric keyboard
   */
  public static final int KEY_NUM_THREE = 99;
  /**
   * Key code for number 4 on numeric keyboard
   */
  public static final int KEY_NUM_FOUR = 100;
  /**
   * Key code for number 5 on numeric keyboard
   */
  public static final int KEY_NUM_FIVE = 101;
  /**
   * Key code for number 6 on numeric keyboard
   */
  public static final int KEY_NUM_SIX = 102;
  /**
   * Key code for number 7 on numeric keyboard
   */
  public static final int KEY_NUM_SEVEN = 103;
  /**
   * Key code for number 8 on numeric keyboard
   */
  public static final int KEY_NUM_EIGHT = 104;
  /**
   * Key code for number 9 on numeric keyboard
   */
  public static final int KEY_NUM_NINE = 105;
  /**
   * Key code for multiply on numeric keyboard
   */
  public static final int KEY_NUM_MULTIPLY = 106;
  /**
   * Key code for plus on numeric keyboard
   */
  public static final int KEY_NUM_PLUS = 107;
  /**
   * Key code for minus on numeric keyboard
   */
  public static final int KEY_NUM_MINUS = 109;
  /**
   * Key code for period on numeric keyboard
   */
  public static final int KEY_NUM_PERIOD = 110;
  /**
   * Key code for division on numeric keyboard
   */
  public static final int KEY_NUM_DIVISION = 111;
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
   * Delete key code (also numeric keypad delete).
   */
  public static final int KEY_DELETE = 46;

  /**
   * Down arrow code (Also numeric keypad down).
   */
  public static final int KEY_DOWN = 40;

  /**
   * End key code (Also numeric keypad south west).
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
   * Home key code (Also numeric keypad north west).
   */
  public static final int KEY_HOME = 36;
  /**
   * Left key code (Also numeric keypad west).
   */
  public static final int KEY_LEFT = 37;
  /**
   * Page down key code (Also numeric keypad south east).
   */
  public static final int KEY_PAGEDOWN = 34;
  /**
   * Page up key code (Also numeric keypad north east).
   */
  public static final int KEY_PAGEUP = 33;
  /**
   * Right arrow key code (Also numeric keypad east).
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
   * Up Arrow key code (Also numeric keypad north).
   */
  public static final int KEY_UP = 38;

  /**
   * Key code for F1
   */
  public static final int KEY_F1 = 112;
  /**
   * Key code for F2
   */
  public static final int KEY_F2 = 113;
  /**
   * Key code for F3
   */
  public static final int KEY_F3 = 114;
  /**
   * Key code for F4
   */
  public static final int KEY_F4 = 115;
  /**
   * Key code for F5
   */
  public static final int KEY_F5 = 116;
  /**
   * Key code for F6
   */
  public static final int KEY_F6 = 117;
  /**
   * Key code for F7
   */
  public static final int KEY_F7 = 118;
  /**
   * Key code for F8
   */
  public static final int KEY_F8 = 119;
  /**
   * Key code for F9
   */
  public static final int KEY_F9 = 120;
  /**
   * Key code for F10
   */
  public static final int KEY_F10 = 121;
  /**
   * Key code for F11
   */
  public static final int KEY_F11 = 122;
  /**
   * Key code for F12
   */
  public static final int KEY_F12 = 123;
  /**
   * Key code for Windows key on Firefox Linux
   */
  public static final int KEY_WIN_KEY_FF_LINUX =  0;
  /**
   * Key code for Mac enter key
   */
  public static final int KEY_MAC_ENTER = 3;
  /**
   * Key code for pause key
   */
  public static final int KEY_PAUSE = 19;
  /**
   * Key code for caps lock key
   */
  public static final int KEY_CAPS_LOCK = 20;
  /**
   * Key code for space
   */
  public static final int KEY_SPACE = 32;

  /**
   * Key code for print key
   */
  public static final int KEY_PRINT_SCREEN = 44;
  /**
   * Key code for insert key (Also numeric keyboard insert).
   */
  public static final int KEY_INSERT = 45;      // also NUM_INSERT

  /**
   * Key code for insert key (Also num lock on FF,Safari Mac).
   */
  public static final int KEY_NUM_CENTER = 12;

  /**
   * Key code for left windows key.
   */
  public static final int KEY_WIN_KEY = 224;

  /**
   * Key code for left windows key or meta.
   */
  public static final int KEY_WIN_KEY_LEFT_META = 91;

  /**
   * Key code for right windows key.
   */
  public static final int KEY_WIN_KEY_RIGHT = 92;
  /**
   * Key code for context menu key.
   */
  public static final int KEY_CONTEXT_MENU = 93;
  /**
   * Key code for {@link KeyCodes#KEY_WIN_KEY_LEFT_META} that Firefox fires
   * for the meta key.
   */
  public static final int KEY_MAC_FF_META = 224; // Firefox (Gecko) fires this for the meta key instead of 91

  /**
   * Key code for num lock.
   */
  public static final int KEY_NUMLOCK = 144;
  /**
   * Key code for scroll lock.
   */
  public static final int KEY_SCROLL_LOCK = 145;

  /**
   * Key code for first OS specific media key (like volume).
   */
  public static final int KEY_FIRST_MEDIA_KEY = 166;
  /**
   * Key code for last OS specific media key (like volume).
   */
  public static final int KEY_LAST_MEDIA_KEY = 183;
  /**
   * Key code for IME.
   */
  public static final int KEY_WIN_IME = 229;

  /**
   * Key code for open square bracket, [.
   */
  public static final int KEY_OPEN_BRACKET = 219;

  /**
   * Key code for close square bracket, ].
   */
  public static final int KEY_CLOSE_BRACKET = 221;

  /**
   * Determines if a key code is an arrow key.
   */
  public static boolean isArrowKey(int code) {
    switch (code) {
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
   * Update arrow keys for left and right based on current locale.
   *
   * <p>Note: this method is used internally by built-in GWT widgets but
   *   could be renamed/refactored without notice.
   * <p>This method simplifies RTL handling in your code:
   * <code><pre>
   * int keyCode = KeyCodes.maybeSwapArrowKeysForRtl(event.getKeyCode(),
   *   LocaleInfo.getCurrentLocale().isRTL());
   * switch (keyCode) {
   *   case KeyCodes.KEY_LEFT:
   *     ... // start of the line, no special RTL handling
   *     break;
   *   case KeyCodes.KEY_RIGHT:
   *     ... // end of the line, no special RTL handling
   *     break;
   *   ...
   *   }
   * </pre></code>
   */
  public static int maybeSwapArrowKeysForRtl(int code, boolean isRtl) {
    if (isRtl) {
      if (code == KEY_RIGHT) {
        code = KEY_LEFT;
      } else if (code == KEY_LEFT) {
        code = KEY_RIGHT;
      }
    }
    return code;
  }

  // This class should never be instantiated
  private KeyCodes() {
  }
}
