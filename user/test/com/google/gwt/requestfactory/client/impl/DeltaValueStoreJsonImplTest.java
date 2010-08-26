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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.SimpleFooRecord;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.Date;

/**
 * Tests for {@link DeltaValueStoreJsonImpl}.
 */
public class DeltaValueStoreJsonImplTest extends GWTTestCase {

  /*
   * sub-classed it here so that the protected constructor of {@link RecordImpl}
   * can remain as such.
   */
  private class MyRecordImpl extends RecordImpl {

    protected MyRecordImpl(RecordJsoImpl record) {
      super(record, false);
    }
  }

  final RecordToTypeMap typeMap = new RecordToTypeMap() {
    @SuppressWarnings("unchecked")
    public <R extends Record> RecordSchema<R> getType(Class<R> recordClass) {
      if (recordClass.equals(SimpleFooRecord.class)) {
        return (RecordSchema<R>) SimpleFooRecordImpl.SCHEMA;
      }
      throw new IllegalArgumentException("Unknown token " + recordClass);
    }

     public RecordSchema<? extends Record> getType(
       String recordClass) {
      if (recordClass.equals("simple-foo-class-token")) {
        return SimpleFooRecordImpl.SCHEMA;
      }
      throw new IllegalArgumentException("Unknown token " + recordClass);
    }

    public String getClassToken(Class<?> recordClass) {
      if (recordClass.equals(SimpleFooRecord.class)) {
        return "simple-foo-class-token";
      }
      throw new IllegalArgumentException("Unknown token " + recordClass);
    }
  };
  ValueStoreJsonImpl valueStore = null;
  RequestFactoryJsonImpl requestFactory = null;

  RecordJsoImpl jso = null;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  @Override
  public void gwtSetUp() {
    valueStore = new ValueStoreJsonImpl();
    requestFactory = new RequestFactoryJsonImpl() {

      public <R extends Record> R create(Class<R> token) {
        return create(token, typeMap);
      }

      @Override
      public RecordSchema<?> getSchema(String token) {
        return typeMap.getType(token);
      }

      @Override
      public void init(EventBus eventBus) {
        // ignore.
      }

      public LoggingRequest loggingRequest() {
        return null; // ignore
      }

      public Class<? extends Record> getClass(String token) {
        throw new UnsupportedOperationException("Auto-generated method stub");
      }

      public Record getProxy(String token) {
        throw new UnsupportedOperationException("Auto-generated method stub");
      }

      public String getToken(Class<? extends Record> clazz) {
        throw new UnsupportedOperationException("Auto-generated method stub");
      }

      public String getToken(Record proxy) {
        throw new UnsupportedOperationException("Auto-generated method stub");
      }
    };

    // add a record
    jso = RecordJsoImpl.fromJson("{}");
    jso.set(SimpleFooRecord.id, 42L);
    jso.set(SimpleFooRecord.version, 1);
    jso.set(SimpleFooRecord.userName, "bovik");
    jso.set(SimpleFooRecord.password, "bovik");
    jso.set(SimpleFooRecord.intId, 4);
    jso.set(SimpleFooRecord.created, new Date());
    jso.setSchema(SimpleFooRecordImpl.SCHEMA);
    valueStore.setRecord(jso, requestFactory);
  }

  public void testCreate() {
    Record created = requestFactory.create(SimpleFooRecord.class);
    assertNotNull(created.getId());
    assertNotNull(created.getVersion());

    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);
    String json = deltaValueStore.toJson();
    JSONObject jsonObject = (JSONObject) JSONParser.parseLenient(json);
    assertFalse(jsonObject.containsKey(WriteOperation.CREATE.name()));
  }

  public void testCreateWithSet() {
    Record created = requestFactory.create(SimpleFooRecord.class);
    assertNotNull(created.getId());
    assertNotNull(created.getVersion());

    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);
    // DVS does not know about the created entity.
    assertFalse(deltaValueStore.isChanged());
    deltaValueStore.set(SimpleFooRecord.userName, created, "harry");
    assertTrue(deltaValueStore.isChanged());
    testAndGetChangeRecord(deltaValueStore.toJson(), WriteOperation.CREATE);
  }

  public void testCreateUpdate() {
    Record created = requestFactory.create(SimpleFooRecord.class);
    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);
    // DVS does not know about the created entity.
    assertFalse(deltaValueStore.isChanged());
    deltaValueStore.set(SimpleFooRecord.userName, created, "harry");
    assertTrue(deltaValueStore.isChanged());
    JSONObject changeRecord = testAndGetChangeRecord(deltaValueStore.toJson(),
        WriteOperation.CREATE);
    assertEquals(
        "harry",
        changeRecord.get(SimpleFooRecord.userName.getName()).isString().stringValue());
  }

  public void testOperationAfterJson() {
    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "newHarry");
    assertTrue(deltaValueStore.isChanged());

    deltaValueStore.toJson();

    try {
      deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
          "harry");
      fail("Modifying DeltaValueStore after calling toJson should throw a RuntimeException");
    } catch (RuntimeException ex) {
      // expected.
    }

    deltaValueStore.clearUsed();
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "harry");
  }

  public void testSeparateIds() {
    RecordImpl createRecord = (RecordImpl) requestFactory.create(SimpleFooRecord.class);
    assertTrue(createRecord.isFuture());
    Long futureId = createRecord.getId();

    RecordImpl mockRecord = new RecordImpl(RecordJsoImpl.create(futureId, 1,
        SimpleFooRecordImpl.SCHEMA), RequestFactoryJsonImpl.NOT_FUTURE);
    valueStore.setRecord(mockRecord.asJso(), requestFactory); // marked as non-future..
    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);

    deltaValueStore.set(SimpleFooRecord.userName, createRecord, "harry");
    deltaValueStore.set(SimpleFooRecord.userName, mockRecord, "bovik");
    assertTrue(deltaValueStore.isChanged());
    String jsonString = deltaValueStore.toJson();
    JSONObject jsonObject = (JSONObject) JSONParser.parseLenient(jsonString);
    assertFalse(jsonObject.containsKey(WriteOperation.DELETE.name()));
    assertTrue(jsonObject.containsKey(WriteOperation.CREATE.name()));
    assertTrue(jsonObject.containsKey(WriteOperation.UPDATE.name()));

    JSONArray createOperationArray = jsonObject.get(
        WriteOperation.CREATE.name()).isArray();
    assertEquals(1, createOperationArray.size());
    assertEquals("harry", createOperationArray.get(0).isObject().get(
        SimpleFooRecord.class.getName()).isObject().get(
        SimpleFooRecord.userName.getName()).isString().stringValue());

    JSONArray updateOperationArray = jsonObject.get(
        WriteOperation.UPDATE.name()).isArray();
    assertEquals(1, updateOperationArray.size());
    assertEquals("bovik", updateOperationArray.get(0).isObject().get(
        SimpleFooRecord.class.getName()).isObject().get(
        SimpleFooRecord.userName.getName()).isString().stringValue());
  }

  public void testUpdate() {
    DeltaValueStoreJsonImpl deltaValueStore = new DeltaValueStoreJsonImpl(
        valueStore, requestFactory);
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "harry");
    assertTrue(deltaValueStore.isChanged());
    JSONObject changeRecord = testAndGetChangeRecord(deltaValueStore.toJson(),
        WriteOperation.UPDATE);
    assertEquals(
        "harry",
        changeRecord.get(SimpleFooRecord.userName.getName()).isString().stringValue());
  }

  private JSONObject testAndGetChangeRecord(String jsonString,
      WriteOperation currentWriteOperation) {
    JSONObject jsonObject = (JSONObject) JSONParser.parseLenient(jsonString);
    for (WriteOperation writeOperation : WriteOperation.values()) {
      if (writeOperation != currentWriteOperation) {
        assertFalse(jsonObject.containsKey(writeOperation.name()));
      } else {
        assertTrue(jsonObject.containsKey(writeOperation.name()));
      }
    }

    JSONArray writeOperationArray = jsonObject.get(currentWriteOperation.name()).isArray();
    assertEquals(1, writeOperationArray.size());

    JSONObject recordWithName = writeOperationArray.get(0).isObject();
    assertEquals(1, recordWithName.size());
    assertTrue(recordWithName.containsKey(SimpleFooRecord.class.getName()));

    JSONObject record = recordWithName.get(SimpleFooRecord.class.getName()).isObject();
    assertTrue(record.containsKey("id"));
    assertTrue(record.containsKey("version"));

    return record;
  }
}
