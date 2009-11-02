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
package com.google.gwt.event.shared;

import com.google.gwt.event.shared.GwtEvent.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager responsible for adding handlers to event sources and firing those
 * handlers on passed in events.
 */
public class HandlerManager {

  /**
   * Interface for queued add/remove operations.
   */
  private interface AddOrRemoveCommand {
    void execute();
  }

  /**
   * Inner class used to actually contain the handlers.
   */
  private static class HandlerRegistry {
    private final HashMap<GwtEvent.Type<?>, ArrayList<?>> map = new HashMap<GwtEvent.Type<?>, ArrayList<?>>();

    private <H extends EventHandler> void addHandler(Type<H> type, H handler) {
      ArrayList<H> l = get(type);
      if (l == null) {
        l = new ArrayList<H>();
        map.put(type, l);
      }
      l.add(handler);
    }

    private <H extends EventHandler> void fireEvent(GwtEvent<H> event,
        boolean isReverseOrder) {
      Type<H> type = event.getAssociatedType();
      int count = getHandlerCount(type);
      if (isReverseOrder) {
        for (int i = count - 1; i >= 0; i--) {
          H handler = this.<H> getHandler(type, i);
          event.dispatch(handler);
        }
      } else {
        for (int i = 0; i < count; i++) {
          H handler = this.<H> getHandler(type, i);
          event.dispatch(handler);
        }
      }
    }

    @SuppressWarnings("unchecked")
    private <H> ArrayList<H> get(GwtEvent.Type<H> type) {
      // This cast is safe because we control the puts.
      return (ArrayList<H>) map.get(type);
    }

    private <H extends EventHandler> H getHandler(GwtEvent.Type<H> eventKey,
        int index) {
      ArrayList<H> l = get(eventKey);
      return l.get(index);
    }

    private int getHandlerCount(GwtEvent.Type<?> eventKey) {
      ArrayList<?> l = map.get(eventKey);
      return l == null ? 0 : l.size();
    }

    private boolean isEventHandled(GwtEvent.Type<?> eventKey) {
      return map.containsKey(eventKey);
    }

    private <H> void removeHandler(GwtEvent.Type<H> eventKey, H handler) {
      ArrayList<H> l = get(eventKey);
      boolean result = (l == null) ? false : l.remove(handler);
      if (result && l.size() == 0) {
        map.remove(eventKey);
      }
      assert result : "Tried to remove unknown handler: " + handler + " from "
          + eventKey;
    }
  }

  private int firingDepth = 0;
  private boolean isReverseOrder;

  // map storing the actual handlers
  private HandlerRegistry registry;

  // source of the event.
  private final Object source;

  // Add and remove operations received during dispatch.
  private List<AddOrRemoveCommand> deferredDeltas;

  /**
   * Creates a handler manager with the given source. Handlers will be fired in
   * the order that they are added.
   * 
   * @param source the event source
   */
  public HandlerManager(Object source) {
    this(source, false);
  }

  /**
   * Creates a handler manager with the given source, specifying the order in
   * which handlers are fired.
   * 
   * @param source the event source
   * @param fireInReverseOrder true to fire handlers in reverse order
   */
  public HandlerManager(Object source, boolean fireInReverseOrder) {
    registry = new HandlerRegistry();
    this.source = source;
    this.isReverseOrder = fireInReverseOrder;
  }

  /**
   * Adds a handle.
   * 
   * @param <H> The type of handler
   * @param type the event type associated with this handler
   * @param handler the handler
   * @return the handler registration, can be stored in order to remove the
   *         handler later
   */
  public <H extends EventHandler> HandlerRegistration addHandler(
      GwtEvent.Type<H> type, final H handler) {
    assert type != null : "Cannot add a handler with a null type";
    assert handler != null : "Cannot add a null handler";
    if (firingDepth > 0) {
      enqueueAdd(type, handler);
    } else {
      doAdd(type, handler);
    }

    return new DefaultHandlerRegistration(this, type, handler);
  }

  /**
   * Fires the given event to the handlers listening to the event's type.
   * 
   * Note, any subclass should be very careful about overriding this method, as
   * adds/removes of handlers will not be safe except within this
   * implementation.
   * 
   * @param event the event
   */
  public void fireEvent(GwtEvent<?> event) {
    // If it not live we should revive it.
    if (!event.isLive()) {
      event.revive();
    }
    Object oldSource = event.getSource();
    event.setSource(source);
    try {
      firingDepth++;

      registry.fireEvent(event, isReverseOrder);

    } finally {
      firingDepth--;
      if (firingDepth == 0) {
        handleQueuedAddsAndRemoves();
      }
    }
    if (oldSource == null) {
      // This was my event, so I should kill it now that I'm done.
      event.kill();
    } else {
      // Restoring the source for the next handler to use.
      event.setSource(oldSource);
    }
  }

  /**
   * Gets the handler at the given index.
   * 
   * @param <H> the event handler type
   * @param index the index
   * @param type the handler's event type
   * @return the given handler
   */
  public <H extends EventHandler> H getHandler(GwtEvent.Type<H> type, int index) {
    assert index < getHandlerCount(type) : "handlers for " + type.getClass()
        + " have size: " + getHandlerCount(type)
        + " so do not have a handler at index: " + index;
    return registry.getHandler(type, index);
  }

  /**
   * Gets the number of handlers listening to the event type.
   * 
   * @param type the event type
   * @return the number of registered handlers
   */
  public int getHandlerCount(Type<?> type) {
    return registry.getHandlerCount(type);
  }

  /**
   * Does this handler manager handle the given event type?
   * 
   * @param e the event type
   * @return whether the given event type is handled
   */
  public boolean isEventHandled(Type<?> e) {
    return registry.isEventHandled(e);
  }

  /**
   * Removes the given handler from the specified event type. Normally,
   * applications should call {@link HandlerRegistration#removeHandler()}
   * instead.
   * 
   * @param <H> handler type
   * 
   * @param type the event type
   * @param handler the handler
   */
  public <H extends EventHandler> void removeHandler(GwtEvent.Type<H> type,
      final H handler) {
    if (firingDepth > 0) {
      enqueueRemove(type, handler);
    } else {
      doRemove(type, handler);
    }
  }

  /**
   * Not part of the public API, available only to allow visualization tools to
   * be developed in gwt-incubator.
   * 
   * @return a map of all handlers in this handler manager
   */
  Map<GwtEvent.Type<?>, ArrayList<?>> createHandlerInfo() {
    return registry.map;
  }

  private void defer(AddOrRemoveCommand command) {
    if (deferredDeltas == null) {
      deferredDeltas = new ArrayList<AddOrRemoveCommand>();
    }
    deferredDeltas.add(command);
  }

  private <H extends EventHandler> void doAdd(GwtEvent.Type<H> type,
      final H handler) {
    registry.addHandler(type, handler);
  }

  private <H extends EventHandler> void doRemove(GwtEvent.Type<H> type,
      final H handler) {
    registry.removeHandler(type, handler);
  }

  private <H extends EventHandler> void enqueueAdd(final GwtEvent.Type<H> type,
      final H handler) {
    defer(new AddOrRemoveCommand() {
      public void execute() {
        doAdd(type, handler);
      }
    });
  }

  private <H extends EventHandler> void enqueueRemove(
      final GwtEvent.Type<H> type, final H handler) {
    defer(new AddOrRemoveCommand() {
      public void execute() {
        doRemove(type, handler);
      }
    });
  }

  private void handleQueuedAddsAndRemoves() {
    if (deferredDeltas != null) {
      try {
        for (AddOrRemoveCommand c : deferredDeltas) {
          c.execute();
        }
      } finally {
        deferredDeltas = null;
      }
    }
  }
}
