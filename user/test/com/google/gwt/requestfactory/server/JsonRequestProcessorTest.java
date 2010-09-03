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
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.RequestData;
import com.google.gwt.requestfactory.shared.SimpleEnum;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.WriteOperation;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Tests for {@link JsonRequestProcessor} .
 */
public class JsonRequestProcessorTest extends TestCase {

  enum Foo {
    BAR, BAZ
  }

  private JsonRequestProcessor requestProcessor;;

  @Override
  public void setUp() {
    requestProcessor = new JsonRequestProcessor();
    requestProcessor.setOperationRegistry(new ReflectionBasedOperationRegistry(
        new DefaultSecurityProvider()));
  }

  public void testDecodeParameterValue() {
    // primitives
    assertTypeAndValueEquals(String.class, "Hello", "Hello");
    assertTypeAndValueEquals(Integer.class, 1234, "1234");
    assertTypeAndValueEquals(Byte.class, (byte) 100, "100");
    assertTypeAndValueEquals(Short.class, (short) 12345, "12345");
    assertTypeAndValueEquals(Float.class, 1.234f, "1.234");
    assertTypeAndValueEquals(Double.class, 1.234567, "1.234567");
    assertTypeAndValueEquals(Long.class, 1234L, "1234");
    assertTypeAndValueEquals(Boolean.class, true, "true");
    assertTypeAndValueEquals(Boolean.class, false, "false");

    // dates
    Date now = new Date();
    assertTypeAndValueEquals(Date.class, now, "" + now.getTime());
    // bigdecimal and biginteger
    BigDecimal dec = new BigDecimal("10").pow(100);
    assertTypeAndValueEquals(BigDecimal.class, dec, dec.toPlainString());
    assertTypeAndValueEquals(BigInteger.class, dec.toBigInteger(),
        dec.toBigInteger().toString());
    // enums
    assertTypeAndValueEquals(Foo.class, Foo.BAR, "" + Foo.BAR.ordinal());
  }

  public void testEncodePropertyValue() {
    assertEncodedType(String.class, "Hello");
    // primitive numbers become doubles
    assertEncodedType(Double.class, (byte) 10);
    assertEncodedType(Double.class, (short) 1234);
    assertEncodedType(Double.class, 123.4f);
    assertEncodedType(Double.class, 1234.0);
    assertEncodedType(Double.class, 1234);
    // longs, big nums become strings
    assertEncodedType(String.class, 1234L);
    assertEncodedType(String.class, new BigDecimal(1));
    assertEncodedType(String.class, new BigInteger("1"));
    assertEncodedType(String.class, new Date());
    assertEncodedType(Double.class, Foo.BAR);
    assertEncodedType(Boolean.class, true);
    assertEncodedType(Boolean.class, false);
  }

  public void testEndToEnd() {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    try {
      // fetch object
     JSONObject foo = fetchVerifyAndGetInitialObject();

      // modify fields and sync
      foo.put("intId", 45);
      foo.put("userName", "JSC");
      foo.put("longField", "" + 9L);
      foo.put("enumField", SimpleEnum.BAR.ordinal());
      foo.put("boolField", false);
      Date now = new Date();
      foo.put("created", "" + now.getTime());

     JSONObject result = getResultFromServer(foo);

      // check modified fields and no violations
      SimpleFoo fooResult = SimpleFoo.getSingleton();
      JSONArray updateArray = result.getJSONObject("sideEffects").getJSONArray("UPDATE");
      assertEquals(1, updateArray.length());
      assertEquals(1, updateArray.getJSONObject(0).length());
      assertTrue(updateArray.getJSONObject(0).has("id"));
      assertFalse(updateArray.getJSONObject(0).has("violations"));
      assertEquals(45, (int) fooResult.getIntId());
      assertEquals("JSC", fooResult.getUserName());
      assertEquals(now, fooResult.getCreated());
      assertEquals(9L, (long) fooResult.getLongField());
      assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.BAR,
          fooResult.getEnumField());
      assertEquals(false, (boolean) fooResult.getBoolField());
      
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.toString());
    }
  }

  public void testEndToEndSmartDiff_NoChange() {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    try {
      // fetch object
      JSONObject foo = fetchVerifyAndGetInitialObject();

      // change the value on the server behind the back
      SimpleFoo fooResult = SimpleFoo.getSingleton();
      fooResult.setUserName("JSC");
      fooResult.setIntId(45);

      // modify fields and sync -- there should be no change on the server.
      foo.put("intId", 45);
      foo.put("userName", "JSC");
      JSONObject result = getResultFromServer(foo);

      // check modified fields and no violations
      assertFalse(result.getJSONObject("sideEffects").has("UPDATE"));
      fooResult = SimpleFoo.getSingleton();
      assertEquals(45, (int) fooResult.getIntId());
      assertEquals("JSC", fooResult.getUserName());
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.toString());
    }
  }

  public void testEndToEndSmartDiff_SomeChange() {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    try {
      // fetch object
      JSONObject foo = fetchVerifyAndGetInitialObject();

      // change some fields but don't change other fields.
      SimpleFoo fooResult = SimpleFoo.getSingleton();
      fooResult.setUserName("JSC");
      fooResult.setIntId(45);
      foo.put("userName", "JSC");
      foo.put("intId", 45);
      Date now = new Date();
      long newTime = now.getTime() + 10000;
      foo.put("created", "" + newTime);

      JSONObject result = getResultFromServer(foo);

      // check modified fields and no violations
      assertTrue(result.getJSONObject("sideEffects").has("UPDATE"));
      fooResult = SimpleFoo.getSingleton();
      assertEquals(45, (int) fooResult.getIntId());
      assertEquals("JSC", fooResult.getUserName());
      assertEquals(newTime, fooResult.getCreated().getTime());
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.toString());
    }
  }

  private void assertEncodedType(Class<?> expected, Object value) {
    assertEquals(expected,
        requestProcessor.encodePropertyValue(value).getClass());
  }

  private <T> void assertTypeAndValueEquals(Class<T> expectedType,
      T expectedValue, String paramValue) {
    Object val = requestProcessor.decodeParameterValue(expectedType, paramValue);
    assertEquals(expectedType, val.getClass());
    assertEquals(expectedValue, val);
  }

  private JSONObject fetchVerifyAndGetInitialObject() throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException {
    JSONObject results = (JSONObject) requestProcessor.processJsonRequest("{ \""
        + RequestData.OPERATION_TOKEN
        + "\": \""
        + com.google.gwt.requestfactory.shared.SimpleFooRequest.class.getName()
        + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
        + "findSimpleFooById\", "
        + "\""
        + RequestData.PARAM_TOKEN
        + "0\": \"999\" }");
    JSONObject foo = results.getJSONObject("result");
    assertEquals(foo.getInt("id"), 999);
    assertEquals(foo.getInt("intId"), 42);
    assertEquals(foo.getString("userName"), "GWT");
    assertEquals(foo.getLong("longField"), 8L);
    assertEquals(foo.getInt("enumField"), 0);
    assertEquals(foo.getInt("version"), 1);
    assertEquals(foo.getBoolean("boolField"), true);
    assertTrue(foo.has("created"));
    return foo;
  }

  private JSONObject getResultFromServer(JSONObject foo) throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException {
    JSONObject proxyWithSchema = new JSONObject();
    proxyWithSchema.put(SimpleFooProxy.class.getName(), foo);
    JSONArray arr = new JSONArray();
    arr.put(proxyWithSchema);
    JSONObject operation = new JSONObject();
    operation.put(WriteOperation.UPDATE.toString(), arr);
    JSONObject sync = new JSONObject();
    sync.put(RequestData.OPERATION_TOKEN,
        "com.google.gwt.requestfactory.shared.SimpleFooRequest::persist");
    sync.put(RequestData.CONTENT_TOKEN, operation.toString());
    sync.put(RequestData.PARAM_TOKEN + "0", foo.getInt("id") + "-NO" + "-"
        + SimpleFooProxy.class.getName());
    return (JSONObject) requestProcessor.processJsonRequest(sync.toString());
  }
}
