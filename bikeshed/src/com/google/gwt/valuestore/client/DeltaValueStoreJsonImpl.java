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
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.WriteOperation;
import com.google.gwt.valuestore.shared.impl.RecordImpl;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;
import com.google.gwt.valuestore.shared.impl.RecordToTypeMap;

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

    public final native void fillViolations(HashMap<String, String> s) /*-{
      for (key in this.violations) {
        if (this.violations.hasOwnProperty(key)) {
          s.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, this.violations[key]);
        }
      }
    }-*/;

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

    public final native boolean hasId()/*-{
      return 'id' in this;
    }-*/;

    public final native boolean hasViolations()/*-{
      return 'violations' in this;
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

  private static final HashMap<String, String> NULL_VIOLATIONS = new HashMap<String, String>();

  private static final Integer INITIAL_VERSION = 1;

  private boolean used = false;
  private final FutureIdGenerator futureIdGenerator = new FutureIdGenerator();

  private final ValueStoreJsonImpl master;
  private final RecordToTypeMap recordToTypeMap;

  // track C-U-D of CRUD operations
  private final Map<RecordKey, RecordJsoImpl> creates = new HashMap<RecordKey, RecordJsoImpl>();
  private final Map<RecordKey, RecordJsoImpl> updates = new HashMap<RecordKey, RecordJsoImpl>();
  private final Map<RecordKey, RecordJsoImpl> deletes = new HashMap<RecordKey, RecordJsoImpl>();

  private final Map<RecordKey, WriteOperation> operations = new HashMap<RecordKey, WriteOperation>();

  DeltaValueStoreJsonImpl(ValueStoreJsonImpl master,
      RecordToTypeMap recordToTypeMap) {
    this.master = master;
    this.recordToTypeMap = recordToTypeMap;
  }

  public void addValidation() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public void clearUsed() {
    used = false;
  }

  public Set<SyncResult> commit(String response) {
    Set<SyncResult> syncResults = new HashSet<SyncResult>();
    JavaScriptObject returnedJso = ReturnRecord.getJsoResponse(response);
    HashSet<String> keys = new HashSet<String>();
    ReturnRecord.fillKeys(returnedJso, keys);

    Set<RecordKey> toRemove = new HashSet<RecordKey>();
    if (keys.contains(WriteOperation.CREATE.name())) {
      JsArray<ReturnRecord> newRecords = ReturnRecord.getRecords(returnedJso,
          WriteOperation.CREATE.name());
      /*
       * construct 2 maps: (i) futureId to the datastore Id, (ii) futureId to
       * violationsMap
       */
      Map<Object, Object> futureToDatastoreId = new HashMap<Object, Object>();
      Map<String, Map<String, String>> violationsMap = new HashMap<String, Map<String, String>>();
      int length = newRecords.length();
      for (int i = 0; i < length; i++) {
        ReturnRecord sync = newRecords.get(i);
        if (sync.hasViolations()) {
          // does not have an id.
          assert !sync.hasId();
          HashMap<String, String> violations = new HashMap<String, String>();
          sync.fillViolations(violations);
          violationsMap.put(sync.getFutureId(), violations);
        } else {
          violationsMap.put(sync.getFutureId(), NULL_VIOLATIONS);
          futureToDatastoreId.put(sync.getFutureId(), sync.getId());
        }
      }

      for (Map.Entry<RecordKey, RecordJsoImpl> entry : creates.entrySet()) {
        final RecordKey futureKey = entry.getKey();
        Map<String, String> violations = violationsMap.get(futureKey.id);
        assert violations != null;
        if (violations == NULL_VIOLATIONS) {
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
          toRemove.add(key);
          master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
              masterRecord, WriteOperation.CREATE));
          syncResults.add(new SyncResultImpl(masterRecord, null,
              futureKey.id.toString()));
        } else {
          // do not change the masterRecord or fire event
          syncResults.add(new SyncResultImpl(entry.getValue(), violations,
              futureKey.id.toString()));
        }
      }
    }
    processToRemove(toRemove, WriteOperation.CREATE);

    toRemove.clear();
    if (keys.contains(WriteOperation.DELETE.name())) {
      JsArray<ReturnRecord> deletedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.DELETE.name());
      Map<String, Map<String, String>> violationsMap = getViolationsMap(deletedRecords);
      for (Map.Entry<RecordKey, RecordJsoImpl> entry : deletes.entrySet()) {
        final RecordKey key = entry.getKey();
        Map<String, String> violations = violationsMap.get(key.id);
        assert violations != null;
        if (violations == NULL_VIOLATIONS) {
          RecordJsoImpl masterRecord = master.records.get(key);
          assert masterRecord != null;
          master.records.remove(key);
          toRemove.add(key);
          master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
              masterRecord, WriteOperation.DELETE));
          syncResults.add(new SyncResultImpl(masterRecord, null, null));
        } else {
          // do not change the masterRecord or fire event
          syncResults.add(new SyncResultImpl(entry.getValue(), violations, null));
        }
      }
    }
    processToRemove(toRemove, WriteOperation.DELETE);

    toRemove.clear();
    if (keys.contains(WriteOperation.UPDATE.name())) {
      JsArray<ReturnRecord> updatedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.UPDATE.name());
      Map<String, Map<String, String>> violationsMap = getViolationsMap(updatedRecords);
      for (Map.Entry<RecordKey, RecordJsoImpl> entry : updates.entrySet()) {
        final RecordKey key = entry.getKey();
        Map<String, String> violations = violationsMap.get(key.id);
        assert violations != null;
        if (violations == NULL_VIOLATIONS) {
          RecordJsoImpl masterRecord = master.records.get(key);
          assert masterRecord != null;
          masterRecord.merge(entry.getValue());
          toRemove.add(key);
          master.eventBus.fireEvent(masterRecord.getSchema().createChangeEvent(
              masterRecord, WriteOperation.UPDATE));
          syncResults.add(new SyncResultImpl(masterRecord, null, null));
        } else {
          // do not change the masterRecord or fire event
          syncResults.add(new SyncResultImpl(entry.getValue(), violations, null));
        }
      }
    }
    processToRemove(toRemove, WriteOperation.UPDATE);
    return syncResults;
  }

  public Record create(String token) {
    if (used) {
      throw new IllegalStateException(
          "create can only be called on an un-used DeltaValueStore");
    }
    String futureId = futureIdGenerator.getFutureId();

    RecordSchema<? extends Record> schema = recordToTypeMap.getType(token);
    RecordJsoImpl newRecord = RecordJsoImpl.create(futureId, INITIAL_VERSION,
        schema);
    RecordKey recordKey = new RecordKey(newRecord);
    assert operations.get(recordKey) == null;
    operations.put(recordKey, WriteOperation.CREATE);
    creates.put(recordKey, newRecord);
    return schema.create(newRecord);
  }

  public void delete(Record record) {
    checkArgumentsAndState(record, "delete");
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
    checkArgumentsAndState(record, "set");
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
    if (operations.size() > 1) {
      throw new UnsupportedOperationException(
          "Currently, only one entity can be saved/persisted at a time");
      /*
       * TODO: Short-term todo is to allow multiple entities belonging to the
       * same class to be persisted at the same time. The client side support
       * for this operation is already in place. On the server side, this will
       * entail persisting all entities as part of a single transaction. In
       * particular, the transaction should fail if the validation check on any
       * of the entities fail.
       * 
       * Multiple entities belonging to different records can not be persisted
       * at present due to the appEngine limitation of a transaction not being
       * allowed to span multiple entity groups.
       */
    }
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

  private void checkArgumentsAndState(Record record, String methodName) {
    if (used) {
      throw new IllegalStateException(
          methodName + " can only be called on an un-used DeltaValueStore");
    }
    if (!(record instanceof RecordImpl)) {
      throw new IllegalArgumentException(record + " + must be an instance of "
          + RecordImpl.class);
    }
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

  private Map<String, Map<String, String>> getViolationsMap(
      JsArray<ReturnRecord> records) {
    Map<String, Map<String, String>> violationsMap = new HashMap<String, Map<String, String>>();
    int length = records.length();
    for (int i = 0; i < length; i++) {
      ReturnRecord record = records.get(i);
      HashMap<String, String> violations = null;
      if (record.hasViolations()) {
        violations = new HashMap<String, String>();
        record.fillViolations(violations);
      } else {
        violations = NULL_VIOLATIONS;
      }
      violationsMap.put(record.getId(), violations);
    }
    return violationsMap;
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

  private void processToRemove(Set<RecordKey> toRemove,
      WriteOperation writeOperation) {
    for (RecordKey recordKey : toRemove) {
      operations.remove(recordKey);
      switch (writeOperation) {
        case CREATE:
          creates.remove(recordKey);
          break;
        case DELETE:
          deletes.remove(recordKey);
          break;
        case UPDATE:
          updates.remove(recordKey);
          break;
      }
    }
  }
}
