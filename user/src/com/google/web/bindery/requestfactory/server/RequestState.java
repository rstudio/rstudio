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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor.IdToEntityMap;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.google.web.bindery.requestfactory.shared.impl.Constants;
import com.google.web.bindery.requestfactory.shared.impl.EntityCodex;
import com.google.web.bindery.requestfactory.shared.impl.IdFactory;
import com.google.web.bindery.requestfactory.shared.impl.MessageFactoryHolder;
import com.google.web.bindery.requestfactory.shared.impl.SimpleProxyId;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage.Strength;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates all state relating to the processing of a single request so that
 * the SimpleRequestProcessor can be stateless.
 */
class RequestState implements EntityCodex.EntitySource {
  final IdToEntityMap beans = new IdToEntityMap();
  private final IdentityHashMap<Object, SimpleProxyId<?>> domainObjectsToId;
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

  /**
   * Turn a domain value into a wire format message.
   */
  public Splittable flatten(Object domainValue) {
    Splittable flatValue;
    if (ValueCodex.canDecode(domainValue.getClass())) {
      flatValue = ValueCodex.encode(domainValue);
    } else {
      flatValue =
          new SimpleRequestProcessor(service).createOobMessage(Collections
              .singletonList(domainValue));
    }
    return flatValue;
  }

  /**
   * Get or create a BaseProxy AutoBean for the given id.
   */
  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(SimpleProxyId<Q> id,
      Object domainObject) {
    @SuppressWarnings("unchecked")
    AutoBean<Q> toReturn = (AutoBean<Q>) beans.get(id);
    if (toReturn == null) {
      toReturn = createProxyBean(id, domainObject);
    }
    return toReturn;
  }

  /**
   * EntityCodex support.
   */
  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(Splittable serializedProxyId) {
    IdMessage idMessage =
        AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, IdMessage.class, serializedProxyId).as();
    @SuppressWarnings("unchecked")
    AutoBean<Q> toReturn =
        (AutoBean<Q>) getBeansForPayload(Collections.singletonList(idMessage)).get(0);
    return toReturn;
  }

  /**
   * Get or create BaseProxy AutoBeans for a list of id-bearing messages.
   */
  public List<AutoBean<? extends BaseProxy>> getBeansForPayload(List<? extends IdMessage> idMessages) {
    List<SimpleProxyId<?>> ids = new ArrayList<SimpleProxyId<?>>(idMessages.size());
    for (IdMessage idMessage : idMessages) {
      SimpleProxyId<?> id;
      if (Strength.SYNTHETIC.equals(idMessage.getStrength())) {
        Class<? extends BaseProxy> clazz = service.resolveClass(idMessage.getTypeToken());
        id = idFactory.allocateSyntheticId(clazz, idMessage.getSyntheticId());
      } else {
        String decodedId =
            idMessage.getServerId() == null ? null : SimpleRequestProcessor.fromBase64(idMessage
                .getServerId());
        id = idFactory.getId(idMessage.getTypeToken(), decodedId, idMessage.getClientId());
      }
      ids.add(id);
    }
    return getBeansForIds(ids);
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
   * return it.
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
  private <Q extends BaseProxy> AutoBean<Q> createProxyBean(SimpleProxyId<Q> id, Object domainObject) {
    AutoBean<Q> toReturn =
        AutoBeanFactorySource.createBean(id.getProxyClass(), SimpleRequestProcessor.CONFIGURATION);
    toReturn.setTag(Constants.STABLE_ID, id);
    toReturn.setTag(Constants.DOMAIN_OBJECT, domainObject);
    beans.put(id, toReturn);
    return toReturn;
  }

  /**
   * Returns the AutoBeans corresponding to the given ids, or creates them if
   * they do not yet exist.
   */
  private List<AutoBean<? extends BaseProxy>> getBeansForIds(List<SimpleProxyId<?>> ids) {
    List<Class<?>> domainClasses = new ArrayList<Class<?>>(ids.size());
    List<Object> domainIds = new ArrayList<Object>(ids.size());
    List<SimpleProxyId<?>> idsToLoad = new ArrayList<SimpleProxyId<?>>();

    /*
     * Create proxies for ephemeral or synthetic ids that we haven't seen. Queue
     * up the domain ids for entities that need to be loaded.
     */
    for (SimpleProxyId<?> id : ids) {
      Class<?> domainClass = service.resolveDomainClass(id.getProxyClass());
      if (beans.containsKey(id)) {
        // Already have a proxy for this id, no-op
      } else if (id.isEphemeral() || id.isSynthetic()) {
        // Create a new domain object for the short-lived id
        Object domain = service.createDomainObject(domainClass);
        if (domain == null) {
          throw new UnexpectedException("Could not create instance of "
              + domainClass.getCanonicalName(), null);
        }
        AutoBean<? extends BaseProxy> bean = createProxyBean(id, domain);
        beans.put(id, bean);
        domainObjectsToId.put(domain, id);
      } else {
        // Decode the domain parameter
        Splittable split = StringQuoter.split(id.getServerId());
        Class<?> param = service.getIdType(domainClass);
        Object domainParam;
        if (ValueCodex.canDecode(param)) {
          domainParam = ValueCodex.decode(param, split);
        } else {
          domainParam = new SimpleRequestProcessor(service).decodeOobMessage(param, split).get(0);
        }

        // Enqueue
        domainClasses.add(service.resolveDomainClass(id.getProxyClass()));
        domainIds.add(domainParam);
        idsToLoad.add(id);
      }
    }

    // Actually load the data
    if (!domainClasses.isEmpty()) {
      assert domainClasses.size() == domainIds.size() && domainClasses.size() == idsToLoad.size();
      List<Object> loaded = service.loadDomainObjects(domainClasses, domainIds);
      if (idsToLoad.size() != loaded.size()) {
        throw new UnexpectedException("Expected " + idsToLoad.size()
            + " objects to be loaded, got " + loaded.size(), null);
      }

      Iterator<Object> itLoaded = loaded.iterator();
      for (SimpleProxyId<?> id : idsToLoad) {
        Object domain = itLoaded.next();
        domainObjectsToId.put(domain, id);
        AutoBean<? extends BaseProxy> bean = createProxyBean(id, domain);
        beans.put(id, bean);
      }
    }

    // Construct the return value
    List<AutoBean<? extends BaseProxy>> toReturn =
        new ArrayList<AutoBean<? extends BaseProxy>>(ids.size());
    for (SimpleProxyId<?> id : ids) {
      toReturn.add(beans.get(id));
    }
    return toReturn;
  }
}
