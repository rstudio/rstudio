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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.valuestore.shared.SimpleFooRecord;

import java.util.Date;

/**
 * Tests for {@link RecordJsoImpl}.
 */
public class RecordJsoImplTest extends GWTTestCase {

  private static final String ALL_PROPERTIES_JSON = "{\"id\":\"42\",\"version\":1,\"userName\":\"bovik\",\"password\":\"bovik\",\"intId\":4,\"created\":\"400\"}";
  private static final String EMPTY_JSON = "{}";
  private static final String ID_VERSION_JSON = "{\"id\":\"42\",\"version\":1}";
  private static final String ID_VERSION_JSON2 = "{\"id\":\"43\",\"version\":1}";

  private static final boolean SCHEMA_ABSENT = false;
  private static final boolean SCHEMA_PRESENT = true;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactoryTest";
  }

  public void testEmptyCopy() {
    RecordJsoImpl emptyCopy = RecordJsoImpl.emptyCopy(new RecordImpl(
        getPopulatedJso()));
    testMinimalJso(emptyCopy, SCHEMA_PRESENT);
  }

  public void testFromJson() {
    testEmptyJso(RecordJsoImpl.fromJson(EMPTY_JSON), SCHEMA_ABSENT);
    testMinimalJso(RecordJsoImpl.fromJson(ID_VERSION_JSON), SCHEMA_ABSENT);
    testPopulatedJso(RecordJsoImpl.fromJson(ALL_PROPERTIES_JSON), SCHEMA_ABSENT);
  }

  public void testFromJsonArray() {
    String jsonString = "[" + ID_VERSION_JSON + "," + ID_VERSION_JSON2 + "]";
    JsArray<RecordJsoImpl> jsArray = RecordJsoImpl.arrayFromJson(jsonString);
    assertEquals(2, jsArray.length());
  }

  public void testIsEmpty() {
    try {
      getEmptyJso().isEmpty();
      fail("A Runtime Exception should be thrown because schema is not defined");
    } catch (RuntimeException ex) {
      // NullPointerException in dev mode, JavaScriptException in prod mode
      // expected because schema is not defined.
    }
    assertTrue(getMinimalJso().isEmpty());
    assertFalse(getPopulatedJso().isEmpty());
  }

  public void testSet() {
    RecordJsoImpl jso = getMinimalJso();
    jso.set(SimpleFooRecord.userName, "bovik");
    jso.set(SimpleFooRecord.password, "bovik");
    jso.set(SimpleFooRecord.intId, 4);
    jso.set(SimpleFooRecord.created, new Date(400));
    testPopulatedJso(jso, SCHEMA_PRESENT);
  }

  public void testToJson() {
    assertEquals(ID_VERSION_JSON, getMinimalJso().toJson());
  }

  public void testToJsonIdVersion() {
    assertEquals(ID_VERSION_JSON, getPopulatedJso().toJsonIdVersion());
    assertEquals(ID_VERSION_JSON, getMinimalJso().toJsonIdVersion());
  }

  private RecordJsoImpl getEmptyJso() {
    return RecordJsoImpl.create();
  }

  private RecordJsoImpl getMinimalJso() {
    return RecordJsoImpl.create(42L, 1, SimpleFooRecordImpl.SCHEMA);
  }

  private RecordJsoImpl getPopulatedJso() {
    RecordJsoImpl jso = getMinimalJso();
    jso.set(SimpleFooRecord.userName, "bovik");
    jso.set(SimpleFooRecord.password, "bovik");
    jso.set(SimpleFooRecord.intId, 4);
    jso.set(SimpleFooRecord.created, new Date(400));
    return jso;
  }

  private void testEmptyJso(RecordJsoImpl jso, boolean schemaPresent) {
    assertFalse(jso.isDefined(SimpleFooRecord.id.getName()));
    assertFalse(jso.isDefined(SimpleFooRecord.version.getName()));
    assertEquals("{}", jso.toJson());
    testSchema(jso, schemaPresent);
  }

  private void testMinimalJso(RecordJsoImpl jso, boolean schemaPresent) {
    for (String property : new String[]{"id", "version"}) {
      assertTrue(jso.isDefined(property));
    }
    for (String property : new String[]{
        "created", "intId", "userName", "password"}) {
      assertFalse(jso.isDefined(property));
      assertNull(jso.get(property));
    }
    assertEquals((Long) 42L, jso.getId());
    assertEquals(new Integer(1), jso.getVersion());
    assertEquals(null, jso.get(SimpleFooRecord.longField));
    assertEquals(null, jso.get(SimpleFooRecord.enumField));
    testSchema(jso, schemaPresent);
  }

  private void testPopulatedJso(RecordJsoImpl jso, boolean schemaPresent) {
    for (String property : new String[]{
        "id", "version", "created", "intId", "userName", "password"}) {
      assertTrue(jso.isDefined(property));
    }
    assertEquals((Long) 42L, jso.getId());
    assertEquals(new Integer(1), jso.getVersion());
    assertEquals("bovik", jso.get(SimpleFooRecord.userName));
    assertEquals("bovik", jso.get(SimpleFooRecord.password));
    assertEquals(new Integer(4), jso.get(SimpleFooRecord.intId));
    assertEquals(new Date(400), jso.get(SimpleFooRecord.created));
    testSchema(jso, schemaPresent);
  }

  private void testSchema(RecordJsoImpl jso, boolean schemaPresent) {
    if (schemaPresent) {
      assertEquals(SimpleFooRecordImpl.SCHEMA, jso.getSchema());
    } else {
      assertNull(jso.getSchema());
    }
  }

}
