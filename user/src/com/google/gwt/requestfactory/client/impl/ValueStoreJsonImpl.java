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
public class ValueStoreJsonImpl {
  // package protected fields for use by DeltaValueStoreJsonImpl

  final Map<EntityProxyId, ProxyJsoImpl> records = new HashMap<EntityProxyId, ProxyJsoImpl>();

  public EntityProxy getRecordBySchemaAndId(ProxySchema<?> schema,
      Long id) {
    if (id == null) {
      return null;
    }
    // TODO: pass isFuture to this method from decoding ID string
    EntityProxyId key = new EntityProxyId(id, schema, false);
    return schema.create(records.get(key));
  }

  public void setProxy(ProxyJsoImpl newRecord,
      RequestFactoryJsonImpl requestFactory) {
    setRecordInList(newRecord, 0, null, requestFactory);
  }

  public void setRecords(JsArray<ProxyJsoImpl> newRecords,
      RequestFactoryJsonImpl requestFactory) {
    for (int i = 0, l = newRecords.length(); i < l; i++) {
      ProxyJsoImpl newRecord = newRecords.get(i);
      setRecordInList(newRecord, i, newRecords, requestFactory);
    }
  }

  private void setRecordInList(ProxyJsoImpl newJsoRecord, int i,
      JsArray<ProxyJsoImpl> array, RequestFactoryJsonImpl requestFactory) {
    EntityProxyId recordKey = new EntityProxyId(newJsoRecord, RequestFactoryJsonImpl.NOT_FUTURE);
    newJsoRecord.setValueStore(this);
    newJsoRecord.setRequestFactory(requestFactory);
    
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
        requestFactory.postChangeEvent(newJsoRecord, WriteOperation.UPDATE);
      }
    }
  }
}
