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

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestTransport;
import com.google.gwt.requestfactory.shared.messages.IdUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 */
public abstract class AbstractRequestFactory extends IdFactory implements
    RequestFactory {
  private static final int MAX_VERSION_ENTRIES = 10000;

  private EventBus eventBus;

  @SuppressWarnings("serial")
  private final Map<String, Integer> version = new LinkedHashMap<String, Integer>(
      16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Entry<String, Integer> eldest) {
      return size() > MAX_VERSION_ENTRIES;
    }
  };
  private RequestTransport transport;

  /**
   * Creates a new EntityProxy with an assigned ID.
   */
  public <T extends EntityProxy> AutoBean<T> createEntityProxy(Class<T> clazz,
      SimpleEntityProxyId<T> id) {
    AutoBean<T> created = getAutoBeanFactory().create(clazz);
    if (created == null) {
      throw new IllegalArgumentException("Unknown EntityProxy type "
          + clazz.getName());
    }
    created.setTag(EntityProxyCategory.REQUEST_FACTORY, this);
    created.setTag(EntityProxyCategory.STABLE_ID, id);
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
            new Object[]{proxyId}, propertyRefs, proxyId.getProxyClass(), null);
      }
    };
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public Class<? extends EntityProxy> getProxyClass(String historyToken) {
    String typeToken = IdUtil.getTypeToken(historyToken);
    if (typeToken != null) {
      return getTypeFromToken(typeToken);
    }
    return getTypeFromToken(historyToken);
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
  protected boolean hasVersionChanged(SimpleEntityProxyId<?> id,
      int observedVersion) {
    String key = getHistoryToken(id);
    Integer existingVersion = version.get(key);
    // Return true if we haven't seen this before or the versions differ
    boolean toReturn = existingVersion == null
        || !existingVersion.equals(observedVersion);
    if (toReturn) {
      version.put(key, observedVersion);
    }
    return toReturn;
  }
}
