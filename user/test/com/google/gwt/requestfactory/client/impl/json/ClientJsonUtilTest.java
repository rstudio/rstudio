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
package com.google.gwt.requestfactory.client.impl.json;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.client.impl.ProxyJsoImpl;

/**
 * Tests for {@link ClientJsonUtil}
 */
public class ClientJsonUtilTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }


  public void testEscapeControlChars() {
    String unicodeString = "\u2060Test\ufeffis a test\u17b5";
    assertEquals("\\u00002060Test\\u0000feffis a test\\u000017b5",
        ClientJsonUtil.escapeControlChars(unicodeString));
  }

  public void testQuote() {
    String badString = "\bThis\"is\ufeff\ta\\bad\nstring\u2029\u2029";
    assertEquals("\"\\bThis\\\"is\\u0000feff\\ta\\\\bad\\nstring"
        + "\\u00002029\\u00002029\"", ClientJsonUtil.quote(badString));
  }

  public void testLegalParse() {
    JavaScriptObject obj = ClientJsonUtil.parse(
        "{ \"a\":1, \"b\":\"hello\", \"c\": true,"
            + "\"d\": null, \"e\": [1,2,3,4], \"f\": {} }");
    assertNotNull(obj);
  }

  public void testIllegalParse() {
    try {
      ClientJsonUtil.parse("{ \"a\": new String() }");
      fail("Expected JsonException to be thrown");
    } catch (JsonException je) {
    }
  }

  public void testStringify() {
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    assertEquals(json, ClientJsonUtil.stringify(ClientJsonUtil.parse(json)));
  }

  public void testStringifyCycle() {
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    JsonMap jso = ClientJsonUtil.parse(json).cast();
    jso.put("cycle", jso);
    try {
      ClientJsonUtil.stringify(jso);
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
    assertEquals(json, ClientJsonUtil.stringify(ClientJsonUtil.parse(json), 2));
  }

  public void testStringifySkipKeys() {
    String expectedJson = "{\"a\":1,\"b\":\"hello\",\"c\":true," 
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,"
        + "\"" + ProxyJsoImpl.REQUEST_FACTORY_FIELD + "\": 1,"
        + "\"" + ProxyJsoImpl.SCHEMA_FIELD + "\": 1,"
        + "\"$H\": 1,"
        + "\"__gwt_ObjectId\": 1,"
        + "\"d\":null,\"e\":[1,2,3,4],\"f\":{\"x\":1}}";
    assertEquals(expectedJson, ClientJsonUtil.stringify(
        ClientJsonUtil.parse(json)));
  }
}
