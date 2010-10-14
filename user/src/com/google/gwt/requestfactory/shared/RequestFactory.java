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

/**
 * Marker interface for the RequestFactory code generator.
 * <p>
 * A RequestFactory implementation will post {@link EntityProxyChange} events to
 * the {@link EventBus} passed into the {@link #initialize} method. The events
 * will have the following {@link WriteOperation} associated with them in the
 * following circumstances:
 * <ul>
 * <li>{@link WriteOperation#PERSIST} when an {@link EntityProxy}
 * {@link RequestContext#create(Class) created} on the client is successfully
 * persisted in the server's backing store.</li>
 * <li>{@link WriteOperation#UPDATE} when changes due to an {@link EntityProxy}
 * being {@link RequestContext#edit(EntityProxy) edited} on the client are
 * successfully persisted in the server's backing store.</li>
 * <li>{@link WriteOperation#UPDATE} when a previously-unseen
 * {@link EntityProxy} is reachable from the return value for a
 * successfully-executed {@link Request}.</li>
 * <li>{@link WriteOperation#UPDATE} when any property of an {@link EntityProxy}
 * reachable from a {@link Request Request's} arguments is seen to have changed
 * after executing the service method on the server.</li>
 * <li>{@link WriteOperation#DELETE} when an {@link EntityProxy} reachable from
 * a {@link Request Request's} arguments becomes irretrievable after executing
 * the service method on the server.</li>
 * </ul>
 * <p>
 * Other types of events may be posted to the {@link EventBus} by other services
 * used by the RequestFactory.
 * 
 * @see {@link com.google.gwt.requestfactory.client.DefaultRequestTransport}
 */
public interface RequestFactory {
  String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=utf-8";

  /**
   * Return a request to find a fresh instance of the referenced proxy.
   */
  <P extends EntityProxy> Request<P> find(EntityProxyId<P> proxyId);

  /**
   * Returns the eventbus this factory's events are posted on, which was set via
   * {@link #initialize}.
   */
  EventBus getEventBus();

  /**
   * Get a {@link com.google.gwt.user.client.History} compatible token that
   * represents the given class. It can be processed by
   * {@link #getProxyClass(String)}
   * 
   * @return a {@link com.google.gwt.user.client.History} compatible token
   */
  String getHistoryToken(Class<? extends EntityProxy> clazz);

  /**
   * Get a {@link com.google.gwt.user.client.History} compatible token that
   * represents the given proxy class. It can be processed by
   * {@link #getProxyClass(String)}.
   * <p>
   * The history token returned for an EntityProxyId associated with a
   * newly-created (future) EntityProxy will differ from the token returned by
   * this method after the EntityProxy has been persisted. Once an EntityProxy
   * has been persisted, the return value for this method will always be stable,
   * regardless of when the EntityProxyId was retrieved relative to the persist
   * operation. In other words, the "future" history token returned for an
   * as-yet-unpersisted EntityProxy is only valid for the duration of the
   * RequestFactory's lifespan.
   * 
   * @return a {@link com.google.gwt.user.client.History} compatible token
   */
  String getHistoryToken(EntityProxyId<?> proxy);

  /**
   * Return the class object which may be used to create new instances of the
   * type of this token, via {@link RequestContext#create}. The token may
   * represent either a proxy instance (see {@link #getHistoryToken}) or a proxy
   * class (see {@link #getProxyClass}).
   */
  Class<? extends EntityProxy> getProxyClass(String historyToken);

  /**
   * Return the appropriate {@link EntityProxyId} using a string returned from
   * {@link #getHistoryToken(EntityProxyId)}.
   */
  <T extends EntityProxy> EntityProxyId<T> getProxyId(String historyToken);

  /**
   * Start this request factory with a
   * {@link com.google.gwt.requestfactory.client.DefaultRequestTransport}.
   */
  void initialize(EventBus eventBus);

  /**
   * Start this request factory with a user-provided transport.
   */
  void initialize(EventBus eventBus, RequestTransport transport);
}
