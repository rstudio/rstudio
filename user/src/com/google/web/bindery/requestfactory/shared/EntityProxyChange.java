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
package com.google.web.bindery.requestfactory.shared;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * Event posted by a {@link RequestFactory} when changes to an entity are
 * detected. Provides a {@link WriteOperation} value describing the change, and
 * the {@link EntityProxyId} of the entity in question.
 * <p>
 * EntityProxyChange events are posted with the relevant EntityProxy Class as
 * their source, allowing handlers to register for changes only of the type they
 * care about via {@link #registerForProxyType(EventBus, Class, Handler)}.
 * 
 * @param <P> the type of the proxy
 * 
 * @see RequestFactory#initialize(EventBus)
 * @see RequestFactory#find(EntityProxyId)
 */
public class EntityProxyChange<P extends EntityProxy> extends Event<EntityProxyChange.Handler<P>> {

  /**
   * Implemented by methods that handle EntityProxyChange events.
   * 
   * @param <P> the proxy type
   */
  public interface Handler<P extends EntityProxy> {
    /**
     * Called when an {@link EntityProxyChange} event is fired.
     * 
     * @param event an {@link EntityProxyChange} instance
     */
    void onProxyChange(EntityProxyChange<P> event);
  }

  private static final Type<EntityProxyChange.Handler<?>> TYPE =
      new Type<EntityProxyChange.Handler<?>>();

  /**
   * Register a handler for a EntityProxyChange events for a particular proxy
   * class.
   * 
   * @param eventBus the {@link EventBus}
   * @param proxyType a Class instance of type P
   * @param handler an {@link EntityProxyChange.Handler} instance of type P
   * @return an {@link EntityProxy} instance
   */
  public static <P extends EntityProxy> HandlerRegistration registerForProxyType(EventBus eventBus,
      Class<P> proxyType, EntityProxyChange.Handler<P> handler) {
    return eventBus.addHandlerToSource(TYPE, proxyType, handler);
  }

  private P proxy;

  private WriteOperation writeOperation;

  /**
   * Constructs an EntityProxyChange object.
   * 
   * @param proxy an {@link EntityProxy} instance of type P
   * @param writeOperation a {@link WriteOperation} instance
   */
  public EntityProxyChange(P proxy, WriteOperation writeOperation) {
    this.proxy = proxy;
    this.writeOperation = writeOperation;
  }

  /**
   * Returns the type associated with this instance.
   * 
   * @return an instance of {@link Event.Type Type} of type Handler&lt;P&gt
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Event.Type<Handler<P>> getAssociatedType() {
    /*
     * The instance knows its handler is of type P, but the TYPE field itself
     * does not, so we have to do an unsafe cast here.
     */

    return (Type) TYPE;
  }

  /**
   * Returns an unpopulated copy of the changed proxy &mdash; all properties are
   * undefined except its id.
   * 
   * @return an instance of {@link EntityProxyId}&lt;P&gt;
   */
  @SuppressWarnings("unchecked")
  public EntityProxyId<P> getProxyId() {
    return (EntityProxyId<P>) proxy.stableId();
  }

  /**
   * Returns the {@link WriteOperation} associated with this instance.
   * 
   * @return a {@link WriteOperation} instance
   */
  public WriteOperation getWriteOperation() {
    return writeOperation;
  }

  @Override
  protected void dispatch(Handler<P> handler) {
    handler.onProxyChange(this);
  }
}
