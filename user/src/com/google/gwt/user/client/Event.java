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
package com.google.gwt.user.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * <p>
 * An opaque handle to a native DOM Event. An <code>Event</code> cannot be
 * created directly. Instead, use the <code>Event</code> type when returning a
 * native DOM event from JSNI methods. An <code>Event</code> passed back into
 * JSNI becomes the original DOM event the <code>Event</code> was created
 * from, and can be accessed in JavaScript code as expected. This is typically
 * done by calling methods in the {@link com.google.gwt.user.client.DOM} class.
 * </p>
 * 
 * <h3>Event Attributes</h3>
 * <p>
 * In hosted mode, most accessors (eg. Event{@link #getKeyCode()} and
 * {@link Event#getButton()}) assert that the requested attribute is reliable
 * across all supported browsers.  This means that attempting to retrieve an
 * attribute for an {@link Event} that does not support that attribute will
 * throw an {@link AssertionError}.  For example, an {@link Event} of type
 * {@link Event#ONCLICK} will throw an {@link AssertionError} if you attempt
 * to get the mouse button that was clicked using {@link Event#getButton()}
 * because the mouse button attribute is not defined for {@link Event#ONCLICK}
 * on Internet Explorer. 
 * </p>
 */
public class Event extends JavaScriptObject {

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
   * Fired when an image encounters an error.
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
   * Fired when the user scrolls the mouse wheel over an element.
   */
  public static final int ONMOUSEWHEEL = 0x20000;

  /**
   * Fired when a scrollable element's scroll offset changes.
   */
  public static final int ONSCROLL = 0x04000;

  /**
   * Fired when the user requests an element's context menu (usually by right-clicking).
   * 
   * Note that not all browsers will fire this event (notably Opera, as of 9.5).
   */
  public static final int ONCONTEXTMENU = 0x40000;

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
   * not click, dblclick, or wheel events.
   */
  public static final int MOUSEEVENTS = ONMOUSEDOWN | ONMOUSEUP | ONMOUSEMOVE
      | ONMOUSEOVER | ONMOUSEOUT;

  /**
   * Value returned by accessors when the actual integer value is undefined.  In
   * hosted mode, most accessors assert that the requested attribute is reliable
   * across all supported browsers.
   * 
   * @see Event
   */
  @Deprecated
  public static final int UNDEFINED = 0;

  /**
   * Adds an event preview to the preview stack. As long as this preview remains
   * on the top of the stack, it will receive all events before they are fired
   * to their listeners. Note that the event preview will receive <u>all </u>
   * events, including those received due to bubbling, whereas normal event
   * handlers only receive explicitly sunk events.
   * 
   * @param preview the event preview to be added to the stack.
   */
  public static void addEventPreview(EventPreview preview) {
    DOM.addEventPreview(preview);
  }

  /**
   * Gets the current event that is being fired. The current event is only
   * available within the lifetime of the onBrowserEvent function. Once the
   * onBrowserEvent method returns, the current event is reset to null.
   * 
   * @return the current event
   */
  public static Event getCurrentEvent() {
    return DOM.eventGetCurrentEvent();
  }

  /**
   * Gets the current set of events sunk by a given element.
   * 
   * @param elem the element whose events are to be retrieved
   * @return a bitfield describing the events sunk on this element (its possible
   *         values are described in {@link Event})
   */
  public static int getEventsSunk(Element elem) {
    return DOM.getEventsSunk((com.google.gwt.user.client.Element) elem);
  }

  /**
   * Releases mouse capture on the given element. Calling this method has no
   * effect if the element does not currently have mouse capture.
   * 
   * @param elem the element to release capture
   * @see #setCapture(Element)
   */
  public static void releaseCapture(Element elem) {
    DOM.releaseCapture(elem.<com.google.gwt.user.client.Element>cast());
  }

  /**
   * Removes an element from the preview stack. This element will no longer
   * capture events, though any preview underneath it will begin to do so.
   * 
   * @param preview the event preview to be removed from the stack
   */
  public static void removeEventPreview(EventPreview preview) {
    DOM.removeEventPreview(preview);
  }

  /**
   * Sets mouse-capture on the given element. This element will directly receive
   * all mouse events until {@link #releaseCapture(Element)} is called on it.
   * 
   * @param elem the element on which to set mouse capture
   */
  public static void setCapture(Element elem) {
    DOM.setCapture(elem.<com.google.gwt.user.client.Element>cast());
  }

  /**
   * Sets the current set of events sunk by a given element. These events will
   * be fired to the nearest {@link EventListener} specified on any of the
   * element's parents.
   * 
   * @param elem the element whose events are to be retrieved
   * @param eventBits a bitfield describing the events sunk on this element (its
   *          possible values are described in {@link Event})
   */
  public static void sinkEvents(Element elem, int eventBits) {
    DOM.sinkEvents((com.google.gwt.user.client.Element) elem, eventBits);
  }

  /**
   * Not directly instantiable. Subclasses should also define a protected no-arg
   * constructor to prevent client code from directly instantiating the class.
   */
  protected Event() {
  }

  /**
   * Cancels bubbling for the given event. This will stop the event from being
   * propagated to parent elements.
   * 
   * @param cancel <code>true</code> to cancel bubbling
   */
  public final void cancelBubble(boolean cancel) {
    DOM.eventCancelBubble(this, cancel);
  }

  /**
   * Gets whether the ALT key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if ALT was depressed when the event occurred
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#KEYEVENTS}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final boolean getAltKey() throws AssertionError {
    return DOM.eventGetAltKey(this);
  }

  /**
   * Gets the mouse buttons that were depressed when the given event occurred.
   * 
   * @return a bit-field, defined by {@link Event#BUTTON_LEFT},
   *         {@link Event#BUTTON_MIDDLE}, and {@link Event#BUTTON_RIGHT}
   * @throws AssertionError if event type is not one of
   *           {@link Event#ONMOUSEDOWN} or {@link Event#ONMOUSEUP}
   */
  public final int getButton() throws AssertionError {
    return DOM.eventGetButton(this);
  }

  /**
   * Gets the mouse x-position within the browser window's client area.
   * 
   * @return the mouse x-position
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#ONMOUSEWHEEL}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final int getClientX() throws AssertionError {
    return DOM.eventGetClientX(this);
  }

  /**
   * Gets the mouse y-position within the browser window's client area.
   * 
   * @return the mouse y-position
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#ONMOUSEWHEEL}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final int getClientY() throws AssertionError {
    return DOM.eventGetClientY(this);
  }

  /**
   * Gets whether the CTRL key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if CTRL was depressed when the event occurred
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#KEYEVENTS}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final boolean getCtrlKey() throws AssertionError {
    return DOM.eventGetCtrlKey(this);
  }

  /**
   * Gets the current target element of this event. This is the element whose
   * listener fired last, not the element which fired the event initially.
   * 
   * @return the event's current target element
   */
  public final Element getCurrentTarget() {
    return DOM.eventGetCurrentTarget(this);
  }

  /**
   * Gets the element from which the mouse pointer was moved (only valid for
   * {@link Event#ONMOUSEOVER}).
   * 
   * @return the element from which the mouse pointer was moved
   * @throws AssertionError if event type is not one of
   *           {@link Event#ONMOUSEOVER} or {@link Event#ONMOUSEOUT}
   */
  public final Element getFromElement() throws AssertionError {
    return DOM.eventGetFromElement(this);
  }

  /**
   * Gets the key code associated with this event.
   * 
   * <p>
   * For {@link Event#ONKEYPRESS}, this method returns the Unicode value of the
   * character generated. For {@link Event#ONKEYDOWN} and {@link Event#ONKEYUP},
   * it returns the code associated with the physical key.
   * </p>
   * 
   * @return the Unicode character or key code.
   * @throws AssertionError if event type is not one of {@link Event#KEYEVENTS}
   * @see com.google.gwt.user.client.ui.KeyboardListener
   */
  public final int getKeyCode() throws AssertionError {
    return DOM.eventGetKeyCode(this);
  }

  /**
   * Gets whether the META key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if META was depressed when the event occurred
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#KEYEVENTS}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final boolean getMetaKey() throws AssertionError {
    return DOM.eventGetMetaKey(this);
  }

  /**
   * Gets the velocity of the mouse wheel associated with the event along the Y
   * axis.
   * <p>
   * The velocity of the event is an artifical measurement for relative
   * comparisons of wheel activity. It is affected by some non-browser factors,
   * including choice of input hardware and mouse acceleration settings. The
   * sign of the velocity measurement agrees with the screen coordinate system;
   * negative values are towards the origin and positive values are away from
   * the origin. Standard scrolling speed is approximately ten units per event.
   * </p>
   * 
   * @return The velocity of the mouse wheel.
   * @throws AssertionError if event type is not {@link Event#ONMOUSEWHEEL}
   */
  public final int getMouseWheelVelocityY() throws AssertionError {
    return DOM.eventGetMouseWheelVelocityY(this);
  }

  /**
   * Gets the key-repeat state of this event.
   * 
   * @return <code>true</code> if this key event was an auto-repeat
   * @throws AssertionError if event type is not {@link Event#ONKEYDOWN}
   */
  public final boolean getRepeat() throws AssertionError {
    return DOM.eventGetRepeat(this);
  }

  /**
   * Gets the mouse x-position on the user's display.
   * 
   * @return the mouse x-position
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONMOUSEWHEEL},
   *           {@link Event#ONCLICK}, {@link Event#ONDBLCLICK}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final int getScreenX() throws AssertionError {
    return DOM.eventGetScreenX(this);
  }

  /**
   * Gets the mouse y-position on the user's display.
   * 
   * @return the mouse y-position
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONMOUSEWHEEL},
   *           {@link Event#ONCLICK}, {@link Event#ONDBLCLICK}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final int getScreenY() throws AssertionError {
    return DOM.eventGetScreenY(this);
  }

  /**
   * Gets whether the shift key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if shift was depressed when the event occurred
   * @throws AssertionError if event type is not one of
   *           {@link Event#MOUSEEVENTS}, {@link Event#ONCLICK},
   *           {@link Event#ONDBLCLICK}, {@link Event#KEYEVENTS}, or
   *           {@link Event#ONCONTEXTMENU}
   */
  public final boolean getShiftKey() throws AssertionError {
    return DOM.eventGetShiftKey(this);
  }

  /**
   * Gets a string representation of this event.
   * 
   * We do not override {@link #toString()} because it is final in
   * {@link JavaScriptObject}.
   * 
   * @return the string representation of this event
   */
  public final String getString() {
    return DOM.eventToString(this);
  }

  /**
   * Returns the element that was the actual target of the given event.
   * 
   * @return the target element
   */
  public final Element getTarget() {
    return DOM.eventGetTarget(this);
  }

  /**
   * Gets the element to which the mouse pointer was moved (only valid for
   * {@link Event#ONMOUSEOUT}).
   * 
   * @return the element to which the mouse pointer was moved
   * @throws AssertionError if event type is not one of
   *           {@link Event#ONMOUSEOVER} or {@link Event#ONMOUSEOUT}
   */
  public final Element getToElement() throws AssertionError {
    return DOM.eventGetToElement(this);
  }

  /**
   * Gets the enumerated type of this event (as defined in {@link Event}).
   * 
   * @return the event's enumerated type
   */
  public final String getType() {
    return DOM.eventGetTypeString(this);
  }

  /**
   * Gets the enumerated type of this event, as defined by {@link #ONCLICK},
   * {@link #ONMOUSEDOWN}, and so forth.
   * 
   * @return the event's enumerated type
   */
  public final int getTypeInt() {
    return DOM.eventGetType(this);
  }

  /**
   * Prevents the browser from taking its default action for the given event.
   */
  public final void preventDefault() {
    DOM.eventPreventDefault(this);
  }
}
