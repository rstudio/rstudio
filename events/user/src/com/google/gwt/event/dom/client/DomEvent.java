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
    public Type(int nativeEventType) {
      // All clinit activity should take place here for DomEvent.
      if (registered == null) {
        registered = new RawJsStringMapImpl();
      }
      this.nativeEventTypeInt = nativeEventType;
      registered.put(getType(nativeEventType), this);
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

    /**
     * Wraps the native event.
     * 
     * @param nativeEvent the native event
     * @return the wrapped native event
     */
    abstract EventType wrap(Event nativeEvent);
  }

  private static RawJsStringMapImpl<Type> registered;

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
  public static void fireNativeEvent(int eventType, HandlerManager manager) {
    if (registered != null) {
      DomEvent.Type typeKey = registered.get(getType(eventType));
      if (typeKey != null && manager.isEventHandled(typeKey)) {
        if (typeKey.cached == null || typeKey.cached.isLive()) {
          typeKey.cached = typeKey.wrap(null);
        } else {
          typeKey.cached.reset(null);
        }
        manager.fireEvent(typeKey.cached);
      }
    }
  }

  /**
   * Fires the given native event on the manager.
   * 
   * @param nativeEvent the native event
   * @param manager the event manager
   */
  public static void fireNativeEvent(Event nativeEvent, HandlerManager manager) {
    if (registered != null) {
      DomEvent.Type typeKey = registered.get(nativeEvent.getType());
      if (typeKey != null && manager.isEventHandled(typeKey)) {
        if (typeKey.cached == null || typeKey.cached.isLive()) {
          typeKey.cached = typeKey.wrap(nativeEvent);
        } else {
          typeKey.cached.reset(nativeEvent);
        }
        manager.fireEvent(typeKey.cached);
      }
    }
  }

  private static String getType(int type) {
    switch (type) {
      case 0x01000:
        return "blur";
      case 0x00400:
        return "change";
      case 0x00001:
        return "click";
      case 0x00002:
        return "dblclick";
      case 0x00800:
        return "focus";
      case 0x00080:
        return "keydown";
      case 0x00100:
        return "keypress";
      case 0x00200:
        return "keyup";
      case 0x08000:
        return "load";
      case 0x02000:
        return "losecapture";
      case 0x00004:
        return "mousedown";
      case 0x00040:
        return "mousemove";
      case 0x00020:
        return "mouseout";
      case 0x00010:
        return "mouseover";
      case 0x00008:
        return "mouseup";
      case 0x04000:
        return "scroll";
      case 0x10000:
        return "error";
      case 0x20000:
        return "mousewheel";
      case 0x40000:
        return "contextmenu";
      default:
        return null;
    }
  }

  private Event nativeEvent;

  /**
   * Constructor.
   * 
   * @param nativeEvent the native event
   */
  protected DomEvent(Event nativeEvent) {
    this.nativeEvent = nativeEvent;
  }

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
   * Stops the propagation of the underlying native event.
   */
  public void stopPropagation() {
    assertLive();
    nativeEvent.cancelBubble(true);
  }

  @Override
  public String toString() {
    return getType(getType().getNativeEventTypeInt()) + " event";
  }

  @Override
  protected abstract DomEvent.Type getType();

  void reset(Event event) {
    super.revive();
    nativeEvent = event;
  }
}
