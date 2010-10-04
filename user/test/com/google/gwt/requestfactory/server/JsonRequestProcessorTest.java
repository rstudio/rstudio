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

import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleEnum;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.Constants;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link JsonRequestProcessor} .
 */
public class JsonRequestProcessorTest extends TestCase {

  enum Foo {
    BAR, BAZ
  }

  private JsonRequestProcessor requestProcessor;

  @Override
  public void setUp() {
    requestProcessor = new JsonRequestProcessor();
    requestProcessor.setOperationRegistry(new ReflectionBasedOperationRegistry(
        new DefaultSecurityProvider()));
  }

  public void testDecodeParameterValue() throws SecurityException,
      JSONException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, InstantiationException {
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
    
    // nulls
    assertTypeAndValueEquals(String.class, null, null);
    assertTypeAndValueEquals(Integer.class, null, null);
    assertTypeAndValueEquals(Byte.class, null, null);
    assertTypeAndValueEquals(Short.class, null, null);
    assertTypeAndValueEquals(Float.class, null, null);
    assertTypeAndValueEquals(Double.class, null, null);
    assertTypeAndValueEquals(Long.class, null, null);
    assertTypeAndValueEquals(Boolean.class, null, null);
    assertTypeAndValueEquals(Date.class, null, null);
    assertTypeAndValueEquals(BigDecimal.class, null, null);
    assertTypeAndValueEquals(BigInteger.class, null, null);
    assertTypeAndValueEquals(Foo.class, null, null);
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
    // nulls becomes JSON Null. Needed because JSONObject stringify treats 'null'
    // as a reason to not emit the key in the stringified object
    assertEquals(JSONObject.NULL, requestProcessor.encodePropertyValue(null));
  }

  public void testEndToEnd() throws Exception {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
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
    SimpleFoo fooResult = SimpleFoo.findSimpleFooById(999L);
    JSONArray updateArray = result.getJSONObject("sideEffects").getJSONArray(
        "UPDATE");
    assertEquals(1, updateArray.length());
    assertEquals(2, updateArray.getJSONObject(0).length());
    assertTrue(updateArray.getJSONObject(0).has(Constants.ENCODED_ID_PROPERTY));
    assertTrue(updateArray.getJSONObject(0).has(Constants.ENCODED_VERSION_PROPERTY));
    assertFalse(updateArray.getJSONObject(0).has("violations"));
    assertEquals(45, (int) fooResult.getIntId());
    assertEquals("JSC", fooResult.getUserName());
    assertEquals(now, fooResult.getCreated());
    assertEquals(9L, (long) fooResult.getLongField());
    assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.BAR,
        fooResult.getEnumField());
    assertEquals(false, (boolean) fooResult.getBoolField());
  }

  public void testEndToEndSmartDiff_NoChange() throws Exception {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    // fetch object
    JSONObject foo = fetchVerifyAndGetInitialObject();

    // change the value on the server behind the back
    SimpleFoo fooResult = SimpleFoo.findSimpleFooById(999L);
    fooResult.setUserName("JSC");
    fooResult.setIntId(45);

    // modify fields and sync -- there should be no change on the server.
    foo.put("intId", 45);
    foo.put("userName", "JSC");
    JSONObject result = getResultFromServer(foo);

    // check modified fields and no violations
    assertFalse(result.getJSONObject("sideEffects").has("UPDATE"));
    fooResult = SimpleFoo.findSimpleFooById(999L);
    assertEquals(45, (int) fooResult.getIntId());
    assertEquals("JSC", fooResult.getUserName());
  }

  public void testEndToEndSmartDiff_SomeChangeWithNull() throws Exception {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    // fetch object
    JSONObject foo = fetchVerifyAndGetInitialObject();

    // change some fields but don't change other fields.
    SimpleFoo fooResult = SimpleFoo.findSimpleFooById(999L);
    fooResult.setUserName("JSC");
    fooResult.setIntId(45);
    fooResult.setNullField(null);
    fooResult.setBarField(SimpleBar.getSingleton());
    fooResult.setBarNullField(null);
    foo.put("userName", JSONObject.NULL);
    foo.put("intId", 45);
    foo.put("nullField", "test");
    foo.put("barNullField",
        JsonRequestProcessor.base64Encode(SimpleBar.getSingleton().getId())
            + "@NO@" + SimpleBarProxy.class.getName());
    foo.put("barField", JSONObject.NULL);

    JSONObject result = getResultFromServer(foo);

    // check modified fields and no violations
    assertTrue(result.getJSONObject("sideEffects").has("UPDATE"));
    fooResult = SimpleFoo.findSimpleFooById(999L);
    assertEquals(45, (int) fooResult.getIntId());
    assertNull(fooResult.getUserName());
    assertEquals("test", fooResult.getNullField());
    assertEquals(SimpleBar.getSingleton(), fooResult.getBarNullField());
    assertNull(fooResult.getBarField());
  }

  public void testEndToEndSmartDiff_SomeChange() throws Exception {
    com.google.gwt.requestfactory.server.SimpleFoo.reset();
    // fetch object
    JSONObject foo = fetchVerifyAndGetInitialObject();

    // change some fields but don't change other fields.
    SimpleFoo fooResult = SimpleFoo.findSimpleFooById(999L);
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
    fooResult = SimpleFoo.findSimpleFooById(999L);
    assertEquals(45, (int) fooResult.getIntId());
    assertEquals("JSC", fooResult.getUserName());
    assertEquals(newTime, fooResult.getCreated().getTime());
  }

  public void testEndToEndNumberList()
      throws ClassNotFoundException, InvocationTargetException,
      NoSuchMethodException, JSONException, InstantiationException,
      IllegalAccessException {
    fetchVerifyAndGetNumberList();
  }
  
  private void assertEncodedType(Class<?> expected, Object value) {
    assertEquals(expected,
        requestProcessor.encodePropertyValue(value).getClass());
  }

  private <T> void assertTypeAndValueEquals(Class<T> expectedType,
      T expectedValue, String paramValue) throws SecurityException,
      JSONException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, InstantiationException {
    Object val = requestProcessor.decodeParameterValue(expectedType, paramValue);
    if (val != null) {
      assertEquals(expectedType, val.getClass());
    }
    assertEquals(expectedValue, val);
  }

  @SuppressWarnings("unchecked")
  private JSONObject fetchVerifyAndGetInitialObject() throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException, SecurityException, InstantiationException {
    JSONObject results = requestProcessor.processJsonRequest("{ \""
        + Constants.OPERATION_TOKEN
        + "\": \""
        + com.google.gwt.requestfactory.shared.SimpleFooRequest.class.getName()
        + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
        + "findSimpleFooById\", "
        + "\""
        + Constants.PARAM_TOKEN
        + "0\": \"999\", \"" + Constants.PROPERTY_REF_TOKEN  + "\": "
        + "\"oneToManyField,oneToManySetField,selfOneToManyField\""
        + "}");
    JSONObject foo = results.getJSONObject("result");
    assertEquals(foo.getLong("id"), 999L);
    assertEquals(foo.getInt("intId"), 42);
    assertEquals(foo.getString("userName"), "GWT");
    assertEquals(foo.getLong("longField"), 8L);
    assertEquals(foo.getInt("enumField"), 0);
    assertEquals(foo.getInt(Constants.ENCODED_VERSION_PROPERTY), 1);
    assertEquals(foo.getBoolean("boolField"), true);
    assertNotNull(foo.getString("!id"));
    assertTrue(foo.has("created"));
    List<Double> numList = (List<Double>) foo.get("numberListField");
    assertEquals(2, numList.size());
    assertEquals(42.0, numList.get(0));
    assertEquals(99.0, numList.get(1));

    List<String> oneToMany = (List<String>) foo.get("oneToManyField");
    assertEquals(2, oneToMany.size());
    assertEquals(JsonRequestProcessor.base64Encode("1L") + "@NO@com.google.gwt.requestfactory.shared.SimpleBarProxy", oneToMany.get(0));
    assertEquals(JsonRequestProcessor.base64Encode("1L") + "@NO@com.google.gwt.requestfactory.shared.SimpleBarProxy", oneToMany.get(1));

    List<String> selfOneToMany = (List<String>) foo.get("selfOneToManyField");
    assertEquals(1, selfOneToMany.size());
    assertEquals("999@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy", selfOneToMany.get(0));

    Set<String> oneToManySet = (Set<String>) foo.get("oneToManySetField");
    assertEquals(1, oneToManySet.size());
    assertEquals(JsonRequestProcessor.base64Encode("1L") + "@NO@com.google.gwt.requestfactory.shared.SimpleBarProxy", oneToManySet.iterator().next());
    return foo;
  }

   private JSONArray fetchVerifyAndGetNumberList() throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException, SecurityException, InstantiationException {
    JSONObject results = requestProcessor.processJsonRequest("{ \""
        + Constants.OPERATION_TOKEN
        + "\": \""
        + com.google.gwt.requestfactory.shared.SimpleFooRequest.class.getName()
        + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
        + "getNumberList\" }");
    JSONArray foo = results.getJSONArray("result");
    assertEquals(foo.length(), 3);
    assertEquals(foo.getInt(0), 1);
    assertEquals(foo.getInt(1), 2);
    assertEquals(foo.getInt(2), 3);     
    return foo;
  }

  public void testPrimitiveListAsParameter() throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException, SecurityException, InstantiationException {

    JSONObject results = requestProcessor.processJsonRequest("{ \""
        + Constants.OPERATION_TOKEN
        + "\": \""
        + com.google.gwt.requestfactory.shared.SimpleFooRequest.class.getName()
        + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
        + "sum\", "
        + "\"" + Constants.PARAM_TOKEN + "0\":"
        + "\"1@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy\","
        + "\""
        + Constants.PARAM_TOKEN
        + "1\": [1, 2, 3] }");
    assertEquals(6, results.getInt("result"));
  }

  public void testProxyListAsParameter() throws JSONException,
        NoSuchMethodException, IllegalAccessException, InvocationTargetException,
        ClassNotFoundException, SecurityException, InstantiationException {
      SimpleFoo.reset();
      JSONObject results = requestProcessor.processJsonRequest("{ \""
          + Constants.OPERATION_TOKEN
          + "\": \""
          + com.google.gwt.requestfactory.shared.SimpleFooRequest.class.getName()
          + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
          + "processList\", "
          + "\"" + Constants.PARAM_TOKEN + "0\":"
          + "\"1@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy\","
          + "\""
          + Constants.PARAM_TOKEN
          + "1\": [\"1@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy\", \"1@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy\", \"1@NO@com.google.gwt.requestfactory.shared.SimpleFooProxy\"] }");
      assertEquals("GWTGWTGWT", results.getString("result"));
    }

  private JSONObject getResultFromServer(JSONObject foo) throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException, SecurityException, InstantiationException {
    JSONObject proxyWithSchema = new JSONObject();
    proxyWithSchema.put(SimpleFooProxy.class.getName(), foo);
    JSONArray arr = new JSONArray();
    arr.put(proxyWithSchema);
    JSONObject operation = new JSONObject();
    operation.put(WriteOperation.UPDATE.toString(), arr);
    JSONObject sync = new JSONObject();
    sync.put(Constants.OPERATION_TOKEN,
        "com.google.gwt.requestfactory.shared.SimpleFooRequest::persist");
    sync.put(Constants.CONTENT_TOKEN, operation.toString());
    sync.put(Constants.PARAM_TOKEN + "0", foo.getString("id") + "@NO" + "@"
        + SimpleFooProxy.class.getName());
    return requestProcessor.processJsonRequest(sync.toString());
  }
}
