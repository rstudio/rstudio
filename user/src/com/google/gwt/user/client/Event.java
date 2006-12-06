/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * An opaque handle to a native DOM Event. An <code>Event</code> cannot be
 * created directly. Instead, use the <code>Event</code> type when returning a
 * native DOM event from JSNI methods. An <code>Event</code> passed back into
 * JSNI becomes the original DOM event the <code>Event</code> was created
 * from, and can be accessed in JavaScript code as expected. This is typically
 * done by calling methods in the {@link com.google.gwt.user.client.DOM} class.
 */
public final class Event extends JavaScriptObject {

  /**
   * The left mouse button (used in {@link DOM#eventGetButton(Event)}).
   */
  public static final int BUTTON_LEFT = 1;

  /**
   * The middle mouse button (used in {@link DOM#eventGetButton(Event)}).
   */
  public static final int BUTTON_MIDDLE = 4;

  /**
   * The right mouse button (used in {@link DOM#eventGetButton(Event)}).
   */
  public static final int BUTTON_RIGHT = 2;

  /**
   * Fired when an element loses keyboard focus.
   */
  public static final int ONBLUR = 0x01000;

  /**
   * Fired when the value of an input element changes.
   */
  public static final int ONCHANGE = 0x00400;

  /**
   * Fired when the user clicks on an element.
   */
  public static final int ONCLICK = 0x00001;

  /**
   * Fired when the user double-clicks on an element.
   */
  public static final int ONDBLCLICK = 0x00002;

  /**
   * Fired when a scrollable element's scroll offset changes.
   */
  public static final int ONERROR = 0x10000;

  /**
   * Fired when an element receives keyboard focus.
   */
  public static final int ONFOCUS = 0x00800;

  /**
   * Fired when the user depresses a key.
   */
  public static final int ONKEYDOWN = 0x00080;

  /**
   * Fired when the a character is generated from a keypress (either directly or
   * through auto-repeat).
   */
  public static final int ONKEYPRESS = 0x00100;

  /**
   * Fired when the user releases a key.
   */
  public static final int ONKEYUP = 0x00200;

  /**
   * Fired when an element (normally an IMG) finishes loading.
   */
  public static final int ONLOAD = 0x08000;

  /**
   * Fired when an element that has mouse capture loses it.
   */
  public static final int ONLOSECAPTURE = 0x02000;

  /**
   * Fired when the user depresses a mouse button over an element.
   */
  public static final int ONMOUSEDOWN = 0x00004;

  /**
   * Fired when the mouse is moved within an element's area.
   */
  public static final int ONMOUSEMOVE = 0x00040;

  /**
   * Fired when the mouse is moved out of an element's area.
   */
  public static final int ONMOUSEOUT = 0x00020;

  /**
   * Fired when the mouse is moved into an element's area.
   */
  public static final int ONMOUSEOVER = 0x00010;

  /**
   * Fired when the user releases a mouse button over an element.
   */
  public static final int ONMOUSEUP = 0x00008;

  /**
   * Fired when a scrollable element's scroll offset changes.
   */
  public static final int ONSCROLL = 0x04000;

  /**
   * A bit-mask covering both focus events (focus and blur).
   */
  public static final int FOCUSEVENTS = ONFOCUS | ONBLUR;

  /**
   * A bit-mask covering all keyboard events (down, up, and press).
   */
  public static final int KEYEVENTS = ONKEYDOWN | ONKEYPRESS | ONKEYUP;

  /**
   * A bit-mask covering all mouse events (down, up, move, over, and out), but
   * not click or dblclick.
   */
  public static final int MOUSEEVENTS = ONMOUSEDOWN | ONMOUSEUP | ONMOUSEMOVE
    | ONMOUSEOVER | ONMOUSEOUT;

  /**
   * Creates a new <code>Element</code>. This constructor is used internally
   * and should never be called by a user.
   * 
   * @param opaque the underlying DOM element
   */
  Event(int opaque) {
    super(opaque);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object other) {
    return super.equals(other);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return super.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return DOM.eventToString(this);
  };
}
