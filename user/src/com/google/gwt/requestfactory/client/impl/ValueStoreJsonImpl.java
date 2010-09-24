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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 */
class ValueStoreJsonImpl {
  // package protected fields for use by DeltaValueStoreJsonImpl

  final Map<EntityProxyIdImpl<?>, ProxyJsoImpl> records = new HashMap<EntityProxyIdImpl<?>, ProxyJsoImpl>();

  EntityProxy getRecordBySchemaAndId(ProxySchema<?> schema, String encodedId,
      boolean isFuture, RequestFactoryJsonImpl requestFactory) {
    if (encodedId == null) {
      return null;
    }

    EntityProxyIdImpl<?> key = new EntityProxyIdImpl<EntityProxy>(encodedId,
        schema, isFuture, requestFactory.datastoreToFutureMap.get(encodedId,
            schema));
    return schema.create(records.get(key), isFuture);
  }

  /**
   * Puts a newly-created {@link ProxyJsoImpl} newJsoRecord in the valueStore
   * following a create, and sends appropriate events. If the valuestore had a
   * Jso that was the same or a superset of newJsoRecord, returns the valuestore
   * jso. Otherwise, puts newJsoRecord in the valuestore and returns null.
   * <p>
   * package-protected for testing purposes.
   */
  ProxyJsoImpl putFutureInValueStore(ProxyJsoImpl newJsoRecord) {
    return putInValueStore(newJsoRecord, RequestFactoryJsonImpl.IS_FUTURE);
  }

  /**
   * Puts a {@link ProxyJsoImpl} newJsoRecord in the valueStore following a
   * copy-on-write scheme, and sends appropriate events. If the valuestore had a
   * Jso that was the same or a superset of newJsoRecord, returns the valuestore
   * jso. Otherwise, puts newJsoRecord in the valuestore and returns null.
   * <p>
   * package-protected for testing purposes.
   */
  ProxyJsoImpl putInValueStore(ProxyJsoImpl newJsoRecord) {
    return putInValueStore(newJsoRecord, RequestFactoryJsonImpl.NOT_FUTURE);
  }

  void setRecords(JsArray<ProxyJsoImpl> newRecords) {
    for (int i = 0, l = newRecords.length(); i < l; i++) {
      ProxyJsoImpl oldRecord = putInValueStore(newRecords.get(i));
      if (oldRecord != null) {
        newRecords.set(i, oldRecord);
      }
    }
  }

  private ProxyJsoImpl putInValueStore(ProxyJsoImpl newJsoRecord,
      boolean isFuture) {
    RequestFactoryJsonImpl factory = newJsoRecord.getRequestFactory();
    EntityProxyIdImpl<?> recordKey = new EntityProxyIdImpl<EntityProxy>(
        newJsoRecord.encodedId(), newJsoRecord.getSchema(), isFuture,
        factory.datastoreToFutureMap.get(newJsoRecord.encodedId(),
            newJsoRecord.getSchema()));

    ProxyJsoImpl oldRecord = records.get(recordKey);
    if (oldRecord == null) {
      records.put(recordKey, newJsoRecord);
      newJsoRecord.assertValid();
      factory.postChangeEvent(newJsoRecord, isFuture ? WriteOperation.CREATE
          : WriteOperation.ACQUIRE);
      return null;
    } 

    if (oldRecord.hasChanged(newJsoRecord)) {
      records.put(recordKey, newJsoRecord);
      factory.postChangeEvent(newJsoRecord, WriteOperation.UPDATE);
      return null;
    }
    return oldRecord;
  }
}
