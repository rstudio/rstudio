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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract base class for an event announcing changes to a {@link EntityProxy}.
 * <p>
 * Note that this event includes an unpopulated copy of the changed proxy
 * &mdash; all properties are undefined except it's id. That is, the event
 * includes only enough information for receivers to issue requests to get
 * themselves fresh copies of the proxy.
 * <p>
 * TODO: use ProxyId rather than an empty proxy
 * 
 * @param <P> the type of the proxy
 */
public class EntityProxyChange<P extends EntityProxy> extends
    GwtEvent<EntityProxyChange.Handler<P>> {
  
  /**
   * Implemented by methods that handle EntityProxyChange events.
   * @param <P> the proxy type
   */
  public interface Handler<P extends EntityProxy> extends EventHandler {
    void onProxyChange(EntityProxyChange<P> event);
  }

  private static final Type<EntityProxyChange.Handler<?>> TYPE = new Type<EntityProxyChange.Handler<?>>();

  /**
   * Register a handler for a EntityProxyChange events for a particular proxy
   * class.
   */
  public static <P extends EntityProxy> HandlerRegistration registerForProxyType(
      EventBus eventBus, Class<P> proxyType,
      EntityProxyChange.Handler<P> handler) {
    return eventBus.addHandlerToSource(TYPE, proxyType, handler);
  }

  private P proxy;

  private WriteOperation writeOperation;

  public EntityProxyChange(P proxy, WriteOperation writeOperation) {
    this.proxy = proxy;
    this.writeOperation = writeOperation;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public GwtEvent.Type<Handler<P>> getAssociatedType() {
    /*
     * The instance knows its handler is of type P, but the TYPE field itself
     * does not, so we have to do an unsafe cast here.
     */

    return (Type) TYPE;
  }

  /**
   * @return an unpopulated copy of the changed proxy &mdash; all properties are
   *         undefined except its id
   */
  public P getProxy() {
    return proxy;
  }

  public WriteOperation getWriteOperation() {
    return writeOperation;
  }

  @Override
  protected void dispatch(Handler<P> handler) {
    handler.onProxyChange(this);
  }
}
