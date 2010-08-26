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
import com.google.gwt.requestfactory.client.SimpleRequestFactoryInstance;
import com.google.gwt.requestfactory.shared.SimpleFooRecord;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Tests for {@link RecordJsoImpl}.
 */
public class RecordJsoImplTest extends GWTTestCase {

  private static final String ALL_PROPERTIES_JSON;
  static {
    StringBuilder b = new StringBuilder();
    b.append("{");

    String[] stringBits = new String[] {
        "userName", "bovik", "password", "bovik", "charField", "c",
        "longField", "1234567890", "bigDecimalField",
        "12345678901234.5678901234567890", "bigIntField",
        "123456789012345678901234567890", "created", "400", "id", "42"};

    String[] literalBits = new String[] {
        "version", "1", "intId", "4", "shortField", "5",
        "byteField", "6", "floatField", "12.3456789", "doubleField",
        "12345.6789", "boolField", "false", "otherBoolField", "true"};

    boolean isFirst = true;
    boolean isLabel = true;
    for (String s : stringBits) {

      if (isLabel) {
        if (isFirst) {
          isFirst = false;
        } else {
          b.append(",");
        }
        b.append("\"").append(s).append("\":");
      } else {
        b.append("\"").append(s).append("\"");
      }
      isLabel = !isLabel;
    }

    for (String s : literalBits) {
      if (isLabel) {
        b.append(",");
        b.append("\"").append(s).append("\":");
      } else {
        b.append(s);
      }
      isLabel = !isLabel;
    }

    b.append("}");

    ALL_PROPERTIES_JSON = b.toString();
  }

  private static final String EMPTY_JSON = "{}";
  private static final String ID_VERSION_JSON = "{\"id\":\"42\",\"version\":1}";
  private static final String ID_VERSION_JSON2 = "{\"id\":\"43\",\"version\":1}";

  private static final boolean SCHEMA_ABSENT = false;
  private static final boolean SCHEMA_PRESENT = true;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testEmptyCopy() {
    RecordJsoImpl emptyCopy = RecordJsoImpl.emptyCopy(getPopulatedJso());
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

    jso.set(SimpleFooRecord.charField, 'c');

    jso.set(SimpleFooRecord.longField, 1234567890L);
    jso.set(SimpleFooRecord.bigDecimalField, new BigDecimal(
        "12345678901234.5678901234567890"));
    jso.set(SimpleFooRecord.bigIntField, new BigInteger(
        "123456789012345678901234567890"));

    jso.set(SimpleFooRecord.intId, 4);
    jso.set(SimpleFooRecord.shortField, (short) 5);
    jso.set(SimpleFooRecord.byteField, (byte) 6);

    jso.set(SimpleFooRecord.created, new Date(400));

    jso.set(SimpleFooRecord.doubleField, 12345.6789);
    jso.set(SimpleFooRecord.floatField, 12.3456789f);

    jso.set(SimpleFooRecord.boolField, false);
    jso.set(SimpleFooRecord.otherBoolField, true);

    testPopulatedJso(jso, SCHEMA_PRESENT);
  }

  public void testToJson() {
    assertEquals(ID_VERSION_JSON, getMinimalJso().toJson());
  }

  private RecordJsoImpl getEmptyJso() {
    return RecordJsoImpl.create();
  }

  private RecordJsoImpl getMinimalJso() {
    return RecordJsoImpl.create(42L, 1, SimpleRequestFactoryInstance.schema());
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
    for (String property : new String[] {"id", "version"}) {
      assertTrue(jso.isDefined(property));
    }
    for (String property : new String[] {
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
    for (String property : new String[] {
        "userName", "password", "charField", "longField", "bigDecimalField",
        "bigIntField", "intId", "shortField", "byteField", "created",
        "doubleField", "floatField", "boolField", "otherBoolField", "id",
        "version"}) {
      assertTrue("Expect " + property + " to be defined",
          jso.isDefined(property));
    }
    assertEquals("bovik", jso.get(SimpleFooRecord.userName));
    assertEquals("bovik", jso.get(SimpleFooRecord.password));

    assertEquals(new Character('c'), jso.get(SimpleFooRecord.charField));

    assertEquals(new Long(1234567890L), jso.get(SimpleFooRecord.longField));
    assertEquals(new BigDecimal("12345678901234.5678901234567890"),
        jso.get(SimpleFooRecord.bigDecimalField));
    assertEquals(new BigInteger("123456789012345678901234567890"),
        jso.get(SimpleFooRecord.bigIntField));

    assertEquals(Integer.valueOf(4), jso.get(SimpleFooRecord.intId));
    assertEquals(Short.valueOf((short) 5), jso.get(SimpleFooRecord.shortField));
    assertEquals(Byte.valueOf((byte) 6), jso.get(SimpleFooRecord.byteField));

    assertEquals(new Date(400), jso.get(SimpleFooRecord.created));
    assertEquals(Double.valueOf(12345.6789), jso.get(SimpleFooRecord.doubleField));
    
    int expected = (int) (Float.valueOf(12.3456789f) * 1000);
    int actual = (int) (jso.get(SimpleFooRecord.floatField) * 1000);
    assertEquals(expected, actual);

    assertFalse(jso.get(SimpleFooRecord.boolField));
    assertTrue(jso.get(SimpleFooRecord.otherBoolField));

    assertEquals((Long) 42L, jso.getId());
    assertEquals(new Integer(1), jso.getVersion());

    testSchema(jso, schemaPresent);
  }

  private void testSchema(RecordJsoImpl jso, boolean schemaPresent) {
    if (schemaPresent) {
      assertEquals(SimpleRequestFactoryInstance.schema(), jso.getSchema());
    } else {
      assertNull(jso.getSchema());
    }
  }

}
