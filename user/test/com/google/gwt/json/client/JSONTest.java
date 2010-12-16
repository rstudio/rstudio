/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.json.client;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Set;

/**
 * Test case for JSONValue and friends.
 */
public class JSONTest extends GWTTestCase {
  static final String menuTest = "{\"menu\": {\n" + "  \"id\": \"file\",\n"
      + "  \"value\": \"File:\",\n" + "  \"popup\": {\n"
      + "    \"menuitem\": [\n"
      + "      {\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"},\n"
      + "      {\"value\": \"Open\", \"onclick\": \"OpenDoc()\"},\n"
      + "      {\"value\": \"Close\", \"onclick\": \"CloseDoc()\"}\n"
      + "    ]\n" + "  }\n" + "}}\n" + "";

  static final String widgetTest = "{\"widget\": {\n"
      + "    \"debug\": \"on\",\n"
      + "    \"window\": {\n"
      + "        \"title\": \"Sample Konfabulator Widget\",        \"name\": \"main_window\",        \"width\": 500,        \"height\": 500\n"
      + "    },    \"image\": { \n"
      + "        \"src\": \"Images/Sun.png\",\n"
      + "        \"name\": \"sun1\",        \"hOffset\": 250,        \"vOffset\": 250,        \"alignment\": \"center\"\n"
      + "    },    \"text\": {\n"
      + "        \"data\": \"Click Here\",\n"
      + "        \"size\": 36,\n"
      + "        \"style\": \"bold\",        \"name\": \"text1\",        \"hOffset\": 250,        \"vOffset\": 100,        \"alignment\": \"center\",\n"
      + "        \"onMouseUp\": \"sun1.opacity = (sun1.opacity / 100) * 90;\"\n"
      + "    }\n" + "}}    \n" + "";

  // Number of unicode characters to test at once for JSON escaping
  private static final int JSON_CHUNK_SIZE = 256;

  private static void assertJSONArrayEquals(JSONArray expected, JSONArray actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); ++i) {
      assertJSONValueEquals(expected.get(i), actual.get(i));
    }
  }

  private static void assertJSONObjectEquals(JSONObject expected,
      JSONObject actual) {
    Set<String> actKeys = actual.keySet();
    Set<String> expKeys = expected.keySet();
    assertEquals(expKeys.size(), actKeys.size());
    for (String key : expKeys) {
      assertTrue(actual.containsKey(key));
      JSONValue expValue = expected.get(key);
      JSONValue actValue = actual.get(key);
      assertJSONValueEquals(expValue, actValue);
    }
  }

  private static void assertJSONValueEquals(JSONValue expected, JSONValue actual) {
    if (expected.isArray() != null) {
      JSONArray expArray = expected.isArray();
      JSONArray actArray = actual.isArray();
      assertJSONArrayEquals(expArray, actArray);
    } else if (expected.isBoolean() != null) {
      JSONBoolean expBool = expected.isBoolean();
      JSONBoolean actBool = actual.isBoolean();
      assertEquals(expBool.booleanValue(), actBool.booleanValue());
    } else if (expected.isNull() != null) {
      assertNotNull(actual.isNull());
    } else if (expected.isNumber() != null) {
      JSONNumber expNum = expected.isNumber();
      JSONNumber actNum = actual.isNumber();
      assertEquals(expNum.doubleValue(), actNum.doubleValue());
    } else if (expected.isObject() != null) {
      JSONObject expObj = expected.isObject();
      JSONObject actObj = actual.isObject();
      assertJSONObjectEquals(expObj, actObj);
    } else if (expected.isString() != null) {
      JSONString expStr = expected.isString();
      JSONString actStr = actual.isString();
      assertEquals(expStr.stringValue(), actStr.stringValue());
    } else {
      fail("Unknown JSONValue " + expected);
    }
  }

  /**
   * Returns the module name for GWT unit test running.
   */
  public String getModuleName() {
    return "com.google.gwt.json.JSON";
  }

  public void testArrayBasics() {
    JSONArray a = new JSONArray();
    JSONString s = new JSONString("s");
    JSONBoolean b = JSONBoolean.getInstance(false);
    JSONNumber n = new JSONNumber(4);
    assertNull(a.get(3));
    assertNull(a.get(-4));
    a.set(3, s);
    assertEquals(s, a.get(3));
    a.set(-4, b);
    assertEquals(b, a.get(-4));
    a.set(2, b);
    assertEquals(b, a.get(2));
    a.set(1, n);
    assertEquals(n, a.get(1));
  }

  /**
   * Test deep recursion of arrays.
   */
  public void testArrayOfArraysOfArrays() {
    JSONArray array = populateRecursiveArray(3, 5);
    assertEquals(
        "[[[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]]],[[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]]],[[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]],[[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]],[[[],[],[]],[[],[],[]],[[],[],[]]]]]]",
        array.toString());
    // now to access
    for (int i = 0; i < 5; i++) {
      array = (JSONArray) array.get(0);
    }
  }

  /**
   * Tests an array of raw numbers, like [1,2,3].
   */
  public void testArrayOfNumbers() {
    JSONArray arr = new JSONArray();
    for (int i = 0; i < 10; i++) {
      arr.set(i, new JSONNumber(i));
    }
    String s = arr.toString();
    JSONValue v = parseStrictVsLenient(s);
    JSONArray array = v.isArray();
    assertTrue("v must be an array", array != null);
    assertEquals("Array size must be 10", 10, array.size());
    for (int i = 0; i < 10; i++) {
      assertEquals("Array value at " + i + " must be " + i,
          array.get(i).isNumber().doubleValue(), i, 0.001);
    }
  }

  public void testBooleanBasics() {
    assertTrue(JSONBoolean.getInstance(true).booleanValue());
    assertFalse(JSONBoolean.getInstance(false).booleanValue());

    JSONValue trueVal = parseStrictVsLenient("true");
    assertEquals(trueVal, trueVal.isBoolean());
    assertTrue(trueVal.isBoolean().booleanValue());

    JSONValue falseVal = parseStrictVsLenient("false");
    assertEquals(falseVal, falseVal.isBoolean());
    assertFalse(falseVal.isBoolean().booleanValue());
  }

  public void testEquals() {
    JSONArray array = parseStrictVsLenient("[]").isArray();
    assertEquals(array, new JSONArray(array.getJavaScriptObject()));

    assertEquals(JSONBoolean.getInstance(false), JSONBoolean.getInstance(false));
    assertEquals(JSONBoolean.getInstance(true), JSONBoolean.getInstance(true));

    assertEquals(JSONNull.getInstance(), JSONNull.getInstance());

    assertEquals(new JSONNumber(3.1), new JSONNumber(3.1));

    JSONObject object = parseStrictVsLenient("{}").isObject();
    assertEquals(object, new JSONObject(object.getJavaScriptObject()));

    assertEquals(new JSONString("foo"), new JSONString("foo"));
  }

  // Null characters do not work in Development Mode
  public void testEscaping() {
    JSONObject o = new JSONObject();
    char[] charsToEscape = new char[42];
    for (char i = 1; i < 32; i++) {
      charsToEscape[i] = i;
    }
    charsToEscape[32] = '"';
    charsToEscape[33] = '\\';
    charsToEscape[34] = '\b';
    charsToEscape[35] = '\f';
    charsToEscape[36] = '\n';
    charsToEscape[37] = '\r';
    charsToEscape[38] = '\t';
    charsToEscape[39] = '/';
    charsToEscape[40] = '\u2028';
    charsToEscape[41] = '\u2029';
    for (int i = 1; i < charsToEscape.length; i++) {
      o.put("c" + i, new JSONString(new Character(charsToEscape[i]).toString()));
    }
    assertEquals("{\"c1\":\"\\u0001\", \"c2\":\"\\u0002\", "
        + "\"c3\":\"\\u0003\", \"c4\":\"\\u0004\", \"c5\":\"\\u0005\", "
        + "\"c6\":\"\\u0006\", \"c7\":\"\\u0007\", \"c8\":\"\\b\", "
        + "\"c9\":\"\\t\", \"c10\":\"\\n\", \"c11\":\"\\u000B\", "
        + "\"c12\":\"\\f\", \"c13\":\"\\r\", \"c14\":\"\\u000E\", "
        + "\"c15\":\"\\u000F\", \"c16\":\"\\u0010\", \"c17\":\"\\u0011\", "
        + "\"c18\":\"\\u0012\", \"c19\":\"\\u0013\", \"c20\":\"\\u0014\", "
        + "\"c21\":\"\\u0015\", \"c22\":\"\\u0016\", \"c23\":\"\\u0017\", "
        + "\"c24\":\"\\u0018\", \"c25\":\"\\u0019\", \"c26\":\"\\u001A\", "
        + "\"c27\":\"\\u001B\", \"c28\":\"\\u001C\", \"c29\":\"\\u001D\", "
        + "\"c30\":\"\\u001E\", \"c31\":\"\\u001F\", \"c32\":\"\\\"\", "
        + "\"c33\":\"\\\\\", \"c34\":\"\\b\", \"c35\":\"\\f\", "
        + "\"c36\":\"\\n\", \"c37\":\"\\r\", \"c38\":\"\\t\", "
        + "\"c39\":\"/\", \"c40\":\"\\u2028\", \"c41\":\"\\u2029\"}", o.toString());
  }

  public void testHashCode() {
    JSONArray array = parseStrictVsLenient("[]").isArray();
    assertHashCodeEquals(array, new JSONArray(array.getJavaScriptObject()));

    assertHashCodeEquals(JSONBoolean.getInstance(false), JSONBoolean.getInstance(false));
    assertHashCodeEquals(JSONBoolean.getInstance(true), JSONBoolean.getInstance(true));

    assertHashCodeEquals(JSONNull.getInstance(), JSONNull.getInstance());

    assertHashCodeEquals(new JSONNumber(3.1), new JSONNumber(3.1));

    JSONObject object = parseStrictVsLenient("{}").isObject();
    assertHashCodeEquals(object, new JSONObject(object.getJavaScriptObject()));

    assertHashCodeEquals(new JSONString("foo"), new JSONString("foo"));
  }

  public void testLargeArrays() {
    JSONArray arr = null;
    for (int j = 1; j < 500; j *= 2) {
      arr = createLargeArray(j);
      assertEquals("size j" + j, j, arr.size());
    }
  }

  public void testLenientAndStrict() {
    String jsonString = "{ a:27, 'b': 'value' }";

    // parseLenient should succeed
    JSONValue value = JSONParser.parseLenient(jsonString);
    JSONObject object = value.isObject();
    assertEquals(27.0, object.get("a").isNumber().doubleValue());
    assertEquals("value", object.get("b").isString().stringValue());

    // parseStrict should fail
    try {
      parseStrictVsLenient(jsonString);
      fail();
    } catch (JSONException e) {
    }

    // Must fail even on browsers that lack JSON.parse()
    jsonString = "function f() { return 5; }";
    try {
      parseStrictVsLenient(jsonString);
      fail();
    } catch (JSONException e) {
    }
  }

  public void testMenu() {
    JSONObject v = (JSONObject) parseStrictVsLenient(menuTest);
    assertTrue(v.containsKey("menu"));
    JSONObject menu = ((JSONObject) v.get("menu"));
    assertEquals(3, menu.keySet().size());
  }

  public void testNested() {
    JSONObject obj = new JSONObject();
    nestedAux(obj, 3);
    String s1 = obj.toString();
    String s2 = parseStrictVsLenient(s1).toString();
    assertEquals(s1, s2);
    assertEquals(
        "{\"string3\":\"s3\", \"Number3\":3.1, \"Boolean3\":false, "
            + "\"Null3\":null, \"object3\":{\"string2\":\"s2\", \"Number2\":2.1, "
            + "\"Boolean2\":true, \"Null2\":null, \"object2\":{\"string1\":\"s1\","
            + " \"Number1\":1.1, \"Boolean1\":false, \"Null1\":null, \"object1\":{"
            + "\"string0\":\"s0\", \"Number0\":0.1, \"Boolean0\":true, "
            + "\"Null0\":null, \"object0\":{}, "
            + "\"Array0\":[\"s0\",0.1,true,null,{}]}, "
            + "\"Array1\":[\"s1\",1.1,false,null,{\"string0\":\"s0\", "
            + "\"Number0\":0.1, \"Boolean0\":true, \"Null0\":null, \"object0\":{}, "
            + "\"Array0\":[\"s0\",0.1,true,null,{}]}]}, "
            + "\"Array2\":[\"s2\",2.1,true,null,{\"string1\":\"s1\", "
            + "\"Number1\":1.1, \"Boolean1\":false, \"Null1\":null, "
            + "\"object1\":{\"string0\":\"s0\", \"Number0\":0.1, "
            + "\"Boolean0\":true, \"Null0\":null, \"object0\":{}, "
            + "\"Array0\":[\"s0\",0.1,true,null,{}]}, "
            + "\"Array1\":[\"s1\",1.1,false,null,{\"string0\":\"s0\", "
            + "\"Number0\":0.1, \"Boolean0\":true, \"Null0\":null, "
            + "\"object0\":{}, \"Array0\":[\"s0\",0.1,true,null,{}]}]}]}, "
            + "\"Array3\":[\"s3\",3.1,false,null,{\"string2\":\"s2\", "
            + "\"Number2\":2.1, \"Boolean2\":true, \"Null2\":null, "
            + "\"object2\":{\"string1\":\"s1\", \"Number1\":1.1, "
            + "\"Boolean1\":false, \"Null1\":null, \"object1\":{\"string0\":\"s0\","
            + " \"Number0\":0.1, \"Boolean0\":true, \"Null0\":null, \"object0\":{}, "
            + "\"Array0\":[\"s0\",0.1,true,null,{}]}, "
            + "\"Array1\":[\"s1\",1.1,false,null,{\"string0\":\"s0\", "
            + "\"Number0\":0.1, \"Boolean0\":true, \"Null0\":null, "
            + "\"object0\":{}, \"Array0\":[\"s0\",0.1,true,null,{}]}]}, "
            + "\"Array2\":[\"s2\",2.1,true,null,{\"string1\":\"s1\", "
            + "\"Number1\":1.1, \"Boolean1\":false, \"Null1\":null, "
            + "\"object1\":{\"string0\":\"s0\", \"Number0\":0.1, "
            + "\"Boolean0\":true, \"Null0\":null, \"object0\":{}, "
            + "\"Array0\":[\"s0\",0.1,true,null,{}]}, "
            + "\"Array1\":[\"s1\",1.1,false,null,{\"string0\":\"s0\", "
            + "\"Number0\":0.1, \"Boolean0\":true, \"Null0\":null, \"object0\":{},"
            + " \"Array0\":[\"s0\",0.1,true,null,{}]}]}]}]}", obj.toString());
  }

  public void testNumberBasics() {
    JSONNumber n0 = new JSONNumber(1000);
    assertEquals(1000, n0.doubleValue(), .000001);
    assertTrue(n0.isNumber() == n0);
    assertNull(n0.isObject());

    JSONNumber n1 = new JSONNumber(Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, n1.doubleValue(), .00001);
    assertTrue(n1.isNumber() == n1);
    assertNull(n1.isObject());

    JSONNumber n2 = new JSONNumber(Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, n2.doubleValue(), .00001);
    assertTrue(n2.isNumber() == n2);
    assertNull(n2.isObject());
  }

  public void testObjectBasics() {
    JSONObject s = new JSONObject();
    assertNull(s.get("buba"));
    s.put("a", new JSONString("A"));
    s.put("a", new JSONString("AA"));
    assertEquals("\"AA\"", s.get("a").toString());
  }

  /**
   * Tests an object whose keys are filled out with numbers, like {"a":1}.
   */
  public void testObjectOfNumbers() {
    JSONObject obj = new JSONObject();
    for (int i = 0; i < 10; i++) {
      obj.put("Object " + i, new JSONNumber(i));
    }
    String s = obj.toString();
    JSONValue v = parseStrictVsLenient(s);
    JSONObject objIn = v.isObject();
    assertTrue("v must be an object", objIn != null);
    assertEquals("Object size must be 10", 10, objIn.keySet().size());
    for (int i = 0; i < 10; i++) {
      assertEquals("Object value at 'Object " + i + "' must be " + i,
          objIn.get("Object " + i).isNumber().doubleValue(), i, 0.001);
    }
  }

  /**
   * Tests generic parsing.
   */
  public void testParse() {
    try {
      new JSONString(null);
      fail();
    } catch (NullPointerException t) {
    }
    try {
      parseStrictVsLenient(null);
      fail();
    } catch (NullPointerException t) {
    }
    try {
      parseStrictVsLenient("");
      fail();
    } catch (IllegalArgumentException t) {
    }
    try {
      parseStrictVsLenient("{\"menu\": {\n" + "  \"id\": \"file\",\n");
      fail();
    } catch (JSONException e) {
    }
    assertEquals("\"null\" should be null JSONValue", JSONNull.getInstance(),
        parseStrictVsLenient("null"));
    assertEquals("5 should be JSONNumber 5", 5d,
        parseStrictVsLenient("5").isNumber().doubleValue(), 0.001);
    assertEquals("\"null\" should be null JSONValue", JSONNull.getInstance(),
        parseStrictVsLenient("null"));
    JSONValue somethingHello = parseStrictVsLenient("[{\"something\":\"hello\"}]");
    JSONArray somethingHelloArray = somethingHello.isArray();
    assertTrue("somethingHello must be a JSONArray",
        somethingHelloArray != null);
    assertTrue("somethingHello size must be one",
        somethingHelloArray.size() == 1);
    JSONObject somethingHelloObject = somethingHelloArray.get(0).isObject();
    assertTrue("somethingHello element 0 must be a JSONObject",
        somethingHelloObject != null);
    assertTrue("somethingHello element 0 must have hello for key something",
        somethingHelloObject.get("something").isString().stringValue().equals(
            "hello"));
  }

  public void testParseUnescaped() {
    for (int i = 0; i <= 0xffff; i += JSON_CHUNK_SIZE) {
      doTestParseUnescaped(i, JSON_CHUNK_SIZE);
    }
  }

  public void testRoundTripEscaping() {
    JSONObject obj = new JSONObject();
    obj.put("a", new JSONNumber(42));
    obj.put("\\", new JSONNumber(43.5));
    obj.put("\"", new JSONNumber(44));

    String toString = obj.toString();
    assertEquals("{\"a\":42, \"\\\\\":43.5, \"\\\"\":44}", toString.trim());
    JSONValue parseResponse = parseStrictVsLenient(toString);
    JSONObject obj2 = parseResponse.isObject();
    assertJSONObjectEquals(obj, obj2);
  }

  public void testSimpleNested() {
    JSONObject j1 = new JSONObject();
    j1.put("test1", new JSONString(""));

    JSONObject j2 = new JSONObject();
    j2.put("test1", new JSONString(""));

    JSONObject j22 = new JSONObject();
    j22.put("test12", new JSONString(""));
    j2.put("j22", j22);

    JSONObject j3 = new JSONObject();
    j3.put("j1", j1);
    j3.put("j2", j2);

    assertEquals(
        "{\"j1\":{\"test1\":\"\"}, \"j2\":{\"test1\":\"\", \"j22\":{\"test12\":\"\"}}}",
        j3.toString());
  }

  public void testStringBasics() {
    JSONString arr = new JSONString("");
    assertEquals("\"\"", arr.toString());
    JSONString s = new JSONString(menuTest);
    assertEquals(menuTest, s.stringValue());
  }

  public void testStringEscaping() {
    checkRoundTripJsonText("\"hello\"", "hello");
    checkRoundTripJsonText("\"hel\\\"lo\"", "hel\"lo");
    checkRoundTripJsonText("\"hel\\\\lo\"", "hel\\lo");
    checkRoundTripJsonText("\"hel\\\\\\\"lo\"", "hel\\\"lo");
  }

  public void testStringTypes() {
    JSONObject object = parseStrictVsLenient("{\"a\":\"b\",\"null\":\"foo\"}").isObject();
    assertNotNull(object);

    assertEquals("b",
        object.get(stringAsPrimitive("a")).isString().stringValue());
    assertEquals("b", object.get(stringAsObject("a")).isString().stringValue());
    assertEquals("foo",
        object.get(stringAsPrimitive("null")).isString().stringValue());
    assertEquals("foo",
        object.get(stringAsObject("null")).isString().stringValue());

    try {
      assertNull(object.get(null));
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testUndefined() {
    JSONObject o = parseStrictVsLenient("{\"foo\":\"foo\",\"bar\":null}").isObject();
    assertEquals(new JSONString("foo"), o.get("foo"));
    assertEquals(JSONNull.getInstance(), o.get("bar"));
    assertNull(o.get("baz"));

    o.put("foo", JSONNull.getInstance());
    assertEquals(JSONNull.getInstance(), o.get("foo"));
    o.put("foo", null);
    assertNull(o.get("foo"));

    JSONArray array = parseStrictVsLenient("[\"foo\",null]").isArray();
    assertEquals(new JSONString("foo"), array.get(0));
    assertEquals(JSONNull.getInstance(), array.get(1));
    assertNull(array.get(2));

    array.set(0, JSONNull.getInstance());
    assertEquals(JSONNull.getInstance(), array.get(0));
    array.set(0, null);
    assertNull(array.get(0));
  }

  public void testUnicodeSeparators() {
    /*
     * ECMAScript 5 allows unescaped U+2028 (line separator) and U+2029
     * (paragraph separator) characters to occur inside strings.
     */
    String jsonString = "{ \"name\": \"miles\u2028da\u2029vis\", \"ins\u2028tru\u2029ment\": \"trumpet\" }";
    try {
      JSONValue parsed = parseStrictVsLenient(jsonString);
      JSONObject result = parsed.isObject();
      assertNotNull(result);

      JSONValue nameValue = result.get("name");
      assertNotNull(nameValue);
      JSONString nameJsonString = nameValue.isString();
      assertNotNull(nameJsonString);
      String nameString = nameJsonString.stringValue();
      assertEquals("miles\u2028da\u2029vis", nameString);
      String nameStringQuoted = nameJsonString.toString();
      assertEquals("\"miles\\u2028da\\u2029vis\"", nameStringQuoted);

      JSONValue instrumentValue = result.get("ins\u2028tru\u2029ment");
      assertNotNull(instrumentValue);
      JSONValue instrumentValue2 = result.get("instrument");
      assertNull(instrumentValue2); // check no name collision
      JSONString instrumentJsonString = instrumentValue.isString();
      assertNotNull(instrumentJsonString);
      String instrumentString = instrumentJsonString.stringValue();
      assertEquals("trumpet", instrumentString);
    } catch (JSONException e) {
      fail(e.getMessage());
    }

    // U+2028 and U+2029 should not appear outside a string
    jsonString = "{ \"name\": \"miles davis\",\u2028\"instrument\": \"trumpet\" }";
    try {
      parseStrictVsLenient(jsonString);
      fail();
    } catch (JSONException e) {
    }

    jsonString = "{ \"name\":\u2029\"miles davis\", \"instrument\": \"trumpet\" }";
    try {
      parseStrictVsLenient(jsonString);
      fail();
    } catch (JSONException e) {
    }
  }

  public void testUnsafeEval() {
    JsonUtils.unsafeEval("{\"name\":\"value\"}");
    JsonUtils.unsafeEval("[0,1,2,3,4]");
  }

  public void testWidget() {
    JSONObject v = (JSONObject) parseStrictVsLenient(widgetTest);
    JSONObject widget = (JSONObject) v.get("widget");
    JSONObject window = (JSONObject) widget.get("window");
    JSONValue title = window.get("title");
    assertNotNull(title.isString());
    JSONValue hOffSet = window.get("width");
    assertNotNull(hOffSet.isNumber());
  }

  private void assertHashCodeEquals(Object expected, Object actual) {
    assertEquals("hashCodes are not equal", expected.hashCode(),
        actual.hashCode());
  }

  private void checkRoundTripJsonText(String jsonText, String normaltext) {
    JSONString parsed = parseStrictVsLenient(jsonText).isString();
    assertEquals(normaltext, parsed.stringValue());
    assertEquals(jsonText, parsed.toString());
  }

  private JSONArray createLargeArray(int size) {
    JSONArray arr = new JSONArray();
    for (int i = 0; i < size; i++) {
      arr.set(i, new JSONNumber(i));
    }
    return arr;
  }

  /**
   * Test a chunk of Unicode code points for use in JSON keys and values.
   *
   * @param start starting code point
   * @param len number of code points to test
   */
  private void doTestParseUnescaped(int start, int len) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < start + len; i++) {
      // Skip control characters
      if (i < 0x20) {
        continue;
      }
      // Skip surrogate pairs
      if (i >= 0xD800 && i <= 0xDFFF) {
        continue;
      }
      char c = (char) i;
      // Skip quote and backslash
      if (c == '\"' || c == '\\') {
        continue;
      }
      sb.append(c);
    }
    String chars = sb.toString();
    if (chars.length() == 0) {
      return;
    }

    String key = "na" + chars + "me";
    String value = "miles" + chars + "davis";
    String jsonString = "{ \"" + key + "\": \"" + value + "\"}";
    try {
      JSONValue parsed = parseStrictVsLenient(jsonString);
      JSONObject result = parsed.isObject();
      assertNotNull("start = " + start + ", len = " + len, result);
      Set<String> keys = result.keySet();
      assertTrue("start = " + start + ", len = " + len, keys.contains(key));
      JSONValue nameValue = result.get(key);
      assertNotNull("start = " + start + ", len = " + len, nameValue);
      JSONString nameJsonString = nameValue.isString();
      assertNotNull("start = " + start + ", len = " + len, nameJsonString);
      String nameString = nameJsonString.stringValue();
      assertEquals("start = " + start + ", len = " + len, value, nameString);
    } catch (JSONException e) {
      fail("start = " + start + ", len = " + len + ", e = " + e.getMessage());
    }
  }

  private void nestedAux(JSONObject obj, int i) {
    JSONArray array = new JSONArray();
    JSONString str = new JSONString("s" + i);
    array.set(0, str);
    obj.put("string" + i, str);
    JSONNumber num = new JSONNumber(i + 0.1);
    array.set(1, num);
    obj.put("Number" + i, num);
    JSONBoolean b = JSONBoolean.getInstance((i % 2) == 0);
    array.set(2, b);
    obj.put("Boolean" + i, b);
    JSONNull nul = JSONNull.getInstance();
    array.set(3, nul);
    obj.put("Null" + i, nul);
    JSONObject newObj = new JSONObject();
    obj.put("object" + i, newObj);
    if (i != 0) {
      nestedAux(newObj, i - 1);
    }
    array.set(4, newObj);
    obj.put("Array" + i, array);
  }

  // Check that parseLenient produces the same results as parseStrict
  private JSONValue parseStrictVsLenient(String jsonString) {
    JSONValue strictValue = JSONParser.parseStrict(jsonString);
    JSONValue lenientValue = JSONParser.parseLenient(jsonString);
    assertJSONValueEquals(strictValue, lenientValue);
    return strictValue;
  }

  private JSONArray populateRecursiveArray(int numElements, int recursion) {
    JSONArray newArray = new JSONArray();
    if (recursion <= 0) {
      return newArray;
    }

    for (int i = 0; i < numElements; i++) {
      JSONArray childArray = populateRecursiveArray(numElements, recursion - 1);
      newArray.set(i, childArray);
    }
    return newArray;
  }

  private native String stringAsObject(String str) /*-{
    return new String(str);
  }-*/;

  private native String stringAsPrimitive(String str) /*-{
    return String(str);
  }-*/;
}
