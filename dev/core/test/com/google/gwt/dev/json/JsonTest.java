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
package com.google.gwt.dev.json;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests the JSON library.
 */
public class JsonTest extends TestCase {
  private static String toJsonString(JsonValue value) throws IOException {
    final StringWriter writer = new StringWriter();
    value.write(writer);
    return writer.toString();
  }

  /**
   * Tests that acessing invalid properites yields {@link JsonValue#NULL}.
   */
  public void testNullAccess() {
    final JsonValue nullValue = JsonObject.create().get("invalid_property");
    assertFalse(nullValue.isArray());
    assertFalse(nullValue.isBoolean());
    assertFalse(nullValue.isNumber());
    assertFalse(nullValue.isObject());
    assertFalse(nullValue.isString());
    assertEquals(nullValue, JsonValue.NULL);
  }

  /**
   * Tests updating elements in a {@link JsonArray}.
   */
  public void testWriteArray() throws IOException, JsonException {
    final JsonArray a = new JsonArray();
    a.add(true);
    a.add("foo");
    a.add(230);
    a.add(1.0);
    a.add(JsonArray.create());
    a.add(JsonObject.create());

    final String json = toJsonString(a);
    assertEquals("[true,\"foo\",230,1.0,[],{}]", json);

    final JsonArray b = JsonArray.parse(new StringReader(json));
    assertEquals(6, b.getLength());

    assertTrue(b.get(0).asBoolean().getBoolean());
    assertEquals("foo", b.get(1).asString().getString());
    assertEquals(230, b.get(2).asNumber().getInteger());
    assertEquals(1.0, b.get(3).asNumber().getDecimal(), 0.001);
    assertEquals(0, b.get(4).asArray().getLength());
    assertTrue(b.get(5).asObject().isEmpty());
  }

  /**
   * Tests updating properties in a {@link JsonObject}.
   */
  public void testWriteObject() throws IOException, JsonException {
    final JsonObject a = new JsonObject();
    a.put("a", 3);
    a.put("b", 120.456);
    a.put("c", "json\n\r\f\t\b\u8730");
    a.put("d", new JsonObject());
    a.put("e", new JsonArray());
    a.put("f", true);
    a.put("g", false);

    final String json = toJsonString(a);
    final JsonObject b = JsonObject.parse(new StringReader(json));

    assertEquals(3, b.get("a").asNumber().getInteger());
    assertEquals(120.456, b.get("b").asNumber().getDecimal(), 0.0001);
    assertEquals("json\n\r\f\t\b\u8730", b.get("c").asString().getString());
    assertTrue(b.get("d").asObject().isEmpty());
    assertEquals(0, b.get("e").asArray().getLength());
    assertTrue(b.get("f").asBoolean().getBoolean());
    assertFalse(b.get("g").asBoolean().getBoolean());
  }

  /**
   * Tests {@link JsonValue#copyDeeply()}.
   */
  public void testCopyDeeply() {
    final JsonObject a = new JsonObject();
    a.put("a", 3);
    a.put("b", 120.456);
    a.put("c", "json\n\r\f\t\b\u8730");
    a.put("d", new JsonObject());
    a.put("e", new JsonArray());
    a.put("f", true);
    a.put("g", JsonValue.NULL);

    // Get JsonValues for all of a's properties.
    final JsonNumber aa = a.get("a").asNumber();
    final JsonNumber ab = a.get("b").asNumber();
    final JsonString ac = a.get("c").asString();
    final JsonObject ad = a.get("d").asObject();
    final JsonArray ae = a.get("e").asArray();

    // Copy a and get references to all the new JsonValues.
    final JsonObject b = a.copyDeeply();
    final JsonNumber ba = b.get("a").asNumber();
    final JsonNumber bb = b.get("b").asNumber();
    final JsonString bc = b.get("c").asString();
    final JsonObject bd = b.get("d").asObject();
    final JsonArray be = b.get("e").asArray();
    final JsonBoolean bf = b.get("f").asBoolean();
    final JsonValue bg = b.get("g");

    // Test non-interned types.
    // Integer
    assertEquals(3, ba.getInteger());
    assertNotSame(aa, ba);

    // Decimal
    assertEquals(120.456, bb.getDecimal(), 0.0001);
    assertNotSame(ab, bb);

    // String
    assertEquals("json\n\r\f\t\b\u8730", bc.getString());
    assertNotSame(ac, bc);

    // Object
    assertTrue(bd.isEmpty());
    assertNotSame(ad, bd);

    // Array
    assertEquals(0, be.getLength());
    assertNotSame(ae, be);


    // Test interned types.
    // Boolean
    assertTrue(bf.getBoolean());

    // NULL
    assertEquals(bg, JsonValue.NULL);
  }
}
