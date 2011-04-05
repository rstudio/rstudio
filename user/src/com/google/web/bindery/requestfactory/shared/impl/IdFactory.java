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

import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles common code for creating SimpleProxyIds.
 */
public abstract class IdFactory {
  /**
   * Maps ephemeral history tokens to an id object. This canonicalizing mapping
   * resolves the problem of EntityProxyIds hashcodes changing after persist.
   * Only ids that are created in the RequestFactory are stored here.
   */
  private final Map<String, SimpleProxyId<?>> ephemeralIds = new HashMap<String, SimpleProxyId<?>>();

  /**
   * Allocates an ephemeral proxy id. This object is only valid for the lifetime
   * of the RequestFactory.
   */
  public <P extends BaseProxy> SimpleProxyId<P> allocateId(Class<P> clazz) {
    SimpleProxyId<P> toReturn = createId(clazz, ephemeralIds.size() + 1);
    ephemeralIds.put(getHistoryToken(toReturn), toReturn);
    return toReturn;
  }

  /**
   * Allocates a synthetic proxy id. This object is only valid for the lifetime
   * of a request.
   */
  public <P extends BaseProxy> SimpleProxyId<P> allocateSyntheticId(
      Class<P> clazz, int syntheticId) {
    assert syntheticId > 0;
    SimpleProxyId<P> toReturn = createId(clazz, "%" + syntheticId);
    toReturn.setSyntheticId(syntheticId);
    return toReturn;
  }

  /**
   * A utility function to handle generic type conversion. This method will also
   * assert that {@code clazz} is actually an EntityProxy type.
   */
  @SuppressWarnings("unchecked")
  public <P extends EntityProxy> Class<P> asEntityProxy(
      Class<? extends BaseProxy> clazz) {
    assert isEntityType(clazz) : clazz.getName()
        + " is not an EntityProxy type";
    return (Class<P>) clazz;
  }

  /**
   * A utility function to handle generic type conversion. This method will also
   * assert that {@code clazz} is actually a ValueProxy type.
   */
  @SuppressWarnings("unchecked")
  public <P extends ValueProxy> Class<P> asValueProxy(
      Class<? extends BaseProxy> clazz) {
    assert isValueType(clazz) : clazz.getName() + " is not a ValueProxy type";
    return (Class<P>) clazz;
  }

  public <P extends BaseProxy> SimpleProxyId<P> getBaseProxyId(
      String historyToken) {
    assert !IdUtil.isSynthetic(historyToken) : "Synthetic id resolution"
        + " should be handled by AbstractRequestContext";
    if (IdUtil.isPersisted(historyToken)) {
      return getId(IdUtil.getTypeToken(historyToken),
          IdUtil.getServerId(historyToken));
    }
    if (IdUtil.isEphemeral(historyToken)) {
      @SuppressWarnings("unchecked")
      SimpleProxyId<P> toReturn = (SimpleProxyId<P>) ephemeralIds.get(historyToken);

      /*
       * This is tested in FindServiceTest.testFetchUnpersistedFutureId. In
       * order to get here, the user would have to get an unpersisted history
       * token and attempt to use it with a different RequestFactory instance.
       * This could occur if an ephemeral token were bookmarked. In this case,
       * we'll create a token, however it will never match anything.
       */
      if (toReturn == null) {
        Class<P> clazz = checkTypeToken(IdUtil.getTypeToken(historyToken));
        toReturn = createId(clazz, -1 * ephemeralIds.size());
        ephemeralIds.put(historyToken, toReturn);
      }

      return toReturn;
    }
    throw new IllegalArgumentException(historyToken);
  }

  public String getHistoryToken(SimpleProxyId<?> proxy) {
    SimpleProxyId<?> id = (SimpleProxyId<?>) proxy;
    String token = getTypeToken(proxy.getProxyClass());
    if (id.isEphemeral()) {
      return IdUtil.ephemeralId(id.getClientId(), token);
    } else if (id.isSynthetic()) {
      return IdUtil.syntheticId(id.getSyntheticId(), token);
    } else {
      return IdUtil.persistedId(id.getServerId(), token);
    }
  }

  /**
   * Create or retrieve a SimpleProxyId. If both the serverId and clientId are
   * specified and the id is ephemeral, it will be updated with the server id.
   */
  public <P extends BaseProxy> SimpleProxyId<P> getId(Class<P> clazz,
      String serverId, int clientId) {
    return getId(getTypeToken(clazz), serverId, clientId);
  }

  /**
   * Create or retrieve a SimpleProxyId.
   */
  public <P extends BaseProxy> SimpleProxyId<P> getId(String typeToken,
      String serverId) {
    return getId(typeToken, serverId, 0);
  }

  /**
   * Create or retrieve a SimpleEntityProxyId. If both the serverId and clientId
   * are specified and the id is ephemeral, it will be updated with the server
   * id.
   */
  public <P extends BaseProxy> SimpleProxyId<P> getId(String typeToken,
      String serverId, int clientId) {
    /*
     * If there's a clientId, that probably means we've just created a brand-new
     * EntityProxy or have just persisted something on the server.
     */
    if (clientId > 0) {
      // Try a cache lookup for the ephemeral key
      String ephemeralKey = IdUtil.ephemeralId(clientId, typeToken);
      @SuppressWarnings("unchecked")
      SimpleProxyId<P> toReturn = (SimpleProxyId<P>) ephemeralIds.get(ephemeralKey);

      // Do we need to allocate an ephemeral id?
      if (toReturn == null) {
        Class<P> clazz = getTypeFromToken(typeToken);
        toReturn = createId(clazz, clientId);
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
    SimpleProxyId<P> toReturn = (SimpleProxyId<P>) ephemeralIds.get(serverKey);
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
    assert clazz != null : "No class literal for " + typeToken;
    return createId(clazz, serverId);
  }

  public abstract boolean isEntityType(Class<?> clazz);

  public abstract boolean isValueType(Class<?> clazz);

  protected abstract <P extends BaseProxy> Class<P> getTypeFromToken(
      String typeToken);

  protected abstract String getTypeToken(Class<? extends BaseProxy> clazz);

  private <P> Class<P> checkTypeToken(String token) {
    @SuppressWarnings("unchecked")
    Class<P> clazz = (Class<P>) getTypeFromToken(token);
    if (clazz == null) {
      throw new IllegalArgumentException("Unknnown type");
    }
    return clazz;
  }

  private <P extends BaseProxy> SimpleProxyId<P> createId(Class<P> clazz,
      int clientId) {
    SimpleProxyId<P> toReturn;
    if (isValueType(clazz)) {
      toReturn = new SimpleProxyId<P>(clazz, clientId);
    } else {
      @SuppressWarnings("unchecked")
      SimpleProxyId<P> temp = (SimpleProxyId<P>) new SimpleEntityProxyId<EntityProxy>(
          asEntityProxy(clazz), clientId);
      toReturn = (SimpleProxyId<P>) temp;
    }
    return toReturn;
  }

  private <P extends BaseProxy> SimpleProxyId<P> createId(Class<P> clazz,
      String serverId) {
    SimpleProxyId<P> toReturn;
    if (isValueType(clazz)) {
      toReturn = new SimpleProxyId<P>(clazz, serverId);
    } else {
      @SuppressWarnings("unchecked")
      SimpleProxyId<P> temp = (SimpleProxyId<P>) new SimpleEntityProxyId<EntityProxy>(
          asEntityProxy(clazz), serverId);
      toReturn = (SimpleProxyId<P>) temp;
    }
    return toReturn;
  }

}