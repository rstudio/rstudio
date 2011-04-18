/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.GwtEvent.Type;

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
  private final boolean isReverseOrder;

  private int firingDepth = 0;

  /**
   * Add and remove operations received during dispatch.
   */
  private List<ScheduledCommand> deferredDeltas;

  /**
   * Map of event type to map of event source to list of their handlers.
   */
  private final Map<GwtEvent.Type<?>, Map<Object, List<?>>> map = new HashMap<GwtEvent.Type<?>, Map<Object, List<?>>>();

  public SimpleEventBus() {
    this(false);
  }

  /**
   * Allows creation of an instance that fires its handlers in the reverse of
   * the order in which they were added, although filtered handlers all fire
   * before unfiltered handlers.
   * <p>
   * 
   * @deprecated This is a legacy feature, required by HandlerManager. Package
   *             protected because it is a bad idea to rely upon the order of
   *             event dispatch, and because fully supporting it (that is, not
   *             segregating filtered and unfiltered handlers, a distinction not
   *             used by HandlerManager) is not worth the effort.
   */
  @Deprecated
  SimpleEventBus(boolean fireInReverseOrder) {
    isReverseOrder = fireInReverseOrder;
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type,
      H handler) {
    if (type == null) {
      throw new NullPointerException("Cannot add a handler with a null type");
    }
    if (handler == null) {
      throw new NullPointerException("Cannot add a null handler");
    }

    return doAdd(type, null, handler);
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandlerToSource(
      final GwtEvent.Type<H> type, final Object source, final H handler) {
    if (type == null) {
      throw new NullPointerException("Cannot add a handler with a null type");
    }
    if (source == null) {
      throw new NullPointerException("Cannot add a handler with a null source");
    }
    if (handler == null) {
      throw new NullPointerException("Cannot add a null handler");
    }

    return doAdd(type, source, handler);
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    if (event == null) {
      throw new NullPointerException("Cannot fire null event");
    }
    doFire(event, null);
  }

  @Override
  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    if (event == null) {
      throw new NullPointerException("Cannot fire null event");
    }
    if (source == null) {
      throw new NullPointerException("Cannot fire from a null source");
    }
    doFire(event, source);
  }

  /**
   * Package protected to support legacy features in HandlerManager.
   */
  <H extends EventHandler> void doRemove(
      com.google.gwt.event.shared.GwtEvent.Type<H> type, Object source,
      H handler) {
    if (firingDepth > 0) {
      enqueueRemove(type, source, handler);
    } else {
      doRemoveNow(type, source, handler);
    }
  }

  /**
   * Package protected to support legacy features in HandlerManager.
   */
  @Deprecated
  <H extends EventHandler> H getHandler(GwtEvent.Type<H> type, int index) {
    assert index < getHandlerCount(type) : "handlers for " + type.getClass()
        + " have size: " + getHandlerCount(type)
        + " so do not have a handler at index: " + index;

    List<H> l = getHandlerList(type, null);
    return l.get(index);
  }

  /**
   * Package protected to support legacy features in HandlerManager.
   */
  @Deprecated
  int getHandlerCount(GwtEvent.Type<?> eventKey) {
    return getHandlerList(eventKey, null).size();
  }

  /**
   * Package protected to support legacy features in HandlerManager.
   */
  @Deprecated
  boolean isEventHandled(GwtEvent.Type<?> eventKey) {
    return map.containsKey(eventKey);
  }

  private void defer(ScheduledCommand command) {
    if (deferredDeltas == null) {
      deferredDeltas = new ArrayList<ScheduledCommand>();
    }
    deferredDeltas.add(command);
  }

  private <H extends EventHandler> HandlerRegistration doAdd(
      final GwtEvent.Type<H> type, final Object source, final H handler) {
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

  private <H extends EventHandler> void doAddNow(GwtEvent.Type<H> type,
      Object source, H handler) {
    List<H> l = ensureHandlerList(type, source);
    l.add(handler);
  }

  private <H extends EventHandler> void doFire(GwtEvent<H> event, Object source) {
    try {
      firingDepth++;

      if (source != null) {
        event.setSource(source);
      }

      List<H> handlers = getDispatchList(event.getAssociatedType(), source);
      Set<Throwable> causes = null;

      ListIterator<H> it = isReverseOrder
          ? handlers.listIterator(handlers.size()) : handlers.listIterator();
      while (isReverseOrder ? it.hasPrevious() : it.hasNext()) {
        H handler = isReverseOrder ? it.previous() : it.next();

        try {
          event.dispatch(handler);
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

  private <H> void doRemoveNow(GwtEvent.Type<H> type, Object source, H handler) {
    List<H> l = getHandlerList(type, source);

    boolean removed = l.remove(handler);
    assert removed : "redundant remove call";
    if (removed && l.isEmpty()) {
      prune(type, source);
    }
  }

  private <H extends EventHandler> void enqueueAdd(final GwtEvent.Type<H> type,
      final Object source, final H handler) {
    defer(new ScheduledCommand() {
      public void execute() {
        doAddNow(type, source, handler);
      }
    });
  }

  private <H extends EventHandler> void enqueueRemove(
      final GwtEvent.Type<H> type, final Object source, final H handler) {
    defer(new ScheduledCommand() {
      public void execute() {
        doRemoveNow(type, source, handler);
      }
    });
  }

  private <H> List<H> ensureHandlerList(GwtEvent.Type<H> type, Object source) {
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

  private <H> List<H> getDispatchList(GwtEvent.Type<H> type, Object source) {
    List<H> directHandlers = getHandlerList(type, source);
    if (source == null) {
      return directHandlers;
    }

    List<H> globalHandlers = getHandlerList(type, null);

    List<H> rtn = new ArrayList<H>(directHandlers);
    rtn.addAll(globalHandlers);
    return rtn;
  }

  private <H> List<H> getHandlerList(GwtEvent.Type<H> type, Object source) {
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
        for (ScheduledCommand c : deferredDeltas) {
          c.execute();
        }
      } finally {
        deferredDeltas = null;
      }
    }
  }

  private void prune(GwtEvent.Type<?> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);

    List<?> pruned = sourceMap.remove(source);

    assert pruned != null : "Can't prune what wasn't there";
    assert pruned.isEmpty() : "Pruned unempty list!";

    if (sourceMap.isEmpty()) {
      map.remove(type);
    }
  }
}