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

import com.google.web.bindery.event.shared.Event;

/**
 * Manager responsible for adding handlers to event sources and firing those
 * handlers on passed in events. Primitive ancestor of {@link EventBus}, and
 * used at the core of {com.google.gwt.user.client.ui.Widget}.
 * <p>
 * While widget authors should continue to use
 * {@link com.google.gwt.user.client.ui.Widget#addDomHandler(EventHandler, com.google.gwt.event.dom.client.DomEvent.Type)}
 * and
 * {@link com.google.gwt.user.client.ui.Widget#addHandler(EventHandler, com.google.gwt.event.shared.GwtEvent.Type)}
 * , application developers are strongly discouraged from using a HandlerManager
 * instance as a global event dispatch mechanism.
 */
public class HandlerManager implements HasHandlers {

  @SuppressWarnings("deprecation")
  private static class Bus extends com.google.web.bindery.event.shared.SimpleEventBus {
    public Bus(boolean fireInReverseOrder) {
      super(fireInReverseOrder);
    }

    @Override
    protected <H> void doRemove(Event.Type<H> type, Object source, H handler) {
      super.doRemove(type, source, handler);
    }

    @Override
    protected <H> H getHandler(Event.Type<H> type, int index) {
      return super.getHandler(type, index);
    }

    @Override
    protected int getHandlerCount(Event.Type<?> eventKey) {
      return super.getHandlerCount(eventKey);
    }

    @Override
    protected boolean isEventHandled(Event.Type<?> eventKey) {
      return super.isEventHandled(eventKey);
    }
  }

  private final Bus eventBus;

  // source of the events
  private final Object source;

  /**
   * Creates a handler manager with a source to be set on all events fired via
   * {@link #fireEvent(GwtEvent)}. Handlers will be fired in the order that they
   * are added.
   * 
   * @param source the default event source
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
    eventBus = new Bus(fireInReverseOrder);
    this.source = source;
  }

  /**
   * Adds a handler.
   * 
   * @param <H> The type of handler
   * @param type the event type associated with this handler
   * @param handler the handler
   * @return the handler registration, can be stored in order to remove the
   *         handler later
   */
  public <H extends EventHandler> HandlerRegistration addHandler(GwtEvent.Type<H> type,
      final H handler) {
    return new LegacyHandlerWrapper(eventBus.addHandler(type, handler));
  }

  /**
   * Fires the given event to the handlers listening to the event's type.
   * <p>
   * Any exceptions thrown by handlers will be bundled into a
   * {@link UmbrellaException} and then re-thrown after all handlers have
   * completed. An exception thrown by a handler will not prevent other handlers
   * from executing.
   * <p>
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
    event.overrideSource(source);
    try {

      // May throw an UmbrellaException.
      eventBus.fireEvent(event);
    } catch (com.google.web.bindery.event.shared.UmbrellaException e) {
      throw new UmbrellaException(e.getCauses());
    } finally {
      if (oldSource == null) {
        // This was my event, so I should kill it now that I'm done.
        event.kill();
      } else {
        // Restoring the source for the next handler to use.
        event.overrideSource(oldSource);
      }
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
    return eventBus.getHandler(type, index);
  }

  /**
   * Gets the number of handlers listening to the event type.
   * 
   * @param type the event type
   * @return the number of registered handlers
   */
  public int getHandlerCount(GwtEvent.Type<?> type) {
    return eventBus.getHandlerCount(type);
  }

  /**
   * Does this handler manager handle the given event type?
   * 
   * @param e the event type
   * @return whether the given event type is handled
   */
  public boolean isEventHandled(GwtEvent.Type<?> e) {
    return eventBus.isEventHandled(e);
  }

  /**
   * Removes the given handler from the specified event type.
   * 
   * @param <H> handler type
   * 
   * @param type the event type
   * @param handler the handler
   */
  public <H extends EventHandler> void removeHandler(GwtEvent.Type<H> type, final H handler) {
    eventBus.doRemove(type, null, handler);
  }
}
