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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.ProxySerializer;
import com.google.web.bindery.requestfactory.shared.ProxyStore;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.RequestTransport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base type for generated RF interfaces.
 */
public abstract class AbstractRequestFactory extends IdFactory implements RequestFactory {
  private static final int MAX_VERSION_ENTRIES = 10000;

  private EventBus eventBus;

  @SuppressWarnings("serial")
  private final Map<String, String> version = new LinkedHashMap<String, String>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Entry<String, String> eldest) {
      return size() > MAX_VERSION_ENTRIES;
    }
  };
  private RequestTransport transport;

  public <P extends EntityProxy> Request<P> find(EntityProxyId<P> proxyId) {
    if (((SimpleEntityProxyId<P>) proxyId).isEphemeral()) {
      throw new IllegalArgumentException("Cannot fetch unpersisted entity");
    }

    AbstractRequestContext context =
        new AbstractRequestContext(AbstractRequestFactory.this,
            AbstractRequestContext.Dialect.STANDARD) {
          @Override
          protected AutoBeanFactory getAutoBeanFactory() {
            return AbstractRequestFactory.this.getAutoBeanFactory();
          }
        };
    return context.find(proxyId);
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  /**
   * Returns a type token for the RequestFactory instance, which is used to seed
   * operation and type token resolution on the server.
   */
  public abstract String getFactoryTypeToken();

  public String getHistoryToken(Class<? extends EntityProxy> clazz) {
    return getTypeToken(clazz);
  }

  public String getHistoryToken(EntityProxyId<?> proxy) {
    return getHistoryToken((SimpleProxyId<?>) proxy);
  }

  public Class<? extends EntityProxy> getProxyClass(String historyToken) {
    String typeToken = IdUtil.getTypeToken(historyToken);
    if (typeToken != null) {
      return getTypeFromToken(typeToken);
    }
    return getTypeFromToken(historyToken);
  }

  @SuppressWarnings("unchecked")
  public <T extends EntityProxy> EntityProxyId<T> getProxyId(String historyToken) {
    return (EntityProxyId<T>) getBaseProxyId(historyToken);
  }

  public RequestTransport getRequestTransport() {
    return transport;
  }

  public ProxySerializer getSerializer(ProxyStore store) {
    return new ProxySerializerImpl(this, store);
  }

  /**
   * The choice of a default request transport is runtime-specific.
   */
  public abstract void initialize(EventBus eventBus);

  public void initialize(EventBus eventBus, RequestTransport transport) {
    this.eventBus = eventBus;
    this.transport = transport;
  }

  /**
   * Implementations of EntityProxies are provided by an AutoBeanFactory, which
   * is itself a generated type. This method knows about all proxy types used in
   * the RequestFactory interface, which prevents pruning of any proxy type. If
   * the {@link #find(EntityProxyId)} and {@link #getSerializer(ProxyStore)}
   * were provided by {@link AbstractRequestContext}, this method could be
   * removed.
   */
  protected abstract AutoBeanFactory getAutoBeanFactory();

  /**
   * Used by {@link AbstractRequestContext} to quiesce update events for objects
   * that haven't truly changed.
   */
  protected boolean hasVersionChanged(SimpleProxyId<?> id, String observedVersion) {
    assert id != null : "id";
    assert observedVersion != null : "observedVersion";
    String key = getHistoryToken(id);
    String existingVersion = version.get(key);
    // Return true if we haven't seen this before or the versions differ
    boolean toReturn = existingVersion == null || !existingVersion.equals(observedVersion);
    if (toReturn) {
      version.put(key, observedVersion);
    }
    return toReturn;
  }
}
