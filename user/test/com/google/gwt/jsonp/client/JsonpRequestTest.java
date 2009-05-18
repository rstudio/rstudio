/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.jsonp.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Tests for {@link JsonpRequest}.
 */
public class JsonpRequestTest extends GWTTestCase {

  /**
   * Checks that an error is received.
   */
  private class AssertFailureCallback<T> implements AsyncCallback<T> {
    private String expectedMessage;

    public AssertFailureCallback(String expectedMessage) {
      this.expectedMessage = expectedMessage;
    }

    public void onFailure(Throwable throwable) {
      assertEquals(expectedMessage, throwable.getMessage());
      finishTest();
    }

    public void onSuccess(T value) {
      fail();
    }
  }

  /**
   * Checks that the received value is as expected.
   */
  private class AssertSuccessCallback<T> implements AsyncCallback<T> {
    private T expectedValue;

    private AssertSuccessCallback(T expectedValue) {
      this.expectedValue = expectedValue;
    }

    public void onFailure(Throwable throwable) {
      fail();
    }

    public void onSuccess(T value) {
      assertEquals(expectedValue, value);
      finishTest();
    }
  }

  /**
   * Checks that a timeout happens.
   */
  private class AssertTimeoutExceptionCallback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable throwable) {
      assertTrue(throwable instanceof TimeoutException);
      finishTest();
    }

    public void onSuccess(T value) {
      fail();
    }
  }

  private static String echo(String value) {
    return GWT.getModuleBaseURL() + "echo?action=SUCCESS&value=" + value;
  }

  private static String echoFailure(String error) {
    return GWT.getModuleBaseURL() + "echo?action=FAILURE&error=" + error;
  }

  private static String echoTimeout() {
    return GWT.getModuleBaseURL() + "echo?action=TIMEOUT";
  }

  private JsonpRequestBuilder jsonp;

  @Override
  public String getModuleName() {
    return "com.google.gwt.jsonp.JsonpTest";
  }

  public void testBooleanFalse() {
    jsonp.requestBoolean(echo("false"), new AssertSuccessCallback<Boolean>(
        Boolean.FALSE));
    delayTestFinish(500);
  }

  public void testBooleanTrue() {
    jsonp.requestBoolean(echo("true"), new AssertSuccessCallback<Boolean>(
        Boolean.TRUE));
    delayTestFinish(500);
  }

  public void testDouble() {
    jsonp.requestDouble(echo("123.456"), new AssertSuccessCallback<Double>(
        123.456));
    delayTestFinish(500);
  }

  public void testFailureCallback() {
    jsonp.setFailureCallbackParam("failureCallback");
    jsonp.requestString(echoFailure("ERROR"),
        new AssertFailureCallback<String>("ERROR"));
    delayTestFinish(500);
  }

  public void testInteger() {
    jsonp.requestInteger(echo("123"), new AssertSuccessCallback<Integer>(123));
    delayTestFinish(500);
  }

  /**
   * Tests that if no failure callback is defined, the servlet receives well
   * only a 'callback' parameter, and sends back the error to it.
   */
  public void testNoFailureCallback() {
    jsonp.setFailureCallbackParam(null);
    jsonp.requestString(echoFailure("ERROR"),
        new AssertSuccessCallback<String>("ERROR"));
    delayTestFinish(500);
  }

  public void testNullBoolean() {
    jsonp.requestBoolean(echo("null"), new AssertSuccessCallback<Boolean>(null));
    delayTestFinish(500);
  }

  public void testNullDouble() {
    jsonp.requestDouble(echo("null"), new AssertSuccessCallback<Double>(null));
    delayTestFinish(500);
  }

  public void testNullInteger() {
    jsonp.requestInteger(echo("null"), new AssertSuccessCallback<Integer>(null));
    delayTestFinish(500);
  }

  public void testNullString() {
    jsonp.requestString(echo("null"), new AssertSuccessCallback<String>(null));
    delayTestFinish(500);
  }

  public void testString() {
    jsonp.requestString(echo("'Hello'"), new AssertSuccessCallback<String>(
        "Hello"));
    delayTestFinish(500);
  }

  public void testTimeout() {
    jsonp.requestString(echoTimeout(), new AssertTimeoutExceptionCallback<String>());
    delayTestFinish(2000);
  }

  public void testVoid() {
    jsonp.send(echo(null), new AssertSuccessCallback<Void>(null));
    delayTestFinish(500);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    jsonp = new JsonpRequestBuilder();
    jsonp.setTimeout(1000);
  }
}
