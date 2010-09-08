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

  final Map<EntityProxyIdImpl, ProxyJsoImpl> records = new HashMap<EntityProxyIdImpl, ProxyJsoImpl>();

  EntityProxy getRecordBySchemaAndId(ProxySchema<?> schema, Long id,
      RequestFactoryJsonImpl requestFactory) {
    if (id == null) {
      return null;
    }
    // TODO: pass isFuture to this method from decoding ID string
    EntityProxyIdImpl key = new EntityProxyIdImpl(id, schema, false,
        requestFactory.datastoreToFutureMap.get(id, schema));
    return schema.create(records.get(key));
  }

  void setProxy(ProxyJsoImpl newRecord) {
    setRecordInList(newRecord, 0, null);
  }

  void setRecords(JsArray<ProxyJsoImpl> newRecords) {
    for (int i = 0, l = newRecords.length(); i < l; i++) {
      ProxyJsoImpl newRecord = newRecords.get(i);
      setRecordInList(newRecord, i, newRecords);
    }
  }

  private void setRecordInList(ProxyJsoImpl newJsoRecord, int i,
      JsArray<ProxyJsoImpl> array) {
    EntityProxyIdImpl recordKey = new EntityProxyIdImpl(newJsoRecord.getId(),
        newJsoRecord.getSchema(), RequestFactoryJsonImpl.NOT_FUTURE,
        newJsoRecord.getRequestFactory().datastoreToFutureMap.get(
            newJsoRecord.getId(), newJsoRecord.getSchema()));

    ProxyJsoImpl oldRecord = records.get(recordKey);
    if (oldRecord == null) {
      records.put(recordKey, newJsoRecord);
      // TODO: need to fire a create event.
    } else {
      // TODO: Merging is not the correct thing to do but it works as long as we
      // don't have filtering by properties. Need to revisit this once response
      // only has a subset of all properties.
      boolean changed = oldRecord.merge(newJsoRecord);
      newJsoRecord = oldRecord.cast();
      if (array != null) {
        array.set(i, newJsoRecord);
      }
      if (changed) {
        newJsoRecord.getRequestFactory().postChangeEvent(newJsoRecord,
            WriteOperation.UPDATE);
      }
    }
  }
}
