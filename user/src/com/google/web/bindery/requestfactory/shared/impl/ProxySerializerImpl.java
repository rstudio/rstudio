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

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.ProxySerializer;
import com.google.web.bindery.requestfactory.shared.ProxyStore;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage;
import com.google.web.bindery.requestfactory.shared.messages.OperationMessage;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage.Strength;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of ProxySerializer.
 */
class ProxySerializerImpl extends AbstractRequestContext implements ProxySerializer {

  /**
   * Used internally to unwind the stack if data cannot be found in the backing
   * store.
   */
  private static class NoDataException extends RuntimeException {
  }

  private final ProxyStore store;
  /**
   * If the user wants to serialize a proxy with a non-persistent id (including
   * ValueProxy), we'll assign a synthetic id that is local to the store being
   * used.
   */
  private final Map<SimpleProxyId<?>, SimpleProxyId<?>> syntheticIds =
      new HashMap<SimpleProxyId<?>, SimpleProxyId<?>>();

  /**
   * The ids of proxies whose content has been reloaded.
   */
  private final Set<SimpleProxyId<?>> restored = new HashSet<SimpleProxyId<?>>();
  private final Map<SimpleProxyId<?>, AutoBean<?>> serialized =
      new HashMap<SimpleProxyId<?>, AutoBean<?>>();

  public ProxySerializerImpl(AbstractRequestFactory factory, ProxyStore store) {
    super(factory, Dialect.STANDARD);
    this.store = store;
  }

  public <T extends BaseProxy> T deserialize(Class<T> proxyType, String key) {
    // Fast exit to prevent getOperation from throwing an exception
    if (store.get(key) == null) {
      return null;
    }
    OperationMessage op = getOperation(key);
    @SuppressWarnings("unchecked")
    SimpleProxyId<T> id = (SimpleProxyId<T>) getId(op);
    return doDeserialize(id);
  }

  public <T extends EntityProxy> T deserialize(EntityProxyId<T> id) {
    return doDeserialize((SimpleEntityProxyId<T>) id);
  }

  /**
   * Replace non-persistent ids with store-local ids.
   */
  @Override
  public Splittable getSerializedProxyId(SimpleProxyId<?> stableId) {
    return super.getSerializedProxyId(serializedId(stableId));
  }

  public String serialize(BaseProxy rootObject) {
    if (rootObject == null) {
      return "null";
    }
    
    final AutoBean<? extends BaseProxy> root = AutoBeanUtils.getAutoBean(rootObject);
    if (root == null) {
      // Unexpected, some kind of foreign implementation of the BaseProxy?
      throw new IllegalArgumentException();
    }

    final SimpleProxyId<?> id = serializedId(BaseProxyCategory.stableId(root));
    // Only persistent and synthetic ids expected
    assert !id.isEphemeral() : "Unexpected ephemeral id " + id.toString();

    /*
     * Don't repeatedly serialize the same proxy, unless we're looking at a
     * mutable instance.
     */
    AutoBean<?> previous = serialized.get(id);
    if (previous == null || !previous.isFrozen()) {
      serialized.put(id, root);
      serializeOneProxy(id, root);
      root.accept(new AutoBeanVisitor() {
        @Override
        public void endVisit(AutoBean<?> bean, Context ctx) {
          // Avoid unnecessary method call
          if (bean == root) {
            return;
          }
          if (isEntityType(bean.getType()) || isValueType(bean.getType())) {
            serialize((BaseProxy) bean.as());
          }
        }

        @Override
        public void endVisitCollectionProperty(String propertyName, AutoBean<Collection<?>> value,
            CollectionPropertyContext ctx) {
          if (value == null) {
            return;
          }
          if (isEntityType(ctx.getElementType()) || isValueType(ctx.getElementType())) {
            for (Object o : value.as()) {
              serialize((BaseProxy) o);
            }
          }
        }
      });
    }

    return getRequestFactory().getHistoryToken(id);
  }

  @Override
  protected AutoBeanFactory getAutoBeanFactory() {
    return getRequestFactory().getAutoBeanFactory();
  }

  @Override
  SimpleProxyId<BaseProxy> getId(IdMessage op) {
    if (Strength.SYNTHETIC.equals(op.getStrength())) {
      return getRequestFactory().allocateSyntheticId(
          getRequestFactory().getTypeFromToken(op.getTypeToken()), op.getSyntheticId());
    }
    return super.getId(op);
  }

  @Override
  <Q extends BaseProxy> AutoBean<Q> getProxyForReturnPayloadGraph(SimpleProxyId<Q> id) {
    AutoBean<Q> toReturn = super.getProxyForReturnPayloadGraph(id);
    if (restored.add(id)) {
      /*
       * If we haven't seen the id before, use the data in the OperationMessage
       * to repopulate the properties of the canonical bean for this id.
       */
      OperationMessage op = getOperation(getRequestFactory().getHistoryToken(id));
      this.processReturnOperation(id, op);
      toReturn.setTag(Constants.STABLE_ID, super.getId(op));
    }
    return toReturn;
  }

  /**
   * Reset all temporary state.
   */
  private void clear() {
    syntheticIds.clear();
    restored.clear();
    serialized.clear();
  }

  private <T extends BaseProxy> T doDeserialize(SimpleProxyId<T> id) {
    try {
      return getProxyForReturnPayloadGraph(id).as();
    } catch (NoDataException e) {
      return null;
    } finally {
      clear();
    }
  }

  /**
   * Load the OperationMessage containing the object state from the backing
   * store.
   */
  private OperationMessage getOperation(String key) {
    Splittable data = store.get(key);
    if (data == null) {
      throw new NoDataException();
    }

    OperationMessage op =
        AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, OperationMessage.class, data).as();
    return op;
  }

  /**
   * Convert any non-persistent ids into store-local synthetic ids.
   */
  private <T extends BaseProxy> SimpleProxyId<T> serializedId(SimpleProxyId<T> stableId) {
    assert !stableId.isSynthetic();
    if (stableId.isEphemeral()) {
      @SuppressWarnings("unchecked")
      SimpleProxyId<T> syntheticId = (SimpleProxyId<T>) syntheticIds.get(stableId);
      if (syntheticId == null) {
        int nextId = store.nextId();
        assert nextId >= 0 : "ProxyStore.nextId() returned a negative number " + nextId;
        syntheticId = getRequestFactory().allocateSyntheticId(stableId.getProxyClass(), nextId + 1);
        syntheticIds.put(stableId, syntheticId);
      }
      return syntheticId;
    }
    return stableId;
  }

  private void serializeOneProxy(SimpleProxyId<?> idForSerialization,
      AutoBean<? extends BaseProxy> bean) {
    AutoBean<OperationMessage> op =
        makeOperationMessage(serializedId(BaseProxyCategory.stableId(bean)), bean, false);

    store.put(getRequestFactory().getHistoryToken(idForSerialization), AutoBeanCodex.encode(op));
  }
}
