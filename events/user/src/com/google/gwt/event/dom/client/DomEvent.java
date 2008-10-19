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

import com.google.gwt.event.shared.AbstractEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Event;

/**
 * {@link DomEvent} is a subclass of AbstractEvent that provides events that map
 * to DOM Level 2 Events. It provides an additional method to access the
 * underlying native browser event object as well as a subclass of
 * AbstractEvent.Key that understands GWT event bits used by sinkEvents().
 * 
 */
public abstract class DomEvent extends AbstractEvent {
  /**
   * Type class used by dom event subclasses.
   * 
   * @param <EventType> event type
   * @param <HandlerType> handler type
   */
  public abstract static class Type<EventType extends DomEvent, HandlerType extends EventHandler>
      extends AbstractEvent.Type<EventType, HandlerType> {
    private int nativeEventTypeInt;
    DomEvent cached;

    /**
     * Constructor.
     * 
     * @param nativeEventType the native event type
     */
    public Type(int nativeEventTypeInt, String nativeEventTypeString,
        DomEvent cached) {
      this.cached = cached;
      // All clinit activity should take place here for DomEvent.
      if (registered == null) {
        registered = new RawJsStringMapImpl();
        reverseRegistered = new RawJsStringMapImpl();
      }
      this.nativeEventTypeInt = nativeEventTypeInt;
      registered.put(nativeEventTypeString, this);
      reverseRegistered.put(nativeEventTypeInt + "", this);
    }

    /**
     * Gets the native {@link Event} type integer corresponding to the native
     * event.
     * 
     * @return the native event type
     */
    public int getNativeEventTypeInt() {
      return nativeEventTypeInt;
    }
  }

  private static RawJsStringMapImpl<Type> registered;

  private static RawJsStringMapImpl<Type> reverseRegistered;

  /**
   * Fires the given native event on the manager.
   * 
   * @param nativeEvent the native event
   * @param manager the event manager
   */
  public static void fireNativeEvent(Event nativeEvent, HandlerManager manager) {
    if (registered != null) {
      DomEvent.Type typeKey = registered.get(nativeEvent.getType());
      if (typeKey != null) {
        fire(nativeEvent, manager, typeKey);
      }
    }
  }

  /**
   * Fires the given native event on the manager with a null underlying native
   * event.
   * 
   * <p>
   * This method is used in the rare case that GWT widgets have to fire native
   * events but do not have access to the corresponding native event. It allows
   * the compiler to avoid instantiating event types that are never handlers.
   * </p>
   * 
   * @param eventType the GWT event type representing the type of the native
   *          event.
   * @param manager the event manager
   */
  public static void unsafeFireNativeEvent(int eventType, HandlerManager manager) {
    if (registered != null) {
      DomEvent.Type typeKey = reverseRegistered.get(eventType + "");
      if (typeKey != null) {
        fire(null, manager, typeKey);
      }
    }
  }

  private static void fire(Event nativeEvent, HandlerManager manager,
      DomEvent.Type typeKey) {
    // Store and restore native event just in case we are in recursive
    // loop.
    Event currentNative = null;
    if (typeKey.cached.isLive()) {
      currentNative = typeKey.cached.nativeEvent;
    }
    typeKey.cached.setNativeEvent(nativeEvent);
    manager.fireEvent(typeKey.cached);
    if (currentNative != null) {
      typeKey.cached.setNativeEvent(currentNative);
    }
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
  public void setNativeEvent(Event nativeEvent) {
    super.revive();
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
  protected abstract DomEvent.Type getType();
}
