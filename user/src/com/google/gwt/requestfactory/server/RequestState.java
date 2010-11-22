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
package com.google.gwt.requestfactory.server;

import com.google.gwt.autobean.server.AutoBeanFactoryMagic;
import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanCodex;
import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.ValueCodex;
import com.google.gwt.autobean.shared.impl.StringQuoter;
import com.google.gwt.requestfactory.server.SimpleRequestProcessor.IdToEntityMap;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.ValueProxy;
import com.google.gwt.requestfactory.shared.impl.Constants;
import com.google.gwt.requestfactory.shared.impl.EntityCodex;
import com.google.gwt.requestfactory.shared.impl.IdFactory;
import com.google.gwt.requestfactory.shared.impl.MessageFactoryHolder;
import com.google.gwt.requestfactory.shared.impl.SimpleProxyId;
import com.google.gwt.requestfactory.shared.messages.IdMessage;
import com.google.gwt.requestfactory.shared.messages.IdMessage.Strength;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Encapsulates all state relating to the processing of a single request so that
 * the SimpleRequestProcessor can be stateless.
 */
class RequestState implements EntityCodex.EntitySource {
  final IdToEntityMap beans = new IdToEntityMap();
  private final Map<Object, SimpleProxyId<?>> domainObjectsToId;
  private final IdFactory idFactory;
  private final ServiceLayer service;
  private final Resolver resolver;

  public RequestState(RequestState parent) {
    idFactory = parent.idFactory;
    domainObjectsToId = parent.domainObjectsToId;
    service = parent.service;
    resolver = new Resolver(this);
  }

  public RequestState(final ServiceLayer service) {
    this.service = service;
    idFactory = new IdFactory() {
      @Override
      public boolean isEntityType(Class<?> clazz) {
        return EntityProxy.class.isAssignableFrom(clazz);
      }

      @Override
      public boolean isValueType(Class<?> clazz) {
        return ValueProxy.class.isAssignableFrom(clazz);
      }

      @Override
      @SuppressWarnings("unchecked")
      protected <P extends BaseProxy> Class<P> getTypeFromToken(String typeToken) {
        return (Class<P>) service.resolveClass(typeToken);
      }

      @Override
      protected String getTypeToken(Class<? extends BaseProxy> clazz) {
        return service.resolveTypeToken(clazz);
      }
    };
    domainObjectsToId = new IdentityHashMap<Object, SimpleProxyId<?>>();
    resolver = new Resolver(this);
  }

  public Splittable flatten(Object domainValue) {
    Splittable flatValue;
    if (ValueCodex.canDecode(domainValue.getClass())) {
      flatValue = ValueCodex.encode(domainValue);
    } else {
      flatValue = new SimpleRequestProcessor(service).createOobMessage(Collections.singletonList(domainValue));
    }
    return flatValue;
  }

  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(IdMessage idMessage) {
    SimpleProxyId<Q> id;
    if (Strength.SYNTHETIC.equals(idMessage.getStrength())) {
      @SuppressWarnings("unchecked")
      Class<Q> clazz = (Class<Q>) service.resolveClass(idMessage.getTypeToken());
      id = idFactory.allocateSyntheticId(clazz, idMessage.getSyntheticId());
    } else {
      String decodedId = idMessage.getServerId() == null ? null
          : SimpleRequestProcessor.fromBase64(idMessage.getServerId());
      id = idFactory.getId(idMessage.getTypeToken(), decodedId,
          idMessage.getClientId());
    }
    return getBeanForPayload(id);
  }

  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(
      SimpleProxyId<Q> id, Object domainObject) {
    @SuppressWarnings("unchecked")
    AutoBean<Q> toReturn = (AutoBean<Q>) beans.get(id);
    if (toReturn == null) {
      toReturn = createEntityProxyBean(id, domainObject);
    }
    return toReturn;
  }

  /**
   * EntityCodex support.
   */
  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(
      Splittable serializedProxyId) {
    IdMessage idMessage = AutoBeanCodex.decode(MessageFactoryHolder.FACTORY,
        IdMessage.class, serializedProxyId).as();
    return getBeanForPayload(idMessage);
  }

  public IdFactory getIdFactory() {
    return idFactory;
  }

  public Resolver getResolver() {
    return resolver;
  }

  /**
   * EntityCodex support. This method is identical to
   * {@link IdFactory#getHistoryToken(SimpleProxyId)} except that it
   * base64-encodes the server ids.
   * <p>
   * XXX: Merge this with AbstsractRequestContext's implementation
   */
  public Splittable getSerializedProxyId(SimpleProxyId<?> stableId) {
    AutoBean<IdMessage> bean = MessageFactoryHolder.FACTORY.id();
    IdMessage ref = bean.as();
    ref.setTypeToken(service.resolveTypeToken(stableId.getProxyClass()));
    if (stableId.isSynthetic()) {
      ref.setStrength(Strength.SYNTHETIC);
      ref.setSyntheticId(stableId.getSyntheticId());
    } else if (stableId.isEphemeral()) {
      ref.setStrength(Strength.EPHEMERAL);
      ref.setClientId(stableId.getClientId());
    } else {
      ref.setServerId(SimpleRequestProcessor.toBase64(stableId.getServerId()));
    }
    return AutoBeanCodex.encode(bean);
  }

  public ServiceLayer getServiceLayer() {
    return service;
  }

  /**
   * If the given domain object has been previously associated with an id,
   * return it
   */
  public SimpleProxyId<?> getStableId(Object domain) {
    return domainObjectsToId.get(domain);
  }

  /**
   * EntityCodex support.
   */
  public boolean isEntityType(Class<?> clazz) {
    return idFactory.isEntityType(clazz);
  }

  /**
   * EntityCodex support.
   */
  public boolean isValueType(Class<?> clazz) {
    return idFactory.isValueType(clazz);
  }

  /**
   * Creates an AutoBean for the given id, tracking a domain object.
   */
  private <Q extends BaseProxy> AutoBean<Q> createEntityProxyBean(
      SimpleProxyId<Q> id, Object domainObject) {
    AutoBean<Q> toReturn = AutoBeanFactoryMagic.createBean(id.getProxyClass(),
        SimpleRequestProcessor.CONFIGURATION);
    toReturn.setTag(Constants.STABLE_ID, id);
    toReturn.setTag(Constants.DOMAIN_OBJECT, domainObject);
    beans.put(id, toReturn);
    return toReturn;
  }

  /**
   * Returns the AutoBean corresponding to the given id, or creates if it does
   * not yet exist.
   */
  private <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(
      SimpleProxyId<Q> id) {
    @SuppressWarnings("unchecked")
    AutoBean<Q> toReturn = (AutoBean<Q>) beans.get(id);
    if (toReturn == null) {
      // Resolve the domain object
      Class<?> domainClass = service.resolveDomainClass(id.getProxyClass());
      Object domain;
      if (id.isEphemeral() || id.isSynthetic()) {
        domain = service.createDomainObject(domainClass);
        if (domain == null) {
          throw new UnexpectedException("Could not create instance of "
              + domainClass.getCanonicalName(), null);
        }
      } else {
        Splittable address = StringQuoter.split(id.getServerId());
        Class<?> param = service.getIdType(domainClass);
        Object domainParam;
        if (ValueCodex.canDecode(param)) {
          domainParam = ValueCodex.decode(param, address);
        } else {
          domainParam = new SimpleRequestProcessor(service).decodeOobMessage(
              param, address).get(0);
        }
        domain = service.loadDomainObject(domainClass, domainParam);
      }
      domainObjectsToId.put(domain, id);
      toReturn = createEntityProxyBean(id, domain);
    }
    return toReturn;
  }
}