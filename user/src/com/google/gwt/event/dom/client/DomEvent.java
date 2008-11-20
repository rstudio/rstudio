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
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Event;

/**
 * {@link DomEvent} is a subclass of {@link GwtEvent} that provides events that
 * map to DOM Level 2 Events. It provides an additional method to access the
 * underlying native browser event object as well as a subclass of {@link Type}
 * that understands GWT event bits used by sinkEvents().
 * 
 * @param <H> handler type
 * 
 */
public abstract class DomEvent<H extends EventHandler> extends GwtEvent<H> {
  /**
   * Type class used by dom event subclasses. Type is specialized for dom in
   * order to carry information about the native event.
   * 
   * @param <H> handler type
   */
  public static class Type<H extends EventHandler> extends GwtEvent.Type<H> {
    private final int eventToSink;
    private DomEvent<H> flyweight;

    /**
     * Constructor.
     * 
     * @param eventToSink the native event type to sink
     * 
     */
    public Type(int eventToSink) {
      this.eventToSink = eventToSink;
    }

    /**
     * This constructor allows dom event types to be triggered by the
     * {@link DomEvent#fireNativeEvent(Event, HandlerManager)} method. It should
     * only be used by implementors supporting new dom events.
     * <p>
     * Any such dom event type must act as a flyweight around a native event
     * object.
     * </p>
     * 
     * 
     * @param eventToSink the integer value used by sink events to set up event
     * handling for this dom type
     * @param eventName the raw native event name
     * @param flyweight the instance that will be used as a flyweight to wrap a
     * native event
     */
    protected Type(int eventToSink, String eventName, DomEvent<H> flyweight) {
      this.flyweight = flyweight;
      this.eventToSink = eventToSink;

      // Until we have eager clinits implemented, we are manually initializing
      // DomEvent here.
      if (registered == null) {
        init();
      }
      registered.unsafePut(eventName, this);
    }

    Type(int nativeEventTypeInt, String eventName, DomEvent<H> cached,
        String... auxNames) {
      this(nativeEventTypeInt, eventName, cached);
      for (int i = 0; i < auxNames.length; i++) {
        registered.unsafePut(auxNames[i], this);
      }
    }

    /**
     * Gets the integer defined by the native {@link Event} type needed to hook
     * up event handling when the user calls
     * {@link com.google.gwt.user.client.DOM#sinkEvents(com.google.gwt.user.client.Element, int)}
     * .
     * 
     * @return the native event type
     */
    public int getEventToSink() {
      return eventToSink;
    }
  }

  private static PrivateMap<Type<?>> registered;
 
  /**
   * Fires the given native event on the specified handlers.
   * 
   * @param nativeEvent the native event
   * @param handlers the event manager containing the handlers to fire (may be
   *          null)
   */
  public static void fireNativeEvent(Event nativeEvent, HandlerManager handlers) {
    assert nativeEvent != null : "nativeEvent must not be null";
    if (registered != null && handlers != null) {
      final DomEvent.Type<?> typeKey = registered.unsafeGet(nativeEvent.getType());
      if (typeKey != null) {
        // Store and restore native event just in case we are in recursive
        // loop.
        Event currentNative = typeKey.flyweight.nativeEvent;
        typeKey.flyweight.setNativeEvent(nativeEvent);
        handlers.fireEvent(typeKey.flyweight);
        typeKey.flyweight.setNativeEvent(currentNative);
      }
    }
  }
 
  // This method can go away once we have eager clinits.
  static void init() {
    registered = new PrivateMap<Type<?>>();
  }

  private Event nativeEvent;

  /**
   * Gets the underlying native event for this {@link DomEvent}.
   * 
   * @return gets the native event
   */
  public final Event getNativeEvent() {
    assertLive();
    return nativeEvent;
  }

  /**
   * Prevents the wrapped native event's default action.
   */
  public void preventDefault() {
    assertLive();
    nativeEvent.preventDefault();
  }

  /**
   * Sets the native event associated with this dom event. In general, dom
   * events should be fired using the static firing methods.
   * 
   * @param nativeEvent the native event
   */
  public final void setNativeEvent(Event nativeEvent) {
    this.nativeEvent = nativeEvent;
  }

  /**
   * Stops the propagation of the underlying native event.
   */
  public void stopPropagation() {
    assertLive();
    nativeEvent.cancelBubble(true);
  }

  @Override
  protected abstract DomEvent.Type<H> getAssociatedType();
}
