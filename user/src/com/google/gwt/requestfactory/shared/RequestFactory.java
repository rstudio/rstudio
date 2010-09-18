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

  // TODO: this must be configurable
  String URL = "gwtRequest";

  <R extends EntityProxy> R create(Class<R> token);

  /**
   * Provide a general purpose find request.
   */
  ProxyRequest<EntityProxy> find(EntityProxyId proxyId);

  /**
   * Return the class object which may be used to create new instances of the
   * type of the given proxy, via {@link #create}. Due to limitations of GWT's
   * metadata system, calls to the proxy's getClass() method will not serve this
   * purpose.
   */
  Class<? extends EntityProxy> getClass(EntityProxyId proxyId);

  /**
   * Return the class object which may be used to create new instances of the
   * type of this token, via {@link #create}. The token may represent either a
   * proxy instance (see {@link #getHistoryToken(Proxy)) or a proxy class (see
   * @link #getToken(Class)}).
   */
  Class<? extends EntityProxy> getClass(String token);

  /**
   * Get a {@link com.google.gwt.user.client.History} compatible token that
   * represents the given proxy. It can be processed by
   * {@link #getProxyId(String)} and {@link #getClass(String)}.
   *
   * @return a {@link com.google.gwt.user.client.History} compatible token
   */
  String getHistoryToken(EntityProxyId proxy);

  /**
   * Return the appropriate {@link EntityProxyId}, a stable id for the Proxy.
   */
  EntityProxyId getProxyId(String token);

  /**
   * Get a {@link com.google.gwt.user.client.History} compatible token that
   * represents the given class. It can be processed by
   * {@link #getClass(String)}
   *
   * @return a {@link com.google.gwt.user.client.History} compatible token
   */
  String getToken(Class<? extends EntityProxy> clazz);

  /**
   * Start this request factory with a
   * {@link com.google.gwt.requestfactory.client.DefaultRequestTransport}.
   */
  void init(EventBus eventBus);

  /**
   * Start this request factory with a user-provided transport.
   */
  void init(EventBus eventBus, RequestTransport transport);
}
