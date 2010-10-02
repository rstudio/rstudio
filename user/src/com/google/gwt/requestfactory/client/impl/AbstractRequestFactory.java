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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.editor.client.AutoBean;
import com.google.gwt.editor.client.AutoBeanFactory;
import com.google.gwt.editor.client.AutoBeanUtils;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.DefaultRequestTransport;
import com.google.gwt.requestfactory.client.impl.messages.RequestData;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestTransport;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 */
public abstract class AbstractRequestFactory implements RequestFactory {
  protected static final String EPHEMERAL_SEPARATOR = "@IS@";
  protected static final String TOKEN_SEPARATOR = "@NO@";
  protected static final int ID_TOKEN_INDEX = 0;
  protected static final int TYPE_TOKEN_INDEX = 1;
  private static final int MAX_VERSION_ENTRIES = 10000;

  public static String getHistoryToken(EntityProxy proxy) {
    AutoBean<EntityProxy> bean = AutoBeanUtils.getAutoBean(proxy);
    String historyToken = EntityProxyCategory.requestFactory(bean).getHistoryToken(
        proxy.stableId());
    return historyToken;
  }

  private EventBus eventBus;

  /**
   * Maps ephemeral history tokens to an id object. This canonicalizing mapping
   * resolves the problem of EntityProxyIds hashcodes changing after persist.
   * Only ids that are created in the RequestFactory are stored here.
   */
  /*
   * All of the ephemeral id-tracking could be moved into RequestContext if we
   * allowed ephemeral history tokens only to be used with the RequestConext
   * where the create method was called.
   */
  private final Map<String, SimpleEntityProxyId<?>> ephemeralIds = new HashMap<String, SimpleEntityProxyId<?>>();
  private final Map<String, Integer> version = new LinkedHashMap<String, Integer>(
      16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Entry<String, Integer> eldest) {
      return size() > MAX_VERSION_ENTRIES;
    }
  };
  private RequestTransport transport;

  /**
   * Allocates an ephemeral proxy id. This object is only valid for the lifetime
   * of the RequestFactory.
   */
  public <P extends EntityProxy> SimpleEntityProxyId<P> allocateId(
      Class<P> clazz) {
    SimpleEntityProxyId<P> toReturn = new SimpleEntityProxyId<P>(clazz,
        ephemeralIds.size() + 1);
    ephemeralIds.put(getHistoryToken(toReturn), toReturn);
    return toReturn;
  }

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

      protected void handleResult(Object result) {
        succeed(decodeReturnObject(proxyId.getProxyClass(), result));
      }

      @Override
      protected RequestData makeRequestData() {
        return new RequestData(
            "com.google.gwt.requestfactory.client.impl.FindRequest::find",
            new Object[] {getHistoryToken(proxyId)}, propertyRefs);
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
    SimpleEntityProxyId<?> id = (SimpleEntityProxyId<?>) proxy;
    if (id.isEphemeral()) {
      return id.getClientId() + EPHEMERAL_SEPARATOR
          + getHistoryToken(proxy.getProxyClass());
    } else {
      return id.getServerId() + TOKEN_SEPARATOR
          + getHistoryToken(proxy.getProxyClass());
    }
  }

  public Class<? extends EntityProxy> getProxyClass(String historyToken) {
    String[] parts = historyToken.split(TOKEN_SEPARATOR);
    if (parts.length == 2) {
      return getTypeFromToken(parts[TYPE_TOKEN_INDEX]);
    }
    parts = historyToken.split(EPHEMERAL_SEPARATOR);
    if (parts.length == 2) {
      return getTypeFromToken(parts[TYPE_TOKEN_INDEX]);
    }
    return getTypeFromToken(historyToken);
  }

  public <P extends EntityProxy> SimpleEntityProxyId<P> getProxyId(
      String historyToken) {
    String[] parts = historyToken.split(TOKEN_SEPARATOR);
    if (parts.length == 2) {
      return getId(parts[TYPE_TOKEN_INDEX], parts[ID_TOKEN_INDEX]);
    }
    parts = historyToken.split(EPHEMERAL_SEPARATOR);
    if (parts.length == 2) {
      @SuppressWarnings("unchecked")
      SimpleEntityProxyId<P> toReturn = (SimpleEntityProxyId<P>) ephemeralIds.get(historyToken);

      /*
       * This is tested in FindServiceTest.testFetchUnpersistedFutureId. In
       * order to get here, the user would have to get an unpersisted history
       * token and attempt to use it with a different RequestFactory instance.
       * This could occur if an ephemeral token was bookmarked. In this case,
       * we'll create a token, however it will never match anything.
       */
      if (toReturn == null) {
        Class<P> clazz = checkTypeToken(parts[TYPE_TOKEN_INDEX]);
        toReturn = new SimpleEntityProxyId<P>(clazz, -1 * ephemeralIds.size());
        ephemeralIds.put(historyToken, toReturn);
      }

      return toReturn;
    }
    throw new IllegalArgumentException(historyToken);
  }

  public RequestTransport getRequestTransport() {
    return transport;
  }

  public void initialize(EventBus eventBus) {
    initialize(eventBus, new DefaultRequestTransport(eventBus));
  }

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
   * Create or retrieve a SimpleEntityProxyId.
   */
  protected <P extends EntityProxy> SimpleEntityProxyId<P> getId(
      Class<P> clazz, String serverId) {
    return getId(getTypeToken(clazz), serverId);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId.
   */
  protected <P extends EntityProxy> SimpleEntityProxyId<P> getId(
      String typeToken, String serverId) {
    return getId(typeToken, serverId, null);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId. If both the serverId and clientId
   * are specified and the id is ephemeral, it will be updated with the server
   * id.
   */
  protected <P extends EntityProxy> SimpleEntityProxyId<P> getId(
      String typeToken, String serverId, String clientId) {
    /*
     * If there's a clientId, that probably means we've just created a brand-new
     * EntityProxy or have just persisted something on the server.
     */
    if (clientId != null) {
      // Try a cache lookup for the ephemeral key
      String ephemeralKey = clientId + EPHEMERAL_SEPARATOR + typeToken;
      @SuppressWarnings("unchecked")
      SimpleEntityProxyId<P> toReturn = (SimpleEntityProxyId<P>) ephemeralIds.get(ephemeralKey);
      // Cache hit based on client id
      if (toReturn != null) {
        // If it's ephemeral, see if we have a serverId and save it
        if (toReturn.isEphemeral()) {
          // Sanity check
          assert toReturn.getProxyClass().equals(getTypeFromToken(typeToken));

          // TODO: This happens when objects fail to validate on create
          if (!"null".equals(serverId)) {
            /*
             * Record the server id so a later "find" operation will have an
             * equal stableId.
             */
            toReturn.setServerId(serverId);
            String serverKey = serverId + TOKEN_SEPARATOR + typeToken;
            ephemeralIds.put(serverKey, toReturn);
          }
        }
        return toReturn;
      }
    }

    // Should never get this far without a server id
    assert serverId != null : "serverId";

    String serverKey = serverId + TOKEN_SEPARATOR + typeToken;
    @SuppressWarnings("unchecked")
    SimpleEntityProxyId<P> toReturn = (SimpleEntityProxyId<P>) ephemeralIds.get(serverKey);
    if (toReturn != null) {
      // A cache hit for a locally-created object that has been persisted
      return toReturn;
    }

    /*
     * No existing id, so it was never an ephemeral id created by this
     * RequestFactory, so we don't need to record it. This should be the normal
     * case for read-dominated applications.
     */
    Class<P> clazz = getTypeFromToken(typeToken);
    return new SimpleEntityProxyId<P>(clazz, serverId);
  }

  protected abstract <P extends EntityProxy> Class<P> getTypeFromToken(
      String typeToken);

  protected abstract String getTypeToken(Class<?> clazz);

  /**
   * Used by {@link AbstractRequestContext} to quiesce update events for objects
   * that haven't truly changed.
   */
  protected boolean hasVersionChanged(SimpleEntityProxyId<?> id,
      int observedVersion) {
    Integer existingVersion = version.get(id.getServerId());
    // Return true if we haven't seen this before or the versions differ
    boolean toReturn = existingVersion == null
        || !existingVersion.equals(observedVersion);
    if (toReturn) {
      version.put(id.getServerId(), observedVersion);
    }
    return toReturn;
  }

  private <P> Class<P> checkTypeToken(String token) {
    @SuppressWarnings("unchecked")
    Class<P> clazz = (Class<P>) getTypeFromToken(token);
    if (clazz == null) {
      throw new IllegalArgumentException("Unknnown type");
    }
    return clazz;
  }
}
