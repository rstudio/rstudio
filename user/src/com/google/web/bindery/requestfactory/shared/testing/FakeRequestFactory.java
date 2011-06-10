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
package com.google.web.bindery.requestfactory.shared.testing;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.ProxySerializer;
import com.google.web.bindery.requestfactory.shared.ProxyStore;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.RequestTransport;

/**
 * A no-op implementation of {@link RequestFactory} that can be used for
 * building mocks.
 */
public class FakeRequestFactory implements RequestFactory {

  private EventBus eventBus;
  private RequestTransport requestTransport;

  /**
   * Returns {@code null}.
   */
  @Override
  public <P extends EntityProxy> Request<P> find(EntityProxyId<P> proxyId) {
    return null;
  }

  /**
   * Returns the last value passed to
   * {@link #initialize(EventBus, RequestTransport)}.
   */
  @Override
  public EventBus getEventBus() {
    return eventBus;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public String getHistoryToken(Class<? extends EntityProxy> clazz) {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public String getHistoryToken(EntityProxyId<?> proxy) {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public Class<? extends EntityProxy> getProxyClass(String historyToken) {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public <T extends EntityProxy> EntityProxyId<T> getProxyId(String historyToken) {
    return null;
  }

  /**
   * Returns the last value passed to
   * {@link #initialize(EventBus, RequestTransport)}.
   */
  @Override
  public RequestTransport getRequestTransport() {
    return requestTransport;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public ProxySerializer getSerializer(ProxyStore store) {
    return null;
  }

  /**
   * Equivalent to {@code initialize(eventBus, new FakeRequestTransport())}.
   */
  @Override
  public void initialize(EventBus eventBus) {
    initialize(eventBus, new FakeRequestTransport());
  }

  /**
   * Saves the parameters for later retrieval.
   */
  @Override
  public void initialize(EventBus eventBus, RequestTransport transport) {
    this.eventBus = eventBus;
    this.requestTransport = transport;
  }
}
