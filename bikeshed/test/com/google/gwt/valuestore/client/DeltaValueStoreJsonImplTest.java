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

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.WriteOperation;
import com.google.gwt.valuestore.shared.impl.RecordImpl;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;
import com.google.gwt.valuestore.shared.impl.RecordToTypeMap;
import com.google.gwt.valuestore.shared.impl.SimpleFooRecordImpl;

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
      super(record);
    }
  }

  ValueStoreJsonImpl valueStore = null;
  RecordJsoImpl jso = null;

  public String getModuleName() {
    return "com.google.gwt.valuestore.ValueStoreTest";
  }

  @Override
  public void gwtSetUp() {
    valueStore = new ValueStoreJsonImpl(null, new RecordToTypeMap() {
      public RecordSchema<? extends Record> getType(String token) {
        if (token.equals(SimpleFooRecordImpl.TOKEN)) {
          return SimpleFooRecordImpl.SCHEMA;
        }
        throw new IllegalArgumentException("Unknown token " + token);
      }

    });

    // add a record
    jso = RecordJsoImpl.fromJson("{}");
    jso.set(SimpleFooRecord.id, "42");
    jso.set(SimpleFooRecord.version, 1);
    jso.set(SimpleFooRecord.userName, "bovik");
    jso.set(SimpleFooRecord.password, "bovik");
    jso.set(SimpleFooRecord.intId, 4);
    jso.set(SimpleFooRecord.created, new Date());
    jso.setSchema(SimpleFooRecordImpl.SCHEMA);
    valueStore.setRecord(jso);
  }

  public void testCreate() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    Record created = deltaValueStore.create(SimpleFooRecordImpl.TOKEN);
    assertNotNull(created.getId());
    assertNotNull(created.getVersion());

    assertTrue(deltaValueStore.isChanged());
    testAndGetChangeRecord(deltaValueStore.toJson(), WriteOperation.CREATE);
  }

  public void testCreateDelete() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    Record created = deltaValueStore.create(SimpleFooRecordImpl.TOKEN);
    assertTrue(deltaValueStore.isChanged());
    deltaValueStore.delete(created);
    assertFalse(deltaValueStore.isChanged());

    String jsonString = deltaValueStore.toJson();
    assertEquals("{}", jsonString);
  }

  public void testCreateUpdate() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    Record created = deltaValueStore.create(SimpleFooRecordImpl.TOKEN);
    assertTrue(deltaValueStore.isChanged());
    deltaValueStore.set(SimpleFooRecord.userName, created, "harry");
    assertTrue(deltaValueStore.isChanged());
    JSONObject changeRecord = testAndGetChangeRecord(deltaValueStore.toJson(),
        WriteOperation.CREATE);
    changeRecord.get(SimpleFooRecord.userName.getName());
  }

  public void testDelete() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    deltaValueStore.delete(new MyRecordImpl(jso));
    assertTrue(deltaValueStore.isChanged());
    testAndGetChangeRecord(deltaValueStore.toJson(), WriteOperation.DELETE);
  }

  public void testDeleteUpdate() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    deltaValueStore.delete(new MyRecordImpl(jso));
    assertTrue(deltaValueStore.isChanged());

    // update after a delete nullifies the delete.
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "harry");
    assertTrue(deltaValueStore.isChanged());
    JSONObject changeRecord = testAndGetChangeRecord(deltaValueStore.toJson(),
        WriteOperation.UPDATE);
    changeRecord.get(SimpleFooRecord.userName.getName());
  }

  public void testOperationAfterJson() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    deltaValueStore.delete(new MyRecordImpl(jso));
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

  public void testUpdate() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "harry");
    assertTrue(deltaValueStore.isChanged());
    JSONObject changeRecord = testAndGetChangeRecord(deltaValueStore.toJson(),
        WriteOperation.UPDATE);
    changeRecord.get(SimpleFooRecord.userName.getName());
  }

  public void testUpdateDelete() {
    DeltaValueStoreJsonImpl deltaValueStore = valueStore.spawnDeltaView();
    deltaValueStore.set(SimpleFooRecord.userName, new MyRecordImpl(jso),
        "harry");
    assertTrue(deltaValueStore.isChanged());

    // delete after an update nullifies the delete.
    deltaValueStore.delete(new MyRecordImpl(jso));
    assertTrue(deltaValueStore.isChanged());
    testAndGetChangeRecord(deltaValueStore.toJson(), WriteOperation.DELETE);
  }

  public void testValidation() {
    ValueStore dummyValueStore = new ValueStoreJsonImpl(null, null);
    DeltaValueStore deltaValueStore = dummyValueStore.spawnDeltaView();

    try {
      deltaValueStore.addValidation();
      fail("Should throw an UnsupportedOperationException");
    } catch (UnsupportedOperationException ex) {
      // expected
    }
  }

  private JSONObject testAndGetChangeRecord(String jsonString,
      WriteOperation currentWriteOperation) {
    JSONObject jsonObject = (JSONObject) JSONParser.parse(jsonString);
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
    assertTrue(recordWithName.containsKey(SimpleFooRecordImpl.TOKEN));

    JSONObject record = recordWithName.get(SimpleFooRecordImpl.TOKEN).isObject();
    assertTrue(record.containsKey("id"));
    assertTrue(record.containsKey("version"));

    return record;
  }
}
