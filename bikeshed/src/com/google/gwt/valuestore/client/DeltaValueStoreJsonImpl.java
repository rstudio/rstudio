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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.shared.RequestFactory.WriteOperation;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.impl.RecordImpl;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link DeltaValueStore} implementation based on {@link ValuesImpl}.
 */
public class DeltaValueStoreJsonImpl implements DeltaValueStore {

  static class ReturnRecord extends JavaScriptObject {

    public static final native JsArray<ReturnRecord> getRecords(
        JavaScriptObject response, String operation) /*-{
      return response[operation];
    }-*/;

    private static native void fillKeys(JavaScriptObject jso, HashSet<String> s) /*-{
      for (key in jso) {
        if (jso.hasOwnProperty(key)) {
          s.@java.util.HashSet::add(Ljava/lang/Object;)(key);
        }
      }
    }-*/;

    private static native JavaScriptObject getJsoResponse(String response) /*-{
      // TODO: clean this
      eval("xyz=" + response);
      return xyz;
    }-*/;

    protected ReturnRecord() {
    }

    public final native String getFutureId()/*-{
      return this.futureId;
    }-*/;

    public final native String getId() /*-{
      return this.id;
    }-*/;

    public final native String getVersion()/*-{
      return this.version;
    }-*/;

    public final native boolean hasFutureId()/*-{
      return 'futureId' in this;
    }-*/;
  }

  private static class FutureIdGenerator {
    Set<String> idsInTransit = new HashSet<String>();
    int maxId = 1;

    void delete(String id) {
      idsInTransit.remove(id);
    }

    String getFutureId() {
      int futureId = maxId++;
      if (maxId == Integer.MAX_VALUE) {
        maxId = 1;
      }
      assert !idsInTransit.contains(futureId);
      return new String(futureId + "");
    }
  }

  private static final String INITIAL_VERSION = "1";

  private boolean used = false;
  private final FutureIdGenerator futureIdGenerator = new FutureIdGenerator();

  private final ValueStoreJsonImpl master;

  // track C-U-D of CRUD operations
  private final Map<RecordKey, RecordJsoImpl> creates = new HashMap<RecordKey, RecordJsoImpl>();
  private final Map<RecordKey, RecordJsoImpl> updates = new HashMap<RecordKey, RecordJsoImpl>();
  private final Map<RecordKey, RecordJsoImpl> deletes = new HashMap<RecordKey, RecordJsoImpl>();

  private final Map<RecordKey, WriteOperation> operations = new HashMap<RecordKey, WriteOperation>();

  DeltaValueStoreJsonImpl(ValueStoreJsonImpl master) {
    this.master = master;
  }

  public void addValidation() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public void commit(String response) {
    JavaScriptObject returnedJso = ReturnRecord.getJsoResponse(response);
    HashSet<String> keys = new HashSet<String>();
    ReturnRecord.fillKeys(returnedJso, keys);

    if (keys.contains(WriteOperation.CREATE.name())) {
      JsArray<ReturnRecord> newRecords = ReturnRecord.getRecords(returnedJso,
          WriteOperation.CREATE.name());
      // construct a map from futureId to the datastore Id
      Map<Object, Object> futureToDatastoreId = new HashMap<Object, Object>();
      int length = newRecords.length();
      for (int i = 0; i < length; i++) {
        ReturnRecord sync = newRecords.get(i);
        futureToDatastoreId.put(sync.getFutureId(), sync.getId());
      }

      for (Map.Entry<RecordKey, RecordJsoImpl> entry : creates.entrySet()) {
        final RecordKey futureKey = entry.getKey();
        Object datastoreId = futureToDatastoreId.get(futureKey.id);
        assert datastoreId != null;
        futureIdGenerator.delete(futureKey.id.toString());

        final RecordKey key = new RecordKey(datastoreId, futureKey.schema);
        RecordJsoImpl value = entry.getValue();
        value.set(Record.id, datastoreId.toString());
        RecordJsoImpl masterRecord = master.records.get(key);
        assert masterRecord == null;
        master.records.put(key, value);
        masterRecord = value;
        master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
            masterRecord, WriteOperation.CREATE));
      }
    }

    if (keys.contains(WriteOperation.DELETE.name())) {
      JsArray<ReturnRecord> deletedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.DELETE.name());
      Set<String> returnedKeys = getKeySet(deletedRecords);
      for (Map.Entry<RecordKey, RecordJsoImpl> entry : deletes.entrySet()) {
        final RecordKey key = entry.getKey();
        assert returnedKeys.contains(key.id);
        RecordJsoImpl masterRecord = master.records.get(key);
        assert masterRecord != null;
        master.records.remove(key);
        master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
            masterRecord, WriteOperation.DELETE));
      }
    }

    if (keys.contains(WriteOperation.UPDATE.name())) {
      JsArray<ReturnRecord> updatedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.UPDATE.name());
      Set<String> returnedKeys = getKeySet(updatedRecords);
      for (Map.Entry<RecordKey, RecordJsoImpl> entry : updates.entrySet()) {
        final RecordKey key = entry.getKey();
        assert returnedKeys.contains(key.id.toString());
        RecordJsoImpl masterRecord = master.records.get(key);
        assert masterRecord != null;
        masterRecord.merge(entry.getValue());
        master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
            masterRecord, WriteOperation.UPDATE));
      }
    }
  }

  // TODO: don't use RecordSchema
  public Record create(Record record) {
    assert !used;
    assert record instanceof RecordImpl;
    RecordImpl recordImpl = (RecordImpl) record;
    String futureId = futureIdGenerator.getFutureId();
    RecordJsoImpl newRecord = RecordJsoImpl.newCopy(recordImpl.getSchema(),
        futureId, INITIAL_VERSION);
    RecordKey recordKey = new RecordKey(newRecord);
    assert operations.get(recordKey) == null;
    operations.put(recordKey, WriteOperation.CREATE);
    creates.put(recordKey, newRecord);
    return newRecord;
  }

  public void delete(Record record) {
    assert !used;
    assert record instanceof RecordImpl;
    RecordImpl recordImpl = (RecordImpl) record;
    RecordKey recordKey = new RecordKey(recordImpl);
    WriteOperation priorOperation = operations.get(recordKey);
    if (priorOperation == null) {
      operations.put(recordKey, WriteOperation.DELETE);
      deletes.put(recordKey, recordImpl.asJso());
      return;
    }
    Record priorRecord = null;
    switch (priorOperation) {
      case CREATE:
        priorRecord = creates.remove(recordKey);
        assert priorRecord != null;
        operations.remove(recordKey);
        break;
      case DELETE:
        // nothing to do here.
        break;
      case UPDATE:
        // undo update
        priorRecord = updates.remove(recordKey);
        assert priorRecord != null;
        operations.remove(recordKey);

        // actually delete
        operations.put(recordKey, WriteOperation.DELETE);
        deletes.put(recordKey, recordImpl.asJso());
        break;
      default:
        throw new IllegalStateException("unknown prior WriteOperation "
            + priorOperation.name());
    }
  }

  public boolean isChanged() {
    assert !used;
    return !operations.isEmpty();
  }

  public <V> void set(Property<V> property, Record record, V value) {
    assert !used;
    assert record instanceof RecordImpl;
    RecordImpl recordImpl = (RecordImpl) record;
    RecordKey recordKey = new RecordKey(recordImpl);

    RecordJsoImpl rawMasterRecord = master.records.get(recordKey);
    WriteOperation priorOperation = operations.get(recordKey);
    if (priorOperation == null) {
      addNewChangeRecord(recordKey, recordImpl, property, value);
      return;
    }

    RecordJsoImpl priorRecord = null;
    switch (priorOperation) {
      case CREATE:
        // nothing to do here.
        priorRecord = creates.get(recordKey);
        assert priorRecord != null;
        priorRecord.set(property, value);
        break;
      case DELETE:
        // undo delete
        RecordJsoImpl recordJsoImpl = deletes.remove(recordKey);
        assert recordJsoImpl != null;
        operations.remove(recordKey);

        // add new change record
        addNewChangeRecord(recordKey, recordImpl, property, value);
        break;
      case UPDATE:
        priorRecord = updates.get(recordKey);
        assert priorRecord != null;

        if (isRealChange(property, value, rawMasterRecord)) {
          priorRecord.set(property, value);
          updates.put(recordKey, priorRecord);
          return;
        }
        /*
         * Not done yet. If the user has changed the value back to the original
         * value, we should eliminate the previous value from the changeRecord.
         * And if the changeRecord is now empty, we should drop it entirely.
         */

        if (priorRecord.isDefined(property.getName())) {
          priorRecord.delete(property.getName());
        }
        if (updates.containsKey(recordKey) && priorRecord.isEmpty()) {
          updates.remove(recordKey);
          operations.remove(recordKey);
        }
        break;
    }
  }

  public DeltaValueStore spawnDeltaView() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public String toJson() {
    used = true;
    StringBuffer jsonData = new StringBuffer("{");
    for (WriteOperation writeOperation : WriteOperation.values()) {
      String jsonDataForOperation = getJsonForOperation(writeOperation);
      if (jsonDataForOperation.equals("")) {
        continue;
      }
      if (jsonData.length() > 1) {
        jsonData.append(",");
      }
      jsonData.append(jsonDataForOperation);
    }
    jsonData.append("}");
    return jsonData.toString();
  }

  public boolean validate() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  /**
   * returns true if a new change record has been added.
   */
  private <V> boolean addNewChangeRecord(RecordKey recordKey,
      RecordImpl recordImpl, Property<V> property, V value) {
    RecordJsoImpl rawMasterRecord = master.records.get(recordKey);
    RecordJsoImpl changeRecord = newChangeRecord(recordImpl);
    if (isRealChange(property, value, rawMasterRecord)) {
      changeRecord.set(property, value);
      updates.put(recordKey, changeRecord);
      operations.put(recordKey, WriteOperation.UPDATE);
      return true;
    }
    return false;
  }

  private String getJsonForOperation(WriteOperation writeOperation) {
    Map<RecordKey, RecordJsoImpl> recordsMap = getRecordsMap(writeOperation);
    if (recordsMap.size() == 0) {
      return "";
    }
    StringBuffer requestData = new StringBuffer("\"" + writeOperation.name()
        + "\":[");
    boolean first = true;
    for (Map.Entry<RecordKey, RecordJsoImpl> entry : recordsMap.entrySet()) {
      RecordJsoImpl impl = entry.getValue();
      if (first) {
        first = false;
      } else {
        requestData.append(",");
      }
      requestData.append("{\"" + entry.getValue().getSchema().getToken()
          + "\":");
      if (writeOperation != WriteOperation.DELETE) {
        requestData.append(impl.toJson());
      } else {
        requestData.append(impl.toJsonIdVersion());
      }
      requestData.append("}");
    }
    requestData.append("]");
    return requestData.toString();
  }

  private Set<String> getKeySet(JsArray<ReturnRecord> records) {
    Set<String> returnSet = new HashSet<String>();
    int length = records.length();
    for (int i = 0; i < length; i++) {
      returnSet.add(records.get(i).getId());
    }
    return returnSet;
  }

  private Map<RecordKey, RecordJsoImpl> getRecordsMap(
      WriteOperation writeOperation) {
    switch (writeOperation) {
      case CREATE:
        return creates;
      case DELETE:
        return deletes;
      case UPDATE:
        return updates;
      default:
        throw new IllegalStateException("unknow writeOperation "
            + writeOperation.name());
    }
  }

  private <V> boolean isRealChange(Property<V> property, V value,
      RecordJsoImpl rawMasterRecord) {
    RecordJsoImpl masterRecord = null;

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

  private RecordJsoImpl newChangeRecord(RecordImpl fromRecord) {
    return RecordJsoImpl.emptyCopy(fromRecord);
  }
}
