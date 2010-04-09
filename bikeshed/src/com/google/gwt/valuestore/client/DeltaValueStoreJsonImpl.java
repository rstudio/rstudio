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

import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DeltaValueStore} implementation based on {@link ValuesImpl}.
 */
public class DeltaValueStoreJsonImpl implements DeltaValueStore {

  private final ValueStoreJsonImpl master;
  private final Map<RecordKey, ValuesImpl<?>> changes = new HashMap<RecordKey, ValuesImpl<?>>();

  DeltaValueStoreJsonImpl(ValueStoreJsonImpl master) {
    this.master = master;
  }

  public void addValidation() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public void commit() {
    // TODO This will drop new verison numbers (and whatever else) returned
    // by the server.
    for (Map.Entry<RecordKey, ValuesImpl<?>> entry : changes.entrySet()) {
      final RecordKey key = entry.getKey();
      ValuesImpl<?> masterRecord = master.records.get(key);
      if (masterRecord == null) {
        master.records.put(key, entry.getValue());
        masterRecord = entry.getValue();
      } else {
        masterRecord.merge(entry.getValue());
      }
      master.eventBus.fireEvent(masterRecord.createChangeEvent());
    }
  }

  public boolean isChanged() {
    return !changes.isEmpty();
  }

  public <K extends ValuesKey<K>, V> void set(Property<K, V> property,
      Values<K> record, V value) {
    assert record instanceof ValuesImpl<?>;

    ValuesImpl<K> jsonRecord = (ValuesImpl<K>) record;

    RecordKey key = new RecordKey(jsonRecord);
    ValuesImpl<?> rawMasterRecord = master.records.get(key);
    ValuesImpl<?> rawChangeRecord = changes.get(key);

    ValuesImpl<K> changeRecord;

    if (rawChangeRecord == null) {
      // TODO will need to mark this as a sync record, not a new record
      changeRecord = newChangeRecord(record);
    } else {
      changeRecord = rawChangeRecord.cast();
    }

    if (isRealChange(property, value, rawMasterRecord)) {
      changeRecord.set(property, value);
      changes.put(key, changeRecord);
      return;
    }

    /*
     * Not done yet. If the user has changed the value back to the original
     * value, we should eliminate the previous value from the changeRecord. And
     * if the changeRecord is now empty, we should drop it entirely.
     */

    if (changeRecord.isDefined(property.getName())) {
      changeRecord.delete(property.getName());
    }
    if (changes.containsKey(key) && changeRecord.isEmpty()) {
      changes.remove(key);
    }
  }

  public DeltaValueStore spawnDeltaView() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public String toJson() {
    StringBuffer requestData = new StringBuffer("[");
    boolean first = true;
    for (Map.Entry<RecordKey, ValuesImpl<?>> entry : changes.entrySet()) {
      ValuesImpl<?> impl = entry.getValue();
      if (first) {
        first = false;
      } else {
        requestData.append(",");
      }
      requestData.append(impl.toJson());
    }
    requestData.append("]");
    return requestData.toString();
  }

  public boolean validate() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  private <K extends ValuesKey<K>, V> boolean isRealChange(
      Property<K, V> property, V value, ValuesImpl<?> rawMasterRecord) {
    ValuesImpl<K> masterRecord = null;

    if (rawMasterRecord == null) {
      return true;
    }

    masterRecord = rawMasterRecord.cast();

    if (!masterRecord.isDefined(property.getName())) {
      return true;
    }

    V masterValue = masterRecord.get(property);

    if (masterValue == value) {
      return false;
    }

    if ((masterValue != null)) {
      return !masterValue.equals(value);
    }

    return true;
  }

  private <K extends ValuesKey<K>> ValuesImpl<K> newChangeRecord(
      Values<K> fromRecord) {
    final ValuesImpl<K> changeRecord = ValuesImpl.emptyCopy(fromRecord);
    return changeRecord;
  }
}
