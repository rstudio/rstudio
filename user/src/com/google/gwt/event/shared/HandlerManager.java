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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.user.client.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manager responsible for adding handlers to event sources and firing those
 * handlers on passed in events.
 */
public class HandlerManager {
  /**
   * Deferred binding used to allow users to optionally chose to use the
   * JavaScript handler registry.
   */
  static class WhichRegistry {
    public boolean useJSRegistry() {
      return false;
    }
  }

  /**
   * Allows users to use the JavaScript handler registry. Cut/Copy lines below
   * to use.
   * 
   * <pre>
      <replace-with
           class="com.google.gwt.event.shared.HandlerManager.UseJsRegistry">
         <when-type-is
      class="com.google.gwt.event.shared.HandlerManager.WhichRegistry" />
       </replace-with>
     </pre>
   */
  static class UseJsRegistry extends WhichRegistry {
    @Override
    public boolean useJSRegistry() {
      return true;
    }
  }

  /**
   * The default handler registry.
   */
  private static class JavaHandlerRegistry {
    private final HashMap<GwtEvent.Type<?>, ArrayList<?>> map = new HashMap<GwtEvent.Type<?>, ArrayList<?>>();

    private <H extends EventHandler> void addHandler(Type<H> type, H handler) {
      ArrayList<H> l = get(type);
      if (l == null) {
        l = new ArrayList<H>();
        map.put(type, l);
      }
      l.add(handler);
    }

    private <H extends EventHandler> void fireEvent(GwtEvent<H> event) {
      Type<H> type = event.getAssociatedType();
      int count = getHandlerCount(type);
      for (int i = 0; i < count; i++) {
        H handler = this.<H> getHandler(type, i);
        event.dispatch(handler);
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

    private <H> void removeHandler(GwtEvent.Type<H> eventKey, H handler) {
      ArrayList<H> l = get(eventKey);
      boolean result = l.remove(handler);
      assert result : "Tried to remove unknown handler: " + handler + " from "
          + eventKey;
    }
  }

  /**
   * Optional JavaScript handler registry.
   * 
   * This is in the shared class but should never be called outside of a GWT
   * runtime environment.
   * 
   * The JsHandlerRegistry makes use of the fact that in the large majority of
   * cases, only one or two handlers are added for each event type. Therefore,
   * rather than storing handlers in a list of lists, we store then in a single
   * flattened array with an escape clause to handle the rare case where we have
   * more handlers then expected.
   */
  private static class JsHandlerRegistry extends JavaScriptObject {

    // Used to optimize the JavaScript handler container structure.
    private static int EXPECTED_HANDLERS = 5;

    private static int createIndex() {
      // Need to leave space for the size and the unflattened list if we end up
      // needing it.
      index += EXPECTED_HANDLERS + 2;
      return index;
    }

    /**
     * Required constructor.
     */
    protected JsHandlerRegistry() {
    }

    private <H extends EventHandler> void addHandler(Type<H> type, H myHandler) {

      // The base is the equivalent to a c pointer into the flattened handler
      // data structure.
      int base = type.hashCode();
      int count = getCount(base);
      boolean flattened = isFlattened(base);
      H handler = myHandler;
      // If we already have the maximum number of handlers we can store in the
      // flattened data structure, store the handlers in an external list
      // instead.
      if ((count == EXPECTED_HANDLERS) & flattened) {
        unflatten(base);
        flattened = false;
      }
      if (flattened) {
        setFlatHandler(base, count, handler);
      } else {
        setHandler(base, count, handler);
      }
      setCount(base, count + 1);
    }

    private <H extends EventHandler> void fireEvent(GwtEvent<H> event) {
      Type<H> type = event.getAssociatedType();
      int base = type.hashCode();
      int count = getCount(base);
      boolean isFlattened = isFlattened(base);
      if (isFlattened) {
        for (int i = 0; i < count; i++) {
          // Gets the given handler to fire. JavaScript array has no intrinsic
          // typing information, so cast is inherently necessary.
          H handler = this.<H> getFlatHandler(base, i);
          // Fires the handler.
          event.dispatch(handler);
        }
      } else {
        JavaScriptObject handlers = getHandlers(base);
        for (int i = 0; i < count; i++) {
          // Gets the given handler to fire. JavaScript array has no intrinsic
          // typing information, so cast is inherently necessary.
          H handler = this.<H> getHandler(handlers, i);
          // Fires the handler.
          event.dispatch(handler);
        }
      }
    }

    private native int getCount(int index) /*-{
      var count = this[index];
      return count == null? 0:count;
    }-*/;

    private native <H extends EventHandler> H getFlatHandler(int base, int index) /*-{
      return this[base + 2 + index];
    }-*/;

    private <H extends EventHandler> H getHandler(GwtEvent.Type<H> type,
        int index) {
      int base = type.hashCode();
      return this.<H> getHandler(base, index, isFlattened(base));
    }

    private native <H extends EventHandler> H getHandler(int base, int index,
        boolean flattened) /*-{
      return flattened? this[base + 2 + index]: this[base + 1][index];
    }-*/;

    private native <H extends EventHandler> H getHandler(
        JavaScriptObject handlers, int index) /*-{
      return handlers[index];
    }-*/;

    private int getHandlerCount(GwtEvent.Type<?> eventKey) {
      return getCount(eventKey.hashCode());
    }

    private native JavaScriptObject getHandlers(int base) /*-{
      return this[base + 1];
    }-*/;

    private native boolean isFlattened(int base) /*-{
      return this[base + 1] == null;
    }-*/;

    private <H> void removeHandler(GwtEvent.Type<H> eventKey,
        EventHandler handler) {
      int base = eventKey.hashCode();

      // Removing a handler is unusual, so smaller code is preferable to
      // handling both flat and dangling list of pointers.
      if (isFlattened(base)) {
        unflatten(base);
      }
      boolean result = removeHelper(base, handler);
      // Hiding this behind an assertion as we'd rather not force the compiler
      // to have to include all handler.toString() instances.
      assert result : handler + " did not exist";
    }

    private native boolean removeHelper(int base, EventHandler handler) /*-{
      // Find the handler.
      var count = this[base];
      var handlerList = this[base + 1];
      var handlerIndex = -1;
      for(var index = 0;  index < count; index++){
        if(handlerList[index] == handler){
          handlerIndex = index;
          break;
        }
      }
      if(handlerIndex == -1) {
        return false;
      }

      // Remove the handler.
      var last = count -1;
      for(; handlerIndex < last; handlerIndex++){
        handlerList[handlerIndex] = handlerList[handlerIndex+1]
      }
      handlerList[last] = null;
      this[base] = this[base]-1;
      return true;
    }-*/;

    private native void setCount(int index, int count) /*-{
      this[index] = count;
    }-*/;

    private native void setFlatHandler(int base, int index, EventHandler handler) /*-{
      this[base + 2 + index] = handler;
    }-*/;

    private native void setHandler(int base, int index, EventHandler handler) /*-{
      this[base + 1][index] = handler;
    }-*/;

    private native void unflatten(int base) /*-{
      var handlerList = {};
      var count = this[base];
      var start = base + 2;
       for(var i = 0; i < count; i++){
         handlerList[i] = this[start + i];
         this[start + i] = null;
        }
       this[base + 1] = handlerList;
    }-*/;
  }

  private static final boolean useJsRegistry = GWT.isClient()
      && ((WhichRegistry) GWT.create(WhichRegistry.class)).useJSRegistry();
  private static int index = 0;

  // Used to assign hash codes to gwt event types so they are easy to store in a
  // js structure.
  static int createTypeHashCode() {
    if (useJsRegistry) {
      return JsHandlerRegistry.createIndex();
    } else {
      return ++index;
    }
  }

  private int firingDepth = 0;
  // Only one of JsHandlerRegistry and JavaHandlerRegistry are live at once.
  private JsHandlerRegistry javaScriptRegistry;
  private JavaHandlerRegistry javaRegistry;

  // source of the event.
  private final Object source;

  // Add and remove operations received during dispatch.
  private List<Command> deferredDeltas;

  /**
   * Creates a handler manager with the given source.
   * 
   * @param source the event source
   */
  public HandlerManager(Object source) {
    if (useJsRegistry) {
      javaScriptRegistry = JavaScriptObject.createObject().cast();
    } else {
      javaRegistry = new JavaHandlerRegistry();
    }
    this.source = source;
  }

  /**
   * Adds a handle.
   * 
   * @param <H> The type of handler
   * @param type the event type associated with this handler
   * @param handler the handler
   * @return the handler registration, can be stored in order to remove the
   * handler later
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
      if (useJsRegistry) {
        javaScriptRegistry.fireEvent(event);
      } else {
        javaRegistry.fireEvent(event);
      }
    } finally {
      firingDepth--;
      if (firingDepth == 0) {
        handleQueuedAddsAndRemoves();
      }
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
   * @param <H> the event handler type
   * @param index the index
   * @param type the handler's event type
   * @return the given handler
   */
  public <H extends EventHandler> H getHandler(GwtEvent.Type<H> type, int index) {
    assert index < getHandlerCount(type) : "handlers for " + type.getClass()
        + " have size: " + getHandlerCount(type)
        + " so do not have a handler at index: " + index;
    if (useJsRegistry) {
      return javaScriptRegistry.getHandler(type, index);
    } else {
      return javaRegistry.getHandler(type, index);
    }
  }

  /**
   * Gets the number of handlers listening to the event type.
   * 
   * @param type the event type
   * @return the number of registered handlers
   */
  public int getHandlerCount(Type<?> type) {
    if (useJsRegistry) {
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
  public boolean isEventHandled(Type<?> type) {
    return getHandlerCount(type) > 0;
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
   * @deprecated We currently believe this method will not be needed after
   * listeners are removed in GWT 2.0. If you have a use case for it after that
   * time, please add your comments to issue
   * http://code.google.com/p/google-web-toolkit/issues/detail?id=3102
   */
  public <H extends EventHandler> void removeHandler(GwtEvent.Type<H> type,
      final H handler) {
    if (firingDepth > 0) {
      enqueueRemove(type, handler);
    } else {
      doRemove(type, handler);
    }
  }

  private void defer(Command command) {
    if (deferredDeltas == null) {
      deferredDeltas = new ArrayList<Command>();
    }
    deferredDeltas.add(command);
  }

  private <H extends EventHandler> void doAdd(GwtEvent.Type<H> type,
      final H handler) {
    if (useJsRegistry) {
      javaScriptRegistry.addHandler(type, handler);
    } else {
      javaRegistry.addHandler(type, handler);
    }
  }

  private <H extends EventHandler> void doRemove(GwtEvent.Type<H> type,
      final H handler) {
    if (useJsRegistry) {
      javaScriptRegistry.removeHandler(type, handler);
    } else {
      javaRegistry.removeHandler(type, handler);
    }
  }

  private <H extends EventHandler> void enqueueAdd(final GwtEvent.Type<H> type,
      final H handler) {
    defer(new Command() {
      public void execute() {
        doAdd(type, handler);
      }
    });
  }

  private <H extends EventHandler> void enqueueRemove(
      final GwtEvent.Type<H> type, final H handler) {
    defer(new Command() {
      public void execute() {
        doRemove(type, handler);
      }
    });
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
}
