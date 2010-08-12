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
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.valuestore.shared.WriteOperation;

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

  final HandlerManager eventBus;

  final Map<RecordKey, RecordJsoImpl> records = new HashMap<RecordKey, RecordJsoImpl>();

  ValueStoreJsonImpl(HandlerManager eventBus) {
    this.eventBus = eventBus;
  }

  public void setRecord(RecordJsoImpl newRecord) {
    setRecordInList(newRecord, 0, null);
  }

  public void setRecords(JsArray<RecordJsoImpl> newRecords) {
    for (int i = 0, l = newRecords.length(); i < l; i++) {
      RecordJsoImpl newRecord = newRecords.get(i);
      setRecordInList(newRecord, i, newRecords);
    }
  }

  /**
   * @param newRecord
   * @param i
   * @param array
   */
  private void setRecordInList(RecordJsoImpl newRecord, int i,
      JsArray<RecordJsoImpl> array) {
    RecordKey recordKey = new RecordKey(newRecord, RequestFactoryJsonImpl.NOT_FUTURE);

    RecordJsoImpl oldRecord = records.get(recordKey);
    if (oldRecord == null) {
      records.put(recordKey, newRecord);
      // TODO: need to fire a create event.
    } else {
      // TODO: Merging is not the correct thing to do but it works as long as we
      // don't have filtering by properties. Need to revisit this once response
      // only has a subset of all properties.
      boolean changed = oldRecord.merge(newRecord);
      newRecord = oldRecord.cast();
      if (array != null) {
        array.set(i, newRecord);
      }
      if (changed) {
        eventBus.fireEvent(newRecord.getSchema().createChangeEvent(newRecord,
            WriteOperation.UPDATE));
      }
    }
  }
}
