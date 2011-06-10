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

import com.google.web.bindery.event.shared.EventBus;

/**
 * Marker interface for the RequestFactory code generator.
 * <p>
 * <b>Limitations on the transferrability of types.</b> <br>
 * RequestFactory currently supports the transfer of basic value types, entity
 * types, and collections, with limitations. The basic value types supported are
 * {@link String}, {@link Enum}, {@link Boolean}, {@link Character}, subtypes of
 * {@link Number}, and {@link java.util.Date}. Any value type not included in
 * this list may not be declared in the type signature of a service method, or
 * {@link EntityProxy}. {@link java.util.Collection} types supported are
 * {@link java.util.List} and {@link java.util.Set} with the restriction that a
 * collection must be homogeneous and only hold one type of value.
 * </p>
 * <p>
 * Polymorphism is not supported at this time. RequestFactory encoding and
 * decoding requires exact knowledge of the concrete type. If a method declares
 * a given type <code>T</code> as a parameter or return type, only
 * <code>T</code>'s transferrable properties will be sent over the wire if it is
 * a proxy, even if the underlying domain value contains extra fields, in
 * effect, treating it as an instance of the supertype. Returning abstract
 * supertypes of value types is not supported (e.g. Object, Enum, Number).
 * </p>
 * 
 * @see com.google.web.bindery.requestfactory.shared.testing.FakeRequestFactory
 * @see com.google.web.bindery.requestfactory.server.testing.InProcessRequestTransport
 */
public interface RequestFactory {
  /**
   * The JSON content type String.
   */
  String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=utf-8";

  /**
   * Return a request to find a fresh instance of the referenced proxy. If it is
   * necessary to load several proxy instances, use
   * {@link RequestContext#find(EntityProxyId)}, which supports chained
   * requests.
   * 
   * @param proxyId an {@link EntityProxyId} instance of type P
   * @return a {@link Request} object
   * @see RequestContext#find(EntityProxyId)
   */
  <P extends EntityProxy> Request<P> find(EntityProxyId<P> proxyId);

  /**
   * Returns the event bus this factory's events are posted on, which was set
   * via {@link #initialize}.
   * 
   * @return the {@link EventBus} associated with this instance
   */
  EventBus getEventBus();

  /**
   * Get a {@link com.google.gwt.user.client.History} compatible token that
   * represents the given class. It can be processed by
   * {@link #getProxyClass(String)}
   * 
   * @param clazz a Class object for an {@link EntityProxy} subclass
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
   * @param proxy an {@link EntityProxyId} instance
   * @return a {@link com.google.gwt.user.client.History} compatible token
   */
  String getHistoryToken(EntityProxyId<?> proxy);

  /**
   * Return the class object which may be used to create new instances of the
   * type of this token, via {@link RequestContext#create}. The token may
   * represent either a proxy instance (see {@link #getHistoryToken}) or a proxy
   * class (see {@link #getProxyClass}).
   * 
   * @param historyToken a String token
   * @return a Class object for an {@link EntityProxy} subclass
   */
  Class<? extends EntityProxy> getProxyClass(String historyToken);

  /**
   * Return the appropriate {@link EntityProxyId} using a string returned from
   * {@link #getHistoryToken(EntityProxyId)}.
   * 
   * @param historyToken a String token
   * @return an {@link EntityProxyId}
   */
  <T extends EntityProxy> EntityProxyId<T> getProxyId(String historyToken);

  /**
   * Returns the RequestTransport set via {@link #initialize}.
   * 
   * @return the {@link RequestTransport} associated with this instance
   */
  RequestTransport getRequestTransport();

  /**
   * Returns a ProxySerializer that can encode and decode the various
   * EntityProxy and ValueProxy types reachable from the RequestFactory.
   * 
   * @param store a helper object for the ProxySerializer to provide low-level
   *          storage access
   * @return a new ProxySerializer
   * @see DefaultProxyStore
   */
  ProxySerializer getSerializer(ProxyStore store);

  /**
   * Start this request factory with a
   * {@link com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport}
   * .
   * 
   * @param eventBus an {@link EventBus}
   */
  void initialize(EventBus eventBus);

  /**
   * Start this request factory with a user-provided transport.
   * 
   * @param eventBus an {@link EventBus}
   * @param transport a {@link RequestTransport} instance
   */
  void initialize(EventBus eventBus, RequestTransport transport);
}
