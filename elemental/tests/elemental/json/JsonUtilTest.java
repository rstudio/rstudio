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
package elemental.json;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.json.impl.JsonUtil;

/**
 * Tests for {@link JsonUtil}
 */
@DoNotRunWith(Platform.Prod)
public class JsonUtilTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  public void testCoercions() {
    // test boolean coercions
    JsonBoolean boolTrue = Json.create(true);
    JsonBoolean boolFalse = Json.create(false);
    // true -> 1, false -> 0
    assertEquals(true, boolTrue.asBoolean());
    assertEquals(false, boolFalse.asBoolean());

    JsonString trueString = Json.create("true");
    JsonString falseString = Json.create("");
    // "" -> false, others true
    assertEquals(true, trueString.asBoolean());
    assertEquals(false, falseString.asBoolean());

    // != 0 -> true, otherwise if 0.0 or -0.0 false
    JsonNumber trueNumber = Json.create(1.0);
    JsonNumber falseNumber = Json.create(0.0);
    JsonNumber falseNumber2 = Json.create(-0.0);
    assertEquals(true, trueNumber.asBoolean());
    assertEquals(false, falseNumber.asBoolean());
    assertEquals(false, falseNumber2.asBoolean());

    // array or object is true
    assertEquals(true, Json.createArray().asBoolean());
    assertEquals(true, Json.createObject().asBoolean());

    // null is false
    assertEquals(false, Json.createNull().asBoolean());

    // test number coercions
    assertEquals(1.0, boolTrue.asNumber());
    assertEquals(0.0, boolFalse.asNumber());

    assertEquals(42.0, Json.create("42").asNumber());
    // non numbers are NaN
    assertTrue(Double.isNaN(trueString.asNumber()));
    // null is 0
    assertEquals(0.0, Json.createNull().asNumber());
    // "" is 0
    assertEquals(0.0, falseString.asNumber());

    // [] -> 0
    assertEquals(0.0, Json.createArray().asNumber());
    // [[42]] -> 42
    JsonArray nested = Json.createArray();
    JsonArray outer = Json.createArray();
    outer.set(0, nested);
    nested.set(0, 42);
    assertEquals(42.0, outer.asNumber());

    // [[42, 45]] -> NaN
    nested.set(1, 45);
    assertTrue(Double.isNaN(outer.asNumber()));

    // object -> NaN
    assertTrue(Double.isNaN(Json.createObject().asNumber()));


    // test string coercions
    assertEquals("true", boolTrue.asString());
    assertEquals("false", boolFalse.asString());
    assertEquals("true", trueString.asString());

    assertEquals("null", Json.createNull().asString());
    assertEquals("42", Json.create(42).asString());

    // [[42, 45], [52, 55]] -> "42, 45, 52, 55"
    JsonArray inner2 = Json.createArray();
    inner2.set(0, 52);
    inner2.set(1, 55);
    outer.set(1, inner2);
    assertEquals("42, 45, 52, 55", outer.asString());

    // object -> [object Object]
    assertEquals("[object Object]", Json.createObject().asString());
  }
  
  public void testEscapeControlChars() {
    String unicodeString = "\u2060Test\ufeffis a test\u17b5";
    assertEquals("\\u00002060Test\\u0000feffis a test\\u000017b5",
        JsonUtil.escapeControlChars(unicodeString));
  }

  public void testIllegalParse() {
    try {
      JsonUtil.parse("{ \"a\": new String() }");
      fail("Expected JsonException to be thrown");
    } catch (JsonException je) {
      // Expected
    }
  }

  public void testLegalParse() {
    JsonValue obj = JsonUtil.parse(
        "{ \"a\":1, \"b\":\"hello\", \"c\": true,"
            + "\"d\": null, \"e\": [1,2,3,4], \"f\": {} }");
    assertNotNull(obj);
  }

  public void testNative() {
    JsonObject obj = Json.createObject();
    obj.put("x", 42);
    Object nativeObj = obj.toNative();

    JsonObject result = nativeMethod(nativeObj);
    assertEquals(43.0, result.get("y").asNumber());
  }

  public void testQuote() {
    String badString = "\bThis\"is\ufeff\ta\\bad\nstring\u2029\u2029";
    assertEquals("\"\\bThis\\\"is\\u0000feff\\ta\\\\bad\\nstring"
        + "\\u00002029\\u00002029\"", JsonUtil.quote(badString));
  }

  public void testStringify() {
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    assertEquals(json, JsonUtil.stringify(JsonUtil.parse(json)));
  }

  public void testStringifyCycle() {
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    JsonObject obj = JsonUtil.parse(json);
    obj.put("cycle", obj);
    try {
      elemental.json.impl.JsonUtil.stringify(obj);
      fail("Expected JsonException for object cycle");
    } catch (JsonException je) {
    }
  }
 
  public void testStringifyIndent() {
    // test string taken from native Chrome window.JSON.stringify
    String json = "{\n" + "  \"a\": 1,\n" + "  \"b\": \"hello\",\n"
        + "  \"c\": true,\n" + "  \"d\": null,\n" + "  \"e\": [\n" + "    1,\n"
        + "    2,\n" + "    3,\n" + "    4\n" + "  ],\n" + "  \"f\": {\n"
        + "    \"x\": 1\n" + "  }\n" + "}";
    assertEquals(json, JsonUtil.stringify(JsonUtil.parse(json), 2));
  }

  public void testStringifyNonCycle() {
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    JsonObject obj = JsonUtil.parse(json);
    JsonObject obj2 = JsonUtil.parse("{\"x\": 1, \"y\":2}");
    obj.put("nocycle", obj2);
    obj.put("nocycle2", obj2);
    try {
      JsonUtil.stringify(obj);
    } catch (JsonException je) {
      fail("JsonException for object cycle when none exists: " + je);
    }
  }

  public void testStringifyOrder() {

    JsonObject obj = Json.instance().createObject();
    obj.put("x", "hello");
    obj.put("a", "world");
    obj.put("2", 21);
    obj.put("1", 42);
    // numbers come first, in ascending order, non-numbers in order of assignment
    assertEquals("{\"1\":42,\"2\":21,\"x\":\"hello\",\"a\":\"world\"}",
        obj.toJson());
  }

  public void testStringifySkipKeys() {
    String expectedJson = "{\"a\":1,\"b\":\"hello\",\"c\":true," 
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"$H\": 1,"
        + "\"__gwt_ObjectId\": 1,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    assertEquals(expectedJson, JsonUtil.stringify(
        JsonUtil.parse(json)));
  }

  private native JsonObject nativeMethod(Object o) /*-{
    o.y = o.x + 1;
    return o;
  }-*/;
}
