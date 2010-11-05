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

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.messages.IdUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles common code for creating SimpleEntityProxyIds.
 */
public abstract class IdFactory {
  /**
   * Maps ephemeral history tokens to an id object. This canonicalizing mapping
   * resolves the problem of EntityProxyIds hashcodes changing after persist.
   * Only ids that are created in the RequestFactory are stored here.
   */
  private final Map<String, SimpleEntityProxyId<?>> ephemeralIds = new HashMap<String, SimpleEntityProxyId<?>>();

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

  public String getHistoryToken(Class<? extends EntityProxy> clazz) {
    return getTypeToken(clazz);
  }

  public String getHistoryToken(EntityProxyId<?> proxy) {
    SimpleEntityProxyId<?> id = (SimpleEntityProxyId<?>) proxy;
    if (id.isEphemeral()) {
      return IdUtil.ephemeralId(id.getClientId(),
          getHistoryToken(proxy.getProxyClass()));
    } else {
      return IdUtil.persistedId(id.getServerId(),
          getHistoryToken(proxy.getProxyClass()));
    }
  }

  /**
   * Create or retrieve a SimpleEntityProxyId.
   */
  public <P extends EntityProxy> SimpleEntityProxyId<P> getId(Class<P> clazz,
      String serverId) {
    return getId(getTypeToken(clazz), serverId);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId. If both the serverId and clientId
   * are specified and the id is ephemeral, it will be updated with the server
   * id.
   */
  public <P extends EntityProxy> SimpleEntityProxyId<P> getId(Class<P> clazz,
      String serverId, Integer clientId) {
    return getId(getTypeToken(clazz), serverId, clientId);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId.
   */
  public <P extends EntityProxy> SimpleEntityProxyId<P> getId(String typeToken,
      String serverId) {
    return getId(typeToken, serverId, null);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId. If both the serverId and clientId
   * are specified and the id is ephemeral, it will be updated with the server
   * id.
   */
  public <P extends EntityProxy> SimpleEntityProxyId<P> getId(String typeToken,
      String serverId, Integer clientId) {
    /*
     * If there's a clientId, that probably means we've just created a brand-new
     * EntityProxy or have just persisted something on the server.
     */
    if (clientId != null) {
      // Try a cache lookup for the ephemeral key
      String ephemeralKey = IdUtil.ephemeralId(clientId, typeToken);
      @SuppressWarnings("unchecked")
      SimpleEntityProxyId<P> toReturn = (SimpleEntityProxyId<P>) ephemeralIds.get(ephemeralKey);

      // Do we need to allocate an ephemeral id?
      if (toReturn == null) {
        Class<P> clazz = getTypeFromToken(typeToken);
        toReturn = new SimpleEntityProxyId<P>(clazz, clientId);
        ephemeralIds.put(ephemeralKey, toReturn);
      }

      // If it's ephemeral, see if we have a serverId and save it
      if (toReturn.isEphemeral()) {
        // Sanity check
        assert toReturn.getProxyClass().equals(getTypeFromToken(typeToken));

        if (serverId != null) {
          /*
           * Record the server id so a later "find" operation will have an equal
           * stableId.
           */
          toReturn.setServerId(serverId);
          String serverKey = IdUtil.persistedId(serverId, typeToken);
          ephemeralIds.put(serverKey, toReturn);
        }
      }
      return toReturn;
    }

    // Should never get this far without a server id
    assert serverId != null : "serverId";

    String serverKey = IdUtil.persistedId(serverId, typeToken);
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

  public <P extends EntityProxy> SimpleEntityProxyId<P> getProxyId(
      String historyToken) {
    if (IdUtil.isPersisted(historyToken)) {
      return getId(IdUtil.getTypeToken(historyToken),
          IdUtil.getServerId(historyToken));
    }
    if (IdUtil.isEphemeral(historyToken)) {
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
        Class<P> clazz = checkTypeToken(IdUtil.getTypeToken(historyToken));
        toReturn = new SimpleEntityProxyId<P>(clazz, -1 * ephemeralIds.size());
        ephemeralIds.put(historyToken, toReturn);
      }

      return toReturn;
    }
    throw new IllegalArgumentException(historyToken);
  }

  protected abstract <P extends EntityProxy> Class<P> getTypeFromToken(
      String typeToken);

  protected abstract String getTypeToken(Class<?> clazz);

  private <P> Class<P> checkTypeToken(String token) {
    @SuppressWarnings("unchecked")
    Class<P> clazz = (Class<P>) getTypeFromToken(token);
    if (clazz == null) {
      throw new IllegalArgumentException("Unknnown type");
    }
    return clazz;
  }

}