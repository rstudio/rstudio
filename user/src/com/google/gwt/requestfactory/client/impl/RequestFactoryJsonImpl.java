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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.DefaultRequestTransport;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.RequestTransport;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Base implementation of RequestFactory.
 */
public abstract class RequestFactoryJsonImpl implements RequestFactory {

  final class DataStoreToFutureMap {

    Map<ProxySchema<? extends ProxyImpl>, Map<Object, Object>> internalMap = new HashMap<ProxySchema<? extends ProxyImpl>, Map<Object, Object>>();

    Object get(Object datastoreId, ProxySchema<? extends ProxyImpl> schema) {
      Map<Object, Object> perSchemaMap = internalMap.get(schema);
      if (perSchemaMap == null) {
        return null;
      }
      return perSchemaMap.get(datastoreId);
    }

    /* returns the previous futureId, if any */
    Object put(Object datastoreId, ProxySchema<? extends ProxyImpl> schema,
        Object futureId) {
      Map<Object, Object> perSchemaMap = internalMap.get(schema);
      if (perSchemaMap == null) {
        perSchemaMap = new HashMap<Object, Object>();
        internalMap.put(schema, perSchemaMap);
      }
      return perSchemaMap.put(datastoreId, futureId);
    }
  }

  static final boolean IS_FUTURE = true;
  static final boolean NOT_FUTURE = false;

  private static Logger logger = Logger.getLogger(RequestFactory.class.getName());

  private final Integer initialVersion = 1;
  /*
   * Keeping these maps forever is not a desirable solution because of the
   * memory overhead but need these if want to provide stable {@EntityProxyId}.
   * 
   * futureToDatastoreMap is currently not used, will be useful in find
   * requests.
   */
  final Map<Object, Object> futureToDatastoreMap = new HashMap<Object, Object>();

  final DataStoreToFutureMap datastoreToFutureMap = new DataStoreToFutureMap();

  private long currentFutureId = 0;

  private ValueStoreJsonImpl valueStore;

  private EventBus eventBus;

  private RequestTransport transport;

  public <R extends ProxyImpl> R create(Class<R> token,
      ProxyToTypeMap recordToTypeMap) {

    ProxySchema<R> schema = recordToTypeMap.getType(token);
    if (schema == null) {
      throw new IllegalArgumentException("Unknown proxy type: " + token);
    }

    return createFuture(schema);
  }

  public ProxyRequest<EntityProxy> find(EntityProxyId proxyId) {
    return findRequest().find(proxyId);
  }

  public void fire(RequestObject<?> requestObject) {
    final AbstractRequest<?, ?> abstractRequest = (AbstractRequest<?, ?>) requestObject;
    RequestData requestData = ((AbstractRequest<?, ?>) requestObject).getRequestData();
    Map<String, String> requestMap = requestData.getRequestMap(abstractRequest.deltaValueStore.toJson());

    String payload = ClientRequestHelper.getRequestString(requestMap);
    transport.send(payload, new RequestTransport.TransportReceiver() {
      public void onTransportFailure(String message) {
        abstractRequest.fail(new ServerFailure(message, null,
            null));
      }

      public void onTransportSuccess(String payload) {
        abstractRequest.handleResponseText(payload);
      }
    });
  }

  public Class<? extends EntityProxy> getClass(EntityProxyId proxyId) {
    return ((EntityProxyIdImpl) proxyId).schema.getProxyClass();
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  public RequestTransport getRequestTransport() {
    return transport;
  }

  public abstract ProxySchema<?> getSchema(String token);

  public String getWireFormat(EntityProxyId proxyId) {
    EntityProxyIdImpl proxyIdImpl = (EntityProxyIdImpl) proxyId;
    Long id = (Long) proxyIdImpl.id;
    if (proxyIdImpl.isFuture) {
      // search for the datastore id for this futureId.
      Long datastoreId = (Long) futureToDatastoreMap.get(id);
      if (datastoreId == null) {
        throw new IllegalArgumentException(
            "Cannot call find on a proxyId before persisting");
      }
      id = datastoreId;
    }
    return ProxyImpl.getWireFormatId(id, NOT_FUTURE, proxyIdImpl.schema);
  }

  public void initialize(EventBus eventBus) {
    initialize(eventBus, new DefaultRequestTransport(eventBus));
  }
  
  public void initialize(EventBus eventBus, RequestTransport transport) {
    this.valueStore = new ValueStoreJsonImpl();
    this.eventBus = eventBus;
    this.transport = transport;
    logger.fine("Successfully initialized RequestFactory");
  }

  protected abstract FindRequest findRequest();

  protected Class<? extends EntityProxy> getClass(String token,
      ProxyToTypeMap recordToTypeMap) {
    String[] bits = token.split("-");
    ProxySchema<? extends EntityProxy> schema = recordToTypeMap.getType(bits[0]);
    if (schema == null) {
      return null;
    }
    return schema.getProxyClass();
  }

  protected String getHistoryToken(EntityProxyId proxyId, ProxyToTypeMap recordToTypeMap) {
    EntityProxyIdImpl entityProxyId = (EntityProxyIdImpl) proxyId;
    Class<? extends EntityProxy> proxyClass = entityProxyId.schema.getProxyClass();
    String rtn = recordToTypeMap.getClassToken(proxyClass) + "-";
    Object datastoreId = entityProxyId.id;
    if (entityProxyId.isFuture) {
      datastoreId = futureToDatastoreMap.get(entityProxyId.id);
    }
    if (datastoreId == null) {
      rtn += "0-FUTURE";
    } else {
      rtn += datastoreId;
    }
    return rtn;
  }

  protected EntityProxyId getProxyId(String token,
      ProxyToTypeMap recordToTypeMap) {
    String[] bits = token.split(EntityProxyIdImpl.SEPARATOR);
    if (bits.length != 2) {
      return null;
    }

    ProxySchema<? extends EntityProxy> schema = recordToTypeMap.getType(bits[1]);
    if (schema == null) {
      return null;
    }

    Long id = null;
    try {
      id = Long.valueOf(bits[0]);
    } catch (NumberFormatException e) {
      return null;
    }

    Object futureId = datastoreToFutureMap.get(id, schema);
    return new EntityProxyIdImpl(id, schema, false, futureId);
  }

  ValueStoreJsonImpl getValueStore() {
    return valueStore;
  }

  /*
   * use ProxyId instead here.
   */
  void postChangeEvent(ProxyJsoImpl newJsoRecord, WriteOperation op) {
    /*
     * Ensure event receivers aren't accidentally using cached info by making an
     * unpopulated copy of the record.
     */
    newJsoRecord = ProxyJsoImpl.emptyCopy(newJsoRecord);
    ProxySchema<?> schema = newJsoRecord.getSchema();
    EntityProxy javaRecord = schema.create(newJsoRecord);

    eventBus.fireEventFromSource(schema.createChangeEvent(javaRecord, op),
        schema.getProxyClass());
  }

  private <R extends ProxyImpl> R createFuture(ProxySchema<R> schema) {
    Long futureId = ++currentFutureId;
    ProxyJsoImpl newRecord = ProxyJsoImpl.create(futureId, initialVersion,
        schema, this);
    return schema.create(newRecord, IS_FUTURE);
  }
}
