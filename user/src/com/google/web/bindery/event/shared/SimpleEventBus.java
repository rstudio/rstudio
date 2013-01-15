/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.event.shared;

import com.google.web.bindery.event.shared.Event.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation of {@link EventBus}.
 */
public class SimpleEventBus extends EventBus {
  private interface Command {
    void execute();
  }

  private final boolean isReverseOrder;

  private int firingDepth = 0;

  /**
   * Add and remove operations received during dispatch.
   */
  private List<Command> deferredDeltas;

  /**
   * Map of event type to map of event source to list of their handlers.
   */
  private final Map<Event.Type<?>, Map<Object, List<?>>> map =
      new HashMap<Event.Type<?>, Map<Object, List<?>>>();

  public SimpleEventBus() {
    this(false);
  }

  /**
   * Allows creation of an instance that fires its handlers in the reverse of
   * the order in which they were added, although filtered handlers all fire
   * before unfiltered handlers.
   * <p>
   * 
   * @deprecated This is a legacy feature, required by GWT's old HandlerManager.
   *             Reverse order is not honored for handlers tied to a specific
   *             event source (via {@link #addHandlerToSource}.
   */
  @Deprecated
  protected SimpleEventBus(boolean fireInReverseOrder) {
    isReverseOrder = fireInReverseOrder;
  }

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    return doAdd(type, null, handler);
  }

  @Override
  public <H> HandlerRegistration addHandlerToSource(final Event.Type<H> type, final Object source,
      final H handler) {
    if (source == null) {
      throw new NullPointerException("Cannot add a handler with a null source");
    }

    return doAdd(type, source, handler);
  }

  @Override
  public void fireEvent(Event<?> event) {
    doFire(event, null);
  }

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    if (source == null) {
      throw new NullPointerException("Cannot fire from a null source");
    }
    doFire(event, source);
  }

  /**
   * @deprecated required by legacy features in GWT's old HandlerManager
   */
  @Deprecated
  protected <H> void doRemove(Event.Type<H> type, Object source, H handler) {
    if (firingDepth > 0) {
      enqueueRemove(type, source, handler);
    } else {
      doRemoveNow(type, source, handler);
    }
  }

  /**
   * @deprecated required by legacy features in GWT's old HandlerManager
   */
  @Deprecated
  protected <H> H getHandler(Event.Type<H> type, int index) {
    assert index < getHandlerCount(type) : "handlers for " + type.getClass() + " have size: "
        + getHandlerCount(type) + " so do not have a handler at index: " + index;

    List<H> l = getHandlerList(type, null);
    return l.get(index);
  }

  /**
   * @deprecated required by legacy features in GWT's old HandlerManager
   */
  @Deprecated
  protected int getHandlerCount(Event.Type<?> eventKey) {
    return getHandlerList(eventKey, null).size();
  }

  /**
   * @deprecated required by legacy features in GWT's old HandlerManager
   */
  @Deprecated
  protected boolean isEventHandled(Event.Type<?> eventKey) {
    return map.containsKey(eventKey);
  }

  private void defer(Command command) {
    if (deferredDeltas == null) {
      deferredDeltas = new ArrayList<Command>();
    }
    deferredDeltas.add(command);
  }

  private <H> HandlerRegistration doAdd(final Event.Type<H> type, final Object source,
      final H handler) {
    if (type == null) {
      throw new NullPointerException("Cannot add a handler with a null type");
    }
    if (handler == null) {
      throw new NullPointerException("Cannot add a null handler");
    }

    if (firingDepth > 0) {
      enqueueAdd(type, source, handler);
    } else {
      doAddNow(type, source, handler);
    }

    return new HandlerRegistration() {
      public void removeHandler() {
        doRemove(type, source, handler);
      }
    };
  }

  private <H> void doAddNow(Event.Type<H> type, Object source, H handler) {
    List<H> l = ensureHandlerList(type, source);
    l.add(handler);
  }

  private <H> void doFire(Event<H> event, Object source) {
    if (event == null) {
      throw new NullPointerException("Cannot fire null event");
    }
    try {
      firingDepth++;

      if (source != null) {
        setSourceOfEvent(event, source);
      }

      List<H> handlers = getDispatchList(event.getAssociatedType(), source);
      Set<Throwable> causes = null;

      ListIterator<H> it =
          isReverseOrder ? handlers.listIterator(handlers.size()) : handlers.listIterator();
      while (isReverseOrder ? it.hasPrevious() : it.hasNext()) {
        H handler = isReverseOrder ? it.previous() : it.next();

        try {
          dispatchEvent(event, handler);
        } catch (Throwable e) {
          if (causes == null) {
            causes = new HashSet<Throwable>();
          }
          causes.add(e);
        }
      }

      if (causes != null) {
        throw new UmbrellaException(causes);
      }
    } finally {
      firingDepth--;
      if (firingDepth == 0) {
        handleQueuedAddsAndRemoves();
      }
    }
  }

  private <H> void doRemoveNow(Event.Type<H> type, Object source, H handler) {
    List<H> l = getHandlerList(type, source);

    boolean removed = l.remove(handler);

    if (removed && l.isEmpty()) {
      prune(type, source);
    }
  }

  private <H> void enqueueAdd(final Event.Type<H> type, final Object source, final H handler) {
    defer(new Command() {
      public void execute() {
        doAddNow(type, source, handler);
      }
    });
  }

  private <H> void enqueueRemove(final Event.Type<H> type, final Object source, final H handler) {
    defer(new Command() {
      public void execute() {
        doRemoveNow(type, source, handler);
      }
    });
  }

  private <H> List<H> ensureHandlerList(Event.Type<H> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);
    if (sourceMap == null) {
      sourceMap = new HashMap<Object, List<?>>();
      map.put(type, sourceMap);
    }

    // safe, we control the puts.
    @SuppressWarnings("unchecked")
    List<H> handlers = (List<H>) sourceMap.get(source);
    if (handlers == null) {
      handlers = new ArrayList<H>();
      sourceMap.put(source, handlers);
    }

    return handlers;
  }

  private <H> List<H> getDispatchList(Event.Type<H> type, Object source) {
    List<H> directHandlers = getHandlerList(type, source);
    if (source == null) {
      return directHandlers;
    }

    List<H> globalHandlers = getHandlerList(type, null);

    List<H> rtn = new ArrayList<H>(directHandlers);
    rtn.addAll(globalHandlers);
    return rtn;
  }

  private <H> List<H> getHandlerList(Event.Type<H> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);
    if (sourceMap == null) {
      return Collections.emptyList();
    }

    // safe, we control the puts.
    @SuppressWarnings("unchecked")
    List<H> handlers = (List<H>) sourceMap.get(source);
    if (handlers == null) {
      return Collections.emptyList();
    }

    return handlers;
  }

  private void handleQueuedAddsAndRemoves() {
    if (deferredDeltas != null) {
      try {
        for (Command c : deferredDeltas) {
          c.execute();
        }
      } finally {
        deferredDeltas = null;
      }
    }
  }

  private void prune(Event.Type<?> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);

    List<?> pruned = sourceMap.remove(source);

    assert pruned != null : "Can't prune what wasn't there";
    assert pruned.isEmpty() : "Pruned unempty list!";

    if (sourceMap.isEmpty()) {
      map.remove(type);
    }
  }
}