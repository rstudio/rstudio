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
package com.google.gwt.event.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.AbstractEvent.Type;

/**
 * Manager responsible for adding handlers to event sources and firing those
 * handlers on passed in events.
 */
public class HandlerManager {
  // Used to optimize the JavaScript handler container structure.
  static int EXPECTED_HANDLERS = 5;

  private static final boolean useJs = GWT.isScript();
  private static int index = -EXPECTED_HANDLERS;

  static int createKeyIndex() {
    // Need to leave space for the size and the unflattened list if we end up
    // needing it.
    index += EXPECTED_HANDLERS + 2;
    return index;
  }

  // Only one of JsHandlerRegistry and JavaHandlerRegistry are live at once.
  private final JsHandlerRegistry javaScriptRegistry;
  private final JavaHandlerRegistry javaRegistry;

  // 
  private final Object source;

  /**
   * Creates a handler manager with the given source.
   * 
   * @param source the event source
   */
  public HandlerManager(Object source) {
    if (useJs) {
      javaScriptRegistry = JsHandlerRegistry.create();
      javaRegistry = null;
    } else {
      javaRegistry = new JavaHandlerRegistry();
      javaScriptRegistry = null;
    }
    this.source = source;
  }

  /**
   * Adds a handle.
   * 
   * @param <HandlerType> The type of handler.
   * @param type the event type associated with this handler
   * @param handler the handler
   * @return the handler registration, can be stored in order to remove the
   *         handler later
   */
  public <HandlerType extends EventHandler> HandlerRegistration addHandler(
      AbstractEvent.Type<?, HandlerType> type, final HandlerType handler) {
    if (useJs) {
      javaScriptRegistry.addHandler(type, handler);
    } else {
      javaRegistry.addHandler(type, handler);
    }
    return new HandlerRegistration(this, type, handler);
  }

  /**
   * Clears all the handlers associated with the given type.
   * 
   * @param type the type
   */
  public void clearHandlers(Type<?, ?> type) {
    if (useJs) {
      javaScriptRegistry.clearHandlers(type);
    } else {
      javaRegistry.clearHandlers(type);
    }
  }

  /**
   * Fires the given event to the handlers listening to the event's type.
   * 
   * @param event the event
   */
  public void fireEvent(AbstractEvent event) {
    Object oldSource = event.getSource();
    event.setSource(source);
    if (useJs) {
      javaScriptRegistry.fireEvent(event);
    } else {
      javaRegistry.fireEvent(event);
    }
    if (oldSource == null) {
      // This was my event, so I should kill it now that I'm done.
      event.onRelease();
    } else {
      // Restoring the source for the next handler to use.
      event.setSource(oldSource);
    }
  }

  /**
   * Gets the handler at the given index.
   * 
   * @param <HandlerType> the event handler type
   * @param index the index
   * @param type the handler's event type
   * @return the given handler
   */
  public <HandlerType extends EventHandler> HandlerType getHandler(
      AbstractEvent.Type<?, HandlerType> type, int index) {
    if (useJs) {
      return (HandlerType) javaScriptRegistry.getHandler(type, index);
    } else {
      return (HandlerType) javaRegistry.getHandler(type, index);
    }
  }

  /**
   * Gets the number of handlers listening to the event type.
   * 
   * @param type the event type
   * @return the number of registered handlers
   */
  public int getHandlerCount(Type type) {
    if (useJs) {
      return javaScriptRegistry.getHandlerCount(type);
    } else {
      return javaRegistry.getHandlerCount(type);
    }
  }

  /**
   * Are there handlers in this manager listening to the given event type?
   * 
   * @param type the event type
   * @return are handlers listening on the given event type
   */
  public boolean isEventHandled(Type type) {
    return getHandlerCount(type) > 0;
  }

  /**
   * Removes the given handler from the specified event type. Normally,
   * applications should call {@link HandlerRegistration#removeHandler()}
   * instead. This method is provided primary to support deprecated APIS.
   * 
   * @param <HandlerType> handler type
   * 
   * @param type the event type
   * @param handler the handler
   */
  public <HandlerType extends EventHandler> void removeHandler(
      AbstractEvent.Type<?, HandlerType> type, final HandlerType handler) {
    if (useJs) {
      javaScriptRegistry.removeHandler(type, handler);
    } else {
      javaRegistry.removeHandler(type, handler);
    }
  }
}
