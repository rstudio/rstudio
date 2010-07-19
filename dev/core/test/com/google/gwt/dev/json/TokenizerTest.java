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

import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonException;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.json.JsonValue;
import com.google.gwt.dev.json.Tokenizer;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * Internal tests of {@link Tokenizer}.
 */
public class TokenizerTest extends TestCase {
  private static Tokenizer tokenizerFor(String data) {
    return new Tokenizer(new StringReader(data));
  }

  /**
   * Tests {@link Tokenizer#back(char)}.
   * 
   * @throws IOException
   */
  public void testBack() throws IOException {
    final Tokenizer tokenizer = tokenizerFor("ab c");
    assertEquals('a', tokenizer.next());
    tokenizer.back('a');
    assertEquals('a', tokenizer.next());
    assertEquals('b', tokenizer.next());
    assertEquals('c', tokenizer.nextNonWhitespace());
    tokenizer.back('c');
    assertEquals('c', tokenizer.nextNonWhitespace());
  }

  /**
   * Tests {@link Tokenizer#next()}.
   * 
   * @throws IOException
   */
  public void testNext() throws IOException {
    final Tokenizer tokenizer = tokenizerFor("abc");
    assertEquals('a', tokenizer.next());
    assertEquals('b', tokenizer.next());
    assertEquals('c', tokenizer.next());
    assertEquals(-1, tokenizer.next());
    assertEquals(-1, tokenizer.next());
  }

  /**
   * Tests {@link Tokenizer#nextNonWhitespace()}.
   * 
   * @throws IOException
   */
  public void testNextNonWhite() throws IOException {
    final Tokenizer tokenizer = tokenizerFor("  a\n\tbc   ");
    assertEquals('a', tokenizer.nextNonWhitespace());
    assertEquals('b', tokenizer.nextNonWhitespace());
    assertEquals('c', tokenizer.nextNonWhitespace());
    assertEquals(-1, tokenizer.nextNonWhitespace());
    assertEquals(-1, tokenizer.nextNonWhitespace());
  }

  /**
   * Tests {@link Tokenizer#nextString()}.
   * 
   * @throws IOException
   * @throws JsonException
   */
  public void testNextString() throws IOException, JsonException {
    assertEquals("json json", tokenizerFor("\"json json\"").nextString());
    assertEquals("json\n\t\rjson",
        tokenizerFor("\"json\\n\\t\\rjson\"").nextString());
    assertEquals("\u8734", tokenizerFor("\"\\u8734\"").nextString());
  }

  /**
   * Tests {@link Tokenizer#nextValue()} for array values.
   * 
   * @throws IOException
   * @throws JsonException
   */
  public void testNextValueForArrays() throws IOException, JsonException {
    final JsonValue a = tokenizerFor("[]").nextValue();
    assertEquals(0, a.asArray().getLength());

    final JsonValue b = tokenizerFor(
        "[1, 1.76, \"pug butt\", true, false, null, {}, []]").nextValue();
    final JsonArray ba = b.asArray();
    assertEquals(1, ba.get(0).asNumber().getInteger());
    assertEquals(1.76, ba.get(1).asNumber().getDecimal(), 0.001);
    assertEquals("pug butt", ba.get(2).asString().getString());
    assertTrue(ba.get(3).asBoolean().getBoolean());
    assertFalse(ba.get(4).asBoolean().getBoolean());
    assertEquals(JsonValue.NULL, ba.get(5));
    assertTrue(ba.get(6).asObject().isEmpty());
    assertEquals(0, ba.get(7).asArray().getLength());
  }

  /**
   * Tests {@link Tokenizer#nextValue()} for literals.
   * 
   * @throws IOException
   * @throws JsonException
   */
  public void testNextValueForLiterals() throws IOException, JsonException {
    final JsonValue a = tokenizerFor("null").nextValue();
    assertEquals(JsonValue.NULL, a);

    final JsonValue b = tokenizerFor("true").nextValue();
    assertTrue(b.asBoolean().getBoolean());

    final JsonValue c = tokenizerFor("false").nextValue();
    assertFalse(c.asBoolean().getBoolean());

    final JsonValue d = tokenizerFor("\"kellegous\u8738\"").nextValue();
    assertEquals("kellegous\u8738", d.asString().getString());

    final JsonValue e = tokenizerFor("0.2").nextValue();
    assertEquals(0.2, e.asNumber().getDecimal(), 0.001);

    final JsonValue f = tokenizerFor("420").nextValue();
    assertEquals(420, f.asNumber().getInteger());

    final JsonValue g = tokenizerFor("6.23e-20").nextValue();
    assertEquals(6.23e-20, g.asNumber().getDecimal(), 1e-20);

    boolean didThrow = false;
    try {
      tokenizerFor("naked").nextValue();
    } catch (JsonException x) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  /**
   * Tests {@link Tokenizer#nextValue()} for object values.
   * 
   * @throws IOException
   * @throws JsonException
   */
  public void testNextValueForObjects() throws IOException, JsonException {
    final JsonValue a = tokenizerFor("{}").nextValue();
    assertTrue(a.asObject().isEmpty());

    final JsonValue b = tokenizerFor("{\"a\" : 1}").nextValue();
    assertEquals(1, b.asObject().get("a").asNumber().getInteger());

    final JsonValue c = tokenizerFor(
        "{\"a b\":12.3e10,\"b a\":\"\u8387\",\n\"c\" :   null,\"d\":true,\"e\" : false, \"f\":{\n}}").nextValue();
    final JsonObject co = c.asObject();
    assertNotNull(co);
    assertEquals(12.3e10, co.get("a b").asNumber().getDecimal(), 0.001);
    assertEquals("\u8387", co.get("b a").asString().getString());
    assertEquals(JsonValue.NULL, co.get("c"));
    assertTrue(co.get("d").asBoolean().getBoolean());
    assertFalse(co.get("e").asBoolean().getBoolean());
    assertTrue(co.get("f").asObject().isEmpty());
  }
}
