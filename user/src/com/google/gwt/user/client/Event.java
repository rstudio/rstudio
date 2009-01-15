/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * <p>
 * An opaque handle to a native DOM Event. An <code>Event</code> cannot be
 * created directly. Instead, use the <code>Event</code> type when returning a
 * native DOM event from JSNI methods. An <code>Event</code> passed back into
 * JSNI becomes the original DOM event the <code>Event</code> was created from,
 * and can be accessed in JavaScript code as expected. This is typically done by
 * calling methods in the {@link com.google.gwt.user.client.DOM} class.
 * </p>
 */
public class Event extends JavaScriptObject {
  /**
   * Represents a preview of a native {@link Event}.
   */
  public static class NativePreviewEvent extends GwtEvent<NativePreviewHandler>
      implements HasNativeEvent {

    /**
     * Handler type.
     */
    private static Type<NativePreviewHandler> TYPE;

    /**
     * The singleton instance of {@link NativePreviewEvent}.
     */
    private static NativePreviewEvent singleton;

    /**
     * Gets the type associated with this event.
     * 
     * @return returns the handler type
     */
    public static Type<NativePreviewHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<NativePreviewHandler>();
      }
      return TYPE;
    }

    /**
     * Fire a {@link NativePreviewEvent} for the native event.
     * 
     * @param handlers the {@link HandlerManager}
     * @param nativeEvent the native event
     * @return true to fire the event normally, false to cancel the event
     */
    private static boolean fire(HandlerManager handlers, Event nativeEvent) {
      if (TYPE != null && handlers != null) {
        // Revive the event
        singleton.revive();
        singleton.setNativeEvent(nativeEvent);

        // Fire the event
        handlers.fireEvent(singleton);
        return !(singleton.isCanceled() && !singleton.isConsumed());
      }
      return true;
    }

    /**
     * A boolean indicating that the native event should be canceled.
     */
    private boolean isCanceled = false;

    /**
     * A boolean indicating whether or not canceling the native event should be
     * prevented. This supercedes {@link #isCanceled}.
     */
    private boolean isConsumed = false;

    /**
     * A boolean indicating that the current handler is at the top of the event
     * preview stack.
     */
    private boolean isFirstHandler = false;

    /**
     * The event being previewed.
     */
    private Event nativeEvent;

    /**
     * Cancel the native event and prevent it from firing. Note that the event
     * can still fire if another handler calls {@link #consume()}.
     * 
     * Classes overriding this method should still call super.cancel().
     */
    public void cancel() {
      isCanceled = true;
    }

    /**
     * Consume the native event and prevent it from being canceled, even if it
     * has already been canceled by another handler.
     * {@link NativePreviewHandler} that fire first have priority over later
     * handlers, so all handlers should check if the event has already been
     * canceled before calling this method.
     */
    public void consume() {
      isConsumed = true;
    }

    @Override
    public final Type<NativePreviewHandler> getAssociatedType() {
      return TYPE;
    }

    public Event getNativeEvent() {
      return nativeEvent;
    }

    /**
     * Has the event already been canceled? Note that {@link #isConsumed()} will
     * still return true if the native event has also been consumed.
     * 
     * @return true if the event has been canceled
     * @see #cancel()
     */
    public boolean isCanceled() {
      return isCanceled;
    }

    /**
     * Has the native event been consumed? Note that {@link #isCanceled()} will
     * still return true if the native event has also been canceled.
     * 
     * @return true if the event has been consumed
     * @see #consume()
     */
    public boolean isConsumed() {
      return isConsumed;
    }

    /**
     * Is the current handler the first to preview this event?
     * 
     * @return true if the current handler is the first to preview the event
     */
    public boolean isFirstHandler() {
      return isFirstHandler;
    }

    @Override
    protected void dispatch(NativePreviewHandler handler) {
      handler.onPreviewNativeEvent(this);
      singleton.isFirstHandler = false;
    }

    @Override
    protected void revive() {
      super.revive();
      isCanceled = false;
      isConsumed = false;
      isFirstHandler = true;
      nativeEvent = null;
    }

    /**
     * Set the native event.
     * 
     * @param nativeEvent the native {@link Event} being previewed.
     */
    private void setNativeEvent(Event nativeEvent) {
      this.nativeEvent = nativeEvent;
    }
  }

  /**
   * Handler interface for {@link NativePreviewEvent} events.
   */
  public static interface NativePreviewHandler extends EventHandler {
    /**
     * Called when {@link NativePreviewEvent} is fired.
     * 
     * @param event the {@link NativePreviewEvent} that was fired
     */
    void onPreviewNativeEvent(NativePreviewEvent event);
  }

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
   * Fired when the user requests an element's context menu (usually by
   * right-clicking).
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
   * Value returned by accessors when the actual integer value is undefined. In
   * hosted mode, most accessors assert that the requested attribute is reliable
   * across all supported browsers.
   * 
   * @see Event
   */
  @Deprecated
  public static final int UNDEFINED = 0;

  /**
   * The list of {@link NativePreviewHandler}. We use a list instead of a
   * handler manager for efficiency and because we want to fire the handlers in
   * reverse order. When the last handler is removed, handlers is reset to null.
   */
  static HandlerManager handlers;

  /**
   * Adds an event preview to the preview stack. As long as this preview remains
   * on the top of the stack, it will receive all events before they are fired
   * to their listeners. Note that the event preview will receive <u>all </u>
   * events, including those received due to bubbling, whereas normal event
   * handlers only receive explicitly sunk events.
   * 
   * @param preview the event preview to be added to the stack.
   * @deprecated replaced by
   *             {@link #addNativePreviewHandler(NativePreviewHandler)}
   */
  @Deprecated
  public static void addEventPreview(EventPreview preview) {
    DOM.addEventPreview(preview);
  }

  /**
   * <p>
   * Adds a {@link NativePreviewHandler} that will receive all events before
   * they are fired to their handlers. Note that the handler will receive
   * <u>all</u> native events, including those received due to bubbling, whereas
   * normal event handlers only receive explicitly sunk events.
   * </p>
   * 
   * <p>
   * Unlike other event handlers, {@link NativePreviewHandler} are fired in the
   * reverse order that they are added, such that the last
   * {@link NativePreviewEvent} that was added is the first to be fired.
   * </p>
   * 
   * @param handler the {@link NativePreviewHandler}
   * @return {@link HandlerRegistration} used to remove this handler
   */
  public static HandlerRegistration addNativePreviewHandler(
      final NativePreviewHandler handler) {
    assert handler != null : "Cannot add a null handler";
    DOM.maybeInitializeEventSystem();

    // Initialize the type
    NativePreviewEvent.getType();
    if (handlers == null) {
      handlers = new HandlerManager(null, true);
      NativePreviewEvent.singleton = new NativePreviewEvent();
    }
    return handlers.addHandler(NativePreviewEvent.TYPE, handler);
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
    DOM.releaseCapture(elem.<com.google.gwt.user.client.Element> cast());
  }

  /**
   * Removes an element from the preview stack. This element will no longer
   * capture events, though any preview underneath it will begin to do so.
   * 
   * @param preview the event preview to be removed from the stack
   * @deprecated use {@link HandlerRegistration} returned from
   *             {@link Event#addNativePreviewHandler(NativePreviewHandler)}
   */
  @Deprecated
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
    DOM.setCapture(elem.<com.google.gwt.user.client.Element> cast());
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
   * Fire a {@link NativePreviewEvent} for the native event.
   * 
   * @param nativeEvent the native event
   * @return true to fire the event normally, false to cancel the event
   */
  static boolean fireNativePreviewEvent(Event nativeEvent) {
    return NativePreviewEvent.fire(handlers, nativeEvent);
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
   */
  public final boolean getAltKey() {
    return DOM.eventGetAltKey(this);
  }

  /**
   * Gets the mouse buttons that were depressed when the given event occurred.
   * 
   * @return a bit-field, defined by {@link Event#BUTTON_LEFT},
   *         {@link Event#BUTTON_MIDDLE}, and {@link Event#BUTTON_RIGHT}
   */
  public final int getButton() {
    return DOM.eventGetButton(this);
  }

  /**
   * Gets the mouse x-position within the browser window's client area.
   * 
   * @return the mouse x-position
   */
  public final int getClientX() {
    return DOM.eventGetClientX(this);
  }

  /**
   * Gets the mouse y-position within the browser window's client area.
   * 
   * @return the mouse y-position
   */
  public final int getClientY() {
    return DOM.eventGetClientY(this);
  }

  /**
   * Gets whether the CTRL key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if CTRL was depressed when the event occurred
   */
  public final boolean getCtrlKey() {
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
   */
  public final Element getFromElement() {
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
   * @see com.google.gwt.event.dom.client.KeyCodes
   */
  public final int getKeyCode() {
    return DOM.eventGetKeyCode(this);
  }

  /**
   * Gets whether the META key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if META was depressed when the event occurred
   */
  public final boolean getMetaKey() {
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
   */
  public final int getMouseWheelVelocityY() {
    return DOM.eventGetMouseWheelVelocityY(this);
  }

  /**
   * Gets the key-repeat state of this event.
   * 
   * @return <code>true</code> if this key event was an auto-repeat
   */
  public final boolean getRepeat() {
    return DOM.eventGetRepeat(this);
  }

  /**
   * Gets the mouse x-position on the user's display.
   * 
   * @return the mouse x-position
   */
  public final int getScreenX() {
    return DOM.eventGetScreenX(this);
  }

  /**
   * Gets the mouse y-position on the user's display.
   * 
   * @return the mouse y-position
   */
  public final int getScreenY() {
    return DOM.eventGetScreenY(this);
  }

  /**
   * Gets whether the shift key was depressed when the given event occurred.
   * 
   * @return <code>true</code> if shift was depressed when the event occurred
   */
  public final boolean getShiftKey() {
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
   */
  public final Element getToElement() {
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
