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
package com.google.gwt.event.dom.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * {@link DomEvent} is a subclass of {@link GwtEvent} that provides events that
 * underlying native browser event object as well as a subclass of {@link Type}
 * that understands GWT event bits used by sinkEvents().
 * 
 * @param <H> handler type
 * 
 */
public abstract class DomEvent<H extends EventHandler> extends GwtEvent<H>
    implements HasNativeEvent {

  /**
   * Type class used by dom event subclasses. Type is specialized for dom in
   * order to carry information about the native event.
   * 
   * @param <H> handler type
   */
  public static class Type<H extends EventHandler> extends GwtEvent.Type<H> {
    private DomEvent<H> flyweight;
    private String name;

    /**
     * This constructor allows dom event types to be triggered by the
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, HasHandlers)}
     * method. It should only be used by implementors supporting new dom events.
     * 
     * <p>
     * Any such dom event type must act as a flyweight around a native event
     * object.
     * </p>
     * 
     * @param eventName the raw native event name
     * @param flyweight the instance that will be used as a flyweight to wrap a
     *          native event
     */
    public Type(String eventName, DomEvent<H> flyweight) {
      this.flyweight = flyweight;

      // Until we have eager clinits implemented, we are manually initializing
      // DomEvent here.
      if (registered == null) {
        init();
      }
      registered.unsafePut(eventName, this);
      name = eventName;
    }

    /**
     * Gets the name associated with this event type.
     * 
     * @return the name of this event type
     */
    public String getName() {
      return name;
    }
  }

  private static PrivateMap<Type<?>> registered;

  /**
   * Fires the given native event on the specified handlers.
   * 
   * @param nativeEvent the native event
   * @param handlerSource the source of the handlers to fire
   */
  public static void fireNativeEvent(NativeEvent nativeEvent,
      HasHandlers handlerSource) {
    fireNativeEvent(nativeEvent, handlerSource, null);
  }

  /**
   * Fires the given native event on the specified handlers.
   * 
   * @param nativeEvent the native event
   * @param handlerSource the source of the handlers to fire
   * @param relativeElem the element relative to which event coordinates will be
   *          measured
   */
  public static void fireNativeEvent(NativeEvent nativeEvent,
      HasHandlers handlerSource, Element relativeElem) {
    assert nativeEvent != null : "nativeEvent must not be null";

    if (registered != null) {
      final DomEvent.Type<?> typeKey = registered.unsafeGet(nativeEvent.getType());
      if (typeKey != null) {
        // Store and restore native event just in case we are in recursive
        // loop.
        NativeEvent currentNative = typeKey.flyweight.nativeEvent;
        Element currentRelativeElem = typeKey.flyweight.relativeElem;
        typeKey.flyweight.setNativeEvent(nativeEvent);
        typeKey.flyweight.setRelativeElement(relativeElem);

        handlerSource.fireEvent(typeKey.flyweight);

        typeKey.flyweight.setNativeEvent(currentNative);
        typeKey.flyweight.setRelativeElement(currentRelativeElem);
      }
    }
  }

  // This method can go away once we have eager clinits.
  static void init() {
    registered = new PrivateMap<Type<?>>();
  }

  private NativeEvent nativeEvent;
  private Element relativeElem;

  @Override
  public abstract DomEvent.Type<H> getAssociatedType();

  public final NativeEvent getNativeEvent() {
    assertLive();
    return nativeEvent;
  }

  /**
   * Gets the element relative to which event coordinates will be measured.
   * If this element is <code>null</code>, event coordinates will be measured
   * relative to the window's client area.
   * 
   * @return the event's relative element
   */
  public final Element getRelativeElement() {
    assertLive();
    return relativeElem;
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
  public final void setNativeEvent(NativeEvent nativeEvent) {
    this.nativeEvent = nativeEvent;
  }

  /**
   * Gets the element relative to which event coordinates will be measured.
   * 
   * @param relativeElem the event's relative element
   */
  public void setRelativeElement(Element relativeElem) {
    this.relativeElem = relativeElem;
  }

  /**
   * Stops the propagation of the underlying native event.
   */
  public void stopPropagation() {
    assertLive();
    nativeEvent.stopPropagation();
  }
}
