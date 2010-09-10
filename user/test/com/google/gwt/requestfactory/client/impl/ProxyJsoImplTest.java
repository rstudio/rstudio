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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.client.SimpleRequestFactoryInstance;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Tests for {@link ProxyJsoImpl}.
 */
public class ProxyJsoImplTest extends GWTTestCase {

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
        "version", "1", "intId", "4", "shortField", "5", "byteField", "6",
        "floatField", "12.3456789", "doubleField", "12345.6789", "boolField",
        "false", "otherBoolField", "true"};

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

  private static final String ID_VERSION_JSON = "{\"id\":\"42\",\"version\":1}";
  private static final String ID_VERSION_JSON2 = "{\"id\":\"43\",\"version\":1}";

  static ProxyJsoImpl getMinimalJso() {
    return ProxyJsoImpl.create(42L, 1, SimpleRequestFactoryInstance.schema(),
        SimpleRequestFactoryInstance.impl());
  }

  static ProxyJsoImpl getPopulatedJso() {
    ProxyJsoImpl jso = getMinimalJso();
    jso.set(SimpleFooProxy.userName, "bovik");
    jso.set(SimpleFooProxy.password, "bovik");
    jso.set(SimpleFooProxy.intId, 4);
    jso.set(SimpleFooProxy.created, new Date(400));
    return jso;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testEmptyCopy() {
    ProxyJsoImpl emptyCopy = ProxyJsoImpl.emptyCopy(getPopulatedJso());
    testMinimalJso(emptyCopy);
  }

  public void testFromJson() {
    testMinimalJso(eval(ID_VERSION_JSON));
    testPopulatedJso(eval(ALL_PROPERTIES_JSON));
  }

  /*
   * TODO(amitmanjhi): test null values.
   */
  public void testHasChanged() {
    ProxyJsoImpl minimalJso = getMinimalJso();
    ProxyJsoImpl populatedJso = getPopulatedJso();
    assertFalse(minimalJso.hasChanged(minimalJso));
    assertFalse(populatedJso.hasChanged(populatedJso));
    
    assertTrue(minimalJso.hasChanged(populatedJso));
    assertFalse(populatedJso.hasChanged(minimalJso));
    
    ProxyJsoImpl minimalJsoCopy = getMinimalJso();
    assertFalse(minimalJso.hasChanged(minimalJsoCopy));
    minimalJsoCopy.set(SimpleFooProxy.id, minimalJso.getId() + 42);
    assertTrue(minimalJso.hasChanged(minimalJsoCopy));
  }

  public void testIsEmpty() {
    assertTrue(getMinimalJso().isEmpty());
    assertFalse(getPopulatedJso().isEmpty());
  }

  public void testSet() {
    ProxyJsoImpl jso = getMinimalJso();

    jso.set(SimpleFooProxy.userName, "bovik");
    jso.set(SimpleFooProxy.password, "bovik");

    jso.set(SimpleFooProxy.charField, 'c');

    jso.set(SimpleFooProxy.longField, 1234567890L);
    jso.set(SimpleFooProxy.bigDecimalField, new BigDecimal(
        "12345678901234.5678901234567890"));
    jso.set(SimpleFooProxy.bigIntField, new BigInteger(
        "123456789012345678901234567890"));

    jso.set(SimpleFooProxy.intId, 4);
    jso.set(SimpleFooProxy.shortField, (short) 5);
    jso.set(SimpleFooProxy.byteField, (byte) 6);

    jso.set(SimpleFooProxy.created, new Date(400));

    jso.set(SimpleFooProxy.doubleField, 12345.6789);
    jso.set(SimpleFooProxy.floatField, 12.3456789f);

    jso.set(SimpleFooProxy.boolField, false);
    jso.set(SimpleFooProxy.otherBoolField, true);

    testPopulatedJso(jso);
  }

  public void testToJson() {
    assertEquals(ID_VERSION_JSON, getMinimalJso().toJson());
  }

  private ProxyJsoImpl eval(String json) {
    JavaScriptObject rawJso = jsEval(json);
    return ProxyJsoImpl.create(rawJso, SimpleRequestFactoryInstance.schema(),
        SimpleRequestFactoryInstance.impl());
  }

  private native JavaScriptObject jsEval(String json) /*-{
    eval("xyz=" + json);
    return xyz;
  }-*/;

  private void testEmptyJso(JavaScriptObject rawJso) {
    ProxyJsoImpl jso = ProxyJsoImpl.create(rawJso, null, null);
    assertFalse(jso.isDefined(SimpleFooProxy.id.getName()));
    assertFalse(jso.isDefined(SimpleFooProxy.version.getName()));
    assertEquals("{}", jso.toJson());
    testSchema(jso);
  }

  private void testMinimalJso(ProxyJsoImpl jso) {
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
    assertEquals(null, jso.get(SimpleFooProxy.longField));
    assertEquals(null, jso.get(SimpleFooProxy.enumField));
    testSchema(jso);
  }

  private void testPopulatedJso(ProxyJsoImpl jso) {
    for (String property : new String[] {
        "userName", "password", "charField", "longField", "bigDecimalField",
        "bigIntField", "intId", "shortField", "byteField", "created",
        "doubleField", "floatField", "boolField", "otherBoolField", "id",
        "version"}) {
      assertTrue("Expect " + property + " to be defined",
          jso.isDefined(property));
    }
    assertEquals("bovik", jso.get(SimpleFooProxy.userName));
    assertEquals("bovik", jso.get(SimpleFooProxy.password));

    assertEquals(new Character('c'), jso.get(SimpleFooProxy.charField));

    assertEquals(new Long(1234567890L), jso.get(SimpleFooProxy.longField));
    assertEquals(new BigDecimal("12345678901234.5678901234567890"),
        jso.get(SimpleFooProxy.bigDecimalField));
    assertEquals(new BigInteger("123456789012345678901234567890"),
        jso.get(SimpleFooProxy.bigIntField));

    assertEquals(Integer.valueOf(4), jso.get(SimpleFooProxy.intId));
    assertEquals(Short.valueOf((short) 5), jso.get(SimpleFooProxy.shortField));
    assertEquals(Byte.valueOf((byte) 6), jso.get(SimpleFooProxy.byteField));

    assertEquals(new Date(400), jso.get(SimpleFooProxy.created));
    assertEquals(Double.valueOf(12345.6789),
        jso.get(SimpleFooProxy.doubleField));

    int expected = (int) (Float.valueOf(12.3456789f) * 1000);
    int actual = (int) (jso.get(SimpleFooProxy.floatField) * 1000);
    assertEquals(expected, actual);

    assertFalse(jso.get(SimpleFooProxy.boolField));
    assertTrue(jso.get(SimpleFooProxy.otherBoolField));

    assertEquals((Long) 42L, jso.getId());
    assertEquals(new Integer(1), jso.getVersion());

    testSchema(jso);
  }

  private void testSchema(ProxyJsoImpl jso) {
    assertEquals(SimpleRequestFactoryInstance.schema(), jso.getSchema());
  }
}
