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
package com.google.gwt.requestfactory.shared.impl;

import static com.google.gwt.requestfactory.shared.impl.Constants.STABLE_ID;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestTransport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base type for generated RF interfaces.
 */
public abstract class AbstractRequestFactory extends IdFactory implements
    RequestFactory {
  private static final int MAX_VERSION_ENTRIES = 10000;

  private EventBus eventBus;

  @SuppressWarnings("serial")
  private final Map<String, String> version = new LinkedHashMap<String, String>(
      16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Entry<String, String> eldest) {
      return size() > MAX_VERSION_ENTRIES;
    }
  };
  private RequestTransport transport;

  /**
   * Creates a new proxy with an assigned ID.
   */
  public <T extends BaseProxy> AutoBean<T> createProxy(Class<T> clazz,
      SimpleProxyId<T> id) {
    AutoBean<T> created = getAutoBeanFactory().create(clazz);
    if (created == null) {
      throw new IllegalArgumentException("Unknown EntityProxy type "
          + clazz.getName());
    }
    created.setTag(STABLE_ID, id);
    return created;
  }

  public <P extends EntityProxy> Request<P> find(final EntityProxyId<P> proxyId) {
    if (((SimpleEntityProxyId<P>) proxyId).isEphemeral()) {
      throw new IllegalArgumentException("Cannot fetch unpersisted entity");
    }

    AbstractRequestContext context = new AbstractRequestContext(
        AbstractRequestFactory.this);
    return new AbstractRequest<P>(context) {
      {
        requestContext.addInvocation(this);
      }

      @Override
      protected RequestData makeRequestData() {
        return new RequestData(
            "com.google.gwt.requestfactory.shared.impl.FindRequest::find",
            new Object[] {proxyId}, propertyRefs, proxyId.getProxyClass(), null);
      }
    };
  }

  public EventBus getEventBus() {
    return eventBus;
  }

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
   * is itself a generated type.
   */
  protected abstract AutoBeanFactory getAutoBeanFactory();

  /**
   * Used by {@link AbstractRequestContext} to quiesce update events for objects
   * that haven't truly changed.
   */
  protected boolean hasVersionChanged(SimpleProxyId<?> id,
      String observedVersion) {
    String key = getHistoryToken(id);
    String existingVersion = version.get(key);
    // Return true if we haven't seen this before or the versions differ
    boolean toReturn = existingVersion == null
        || !existingVersion.equals(observedVersion);
    if (toReturn) {
      version.put(key, observedVersion);
    }
    return toReturn;
  }
}
