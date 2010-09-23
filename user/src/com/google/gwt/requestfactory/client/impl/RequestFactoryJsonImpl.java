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
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
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
  private static final String FUTURE_TOKEN = "F";
  private static final String HISTORY_TOKEN_SEPARATOR = "--";
  private static final int ID_INDEX = 0;
  private static final int SCHEMA_INDEX = 1;

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

  public void fire(Request<?> requestObject) {
    final AbstractRequest<?, ?> abstractRequest = (AbstractRequest<?, ?>) requestObject;
    RequestData requestData = ((AbstractRequest<?, ?>) requestObject).getRequestData();
    Map<String, String> requestMap = requestData.getRequestMap(abstractRequest.deltaValueStore.toJson());

    String payload = ClientRequestHelper.getRequestString(requestMap);
    transport.send(payload, new RequestTransport.TransportReceiver() {
      public void onTransportFailure(String message) {
        abstractRequest.fail(new ServerFailure(message, null, null));
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
    String id = (String) proxyIdImpl.encodedId;
    if (proxyIdImpl.isFuture) {
      // search for the datastore id for this futureId.
      String datastoreId = (String) futureToDatastoreMap.get(id);
      if (datastoreId == null) {
        throw new IllegalArgumentException(
            "Cannot call find on a proxyId before persisting");
      }
      id = datastoreId;
    }
    return ProxyImpl.wireFormatId(id, NOT_FUTURE, proxyIdImpl.schema);
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

  /**
   * This implementation cannot be changed without breaking clients.
   */
  protected Class<? extends EntityProxy> getClass(String token,
      ProxyToTypeMap recordToTypeMap) {
    String[] bits = token.split(HISTORY_TOKEN_SEPARATOR);
    String schemaToken;
    if (bits.length == 1) {
      schemaToken = token;
    } else if (bits.length == 2 || bits.length == 3) {
      schemaToken = bits[SCHEMA_INDEX];
    } else {
      return null;
    }
    ProxySchema<? extends EntityProxy> schema = recordToTypeMap.getType(schemaToken);
    if (schema == null) {
      return null;
    }
    return schema.getProxyClass();
  }

  /**
   * This implementation cannot be changed without breaking clients.
   */
  protected String getHistoryToken(EntityProxyId<?> proxyId,
      ProxyToTypeMap recordToTypeMap) {
    EntityProxyIdImpl<?> entityProxyId = (EntityProxyIdImpl<?>) proxyId;
    boolean isFuture = false;
    Object tokenId = entityProxyId.encodedId;
    if (entityProxyId.isFuture) {
      // See if the associated entityproxy has been persisted in the meantime
      Object persistedId = futureToDatastoreMap.get(entityProxyId.encodedId);
      if (persistedId == null) {
        // Return a future token
        isFuture = true;
      } else {
        // Use the persisted id instead
        tokenId = persistedId;
      }
    }
    StringBuilder toReturn = new StringBuilder();
    toReturn.append(tokenId);
    toReturn.append(HISTORY_TOKEN_SEPARATOR).append(
        entityProxyId.schema.getToken());
    if (isFuture) {
      toReturn.append(HISTORY_TOKEN_SEPARATOR).append(FUTURE_TOKEN);
    }
    return toReturn.toString();
  }

  /**
   * This implementation cannot be changed without breaking clients.
   */
  protected EntityProxyId<?> getProxyId(String historyToken,
      ProxyToTypeMap recordToTypeMap) {
    String[] bits = historyToken.split(HISTORY_TOKEN_SEPARATOR);
    if (bits.length < 2 || bits.length > 3) {
      return null;
    }
    boolean isFuture = bits.length == 3;

    ProxySchema<? extends EntityProxy> schema = recordToTypeMap.getType(bits[SCHEMA_INDEX]);
    if (schema == null) {
      return null;
    }

    String id = bits[ID_INDEX];
    Object futureId = null;
    if (!isFuture) {
      // Look in the map only if it is a datastoreId.
      futureId = datastoreToFutureMap.get(id, schema);
    }
    return new EntityProxyIdImpl<EntityProxy>(id, schema, isFuture, futureId);
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
    ProxyJsoImpl newRecord = ProxyJsoImpl.create(Long.toString(futureId),
        initialVersion, schema, this);
    return schema.create(newRecord, IS_FUTURE);
  }
}
