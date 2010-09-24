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
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Marker interface for the RequestFactory code generator.
 */
public interface RequestFactory {
  String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=utf-8";

  <P extends EntityProxy> P create(Class<P> proxyType);

  /**
   * Return a request to find a fresh instance of the referenced proxy.
   */
  <P extends EntityProxy> ProxyRequest<P> find(EntityProxyId<P> proxyId);

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
   * type of this token, via {@link #create}. The token may represent either a
   * proxy instance (see {@link #getHistoryToken()}) or a proxy class (see
   * {@link #getToken()}).
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
