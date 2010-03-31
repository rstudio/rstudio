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
package com.google.gwt.valuestore.client;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ValueStore} implementation based on {@link ValuesImpl}.
 */
public class ValueStoreJsonImpl implements ValueStore {
  // package protected fields for use by DeltaValueStoreJsonImpl
  
  final HandlerManager eventBus;

  final Map<RecordKey, ValuesImpl<?>> records = new HashMap<RecordKey, ValuesImpl<?>>();

  public ValueStoreJsonImpl(HandlerManager eventBus) {
    this.eventBus = eventBus;
  }

  public void addValidation() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public <K extends ValuesKey<K>> void setRecords(
      JsArray<ValuesImpl<K>> newRecords) {

    for (int i = 0, l = newRecords.length(); i < l; i++) {
      ValuesImpl<K> newRecord = newRecords.get(i);
      RecordKey recordKey = new RecordKey(newRecord);

      ValuesImpl<?> oldRecord = records.get(recordKey);
      if (oldRecord == null) {
        records.put(recordKey, newRecord);
      } else {
        boolean changed = oldRecord.merge(newRecord);
        newRecord = oldRecord.cast();
        newRecords.set(i, newRecord);
        if (changed) {
          eventBus.fireEvent(newRecord.createChangeEvent());
        }
      }
    }
  }
  
  /**
   * @return
   */
  public DeltaValueStoreJsonImpl spawnDeltaView() {
    return new DeltaValueStoreJsonImpl(this);
  }
}
